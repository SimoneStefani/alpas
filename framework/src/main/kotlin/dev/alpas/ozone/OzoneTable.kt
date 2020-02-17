package dev.alpas.ozone

import dev.alpas.extensions.toCamelCase
import me.liuwj.ktorm.dsl.AssignmentsBuilder
import me.liuwj.ktorm.dsl.combineConditions
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.isNull
import me.liuwj.ktorm.entity.findList
import me.liuwj.ktorm.entity.findOne
import me.liuwj.ktorm.expression.ArgumentExpression
import me.liuwj.ktorm.expression.ColumnAssignmentExpression
import me.liuwj.ktorm.schema.*
import java.time.Instant
import java.time.temporal.Temporal
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Ozone's base table that enhances Ktorm's table by adding few super helpful extensions. It is recommended to
 * extend from this table rather than extending Ktorm's table as most of the extensions are available only on
 * this table. This table brings some other features as well such as migrations, relationship extensions, etc.
 *
 * @param tableName The name of the table.
 * @param alias The alias of this table.
 * @param entityClass The entity class for this table.
 */
abstract class OzoneTable<E : Ozone<E>>(tableName: String, alias: String? = null, entityClass: KClass<E>? = null) :
        Table<E>(tableName, alias, entityClass) {

    /**
     * Column metadata for this table. The metadata is used during migration.
     */
    internal val metadataMap = hashMapOf<String, ColumnMetadata>()

    internal var constraints = mutableSetOf<ColumnReferenceConstraint>()

    /**
     * The name of the columns that will be bound automatically to their corresponding entity
     * properties. Override this method to decide what columns would be bound automatically.
     */
    protected open val autoBindColumnNames = arrayOf("id", "created_at", "updated_at")

    /**
     * Whether to autobind the given column name or not. Instead of overriding [autoBindColumnNames],
     * you can instead override this method and decide per column whether to autobind or not. Currently,
     * only `id`, `created_at`, and `updated_at`
     */
    open fun shouldAutoBind(column: String): Boolean {
        return autoBindColumnNames.contains(column)
    }

    // COLUMN REGISTRATION EXTENSIONS
    /**
     * Set the column's size metadata to the given size. This metadata can be applied only to
     * the columns of type string such as  [MediumTextSqlType], [LongTextSqlType], etc.
     *
     * @param size Size of the column.
     */
    fun ColumnRegistration<String>.size(size: Int): ColumnRegistration<String> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(size = size)
        } else {
            metadataMap[columnName] = metadata.copy(size = size)
        }

        return this
    }

    /**
     * Set the column's default value metadata to the given value.
     * This metadata can be applied to any [SqlType] columns.
     *
     * @param default The default value for the column.
     */
    fun <T : Any> ColumnRegistration<T>.default(default: T): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(defaultValue = default)
        } else {
            metadataMap[columnName] = metadata.copy(defaultValue = default)
        }

        return this
    }

    /**
     * Set the column's default value to use current. This metadata can be applied to only
     * the column of [Temporal] sql type such as created_at, updated_at, etc.
     *
     */
    fun <T : Temporal> ColumnRegistration<T>.useCurrent(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(useCurrentTimestamp = true)
        } else {
            metadataMap[columnName] = metadata.copy(useCurrentTimestamp = true)
        }

        return this
    }

    /**
     * Make the column an unsigned column. This can be applied to any column
     * of [Number] sql type such as Int, Long etc.
     *
     */
    fun <T : Number> ColumnRegistration<T>.unsigned(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(unsigned = true)
        } else {
            metadataMap[columnName] = metadata.copy(unsigned = true)
        }

        return this
    }

    /**
     *
     * Make this column an autoincrement column. This can be applied to any column
     * of [Number] sql type such as Int, Long etc.
     *
     * TODO: allow to pass `isUnique` param? as an autoincrement must be defined as a key
     *
     */
    fun <T : Number> ColumnRegistration<T>.autoIncrement(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(autoIncrement = true)
        } else {
            metadataMap[columnName] = metadata.copy(autoIncrement = true)
        }

        return this
    }

    /**
     * Mark this column as nullable. This can be applied to any column type.
     */
    fun <T : Any> ColumnRegistration<T>.nullable(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(nullable = true)
        } else {
            metadataMap[columnName] = metadata.copy(nullable = true)
        }

        return this
    }

    /**
     * Add a reference constraint on the given column for this table.
     */
    fun <T : Any> ColumnRegistration<T>.addReferenceConstraint(columnToRefer: String = "id", block: ColumnReferenceConstraint.() -> Unit = {}): ColumnRegistration<T> {
        val column = getColumn()
        val referenceTable = column.referenceTable ?: throw IllegalStateException("Reference constraint can only be added to a column that references a foreign table")
        ColumnReferenceConstraint(columnName, referenceTable.tableName, columnToRefer).also {
            constraints.add(it)
        }.also(block)
        return this
    }

    /**
     * Set the precision of a double/float/decimal column type.
     *
     * @param total The total precision metadata for the column.
     * @param places The number of decimal places for the column.
     */
    fun <T : Number> ColumnRegistration<T>.precision(total: Int, places: Int): ColumnRegistration<T> {
        when (getColumn().sqlType) {
            !is FloatSqlType, !is DoubleSqlType, !is DecimalSqlType -> return this
        }
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(precision = Precision(total, places))
        } else {
            metadataMap[columnName] = metadata.copy(precision = Precision(total, places))
        }

        return this
    }

    /**
     * Mark the column as unique. This can be applied to any column type.
     */
    fun <T : Any> ColumnRegistration<T>.unique(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(unique = true)
        } else {
            metadataMap[columnName] = metadata.copy(unique = true)
        }

        return this
    }

    /**
     * Add an index to this column.
     */
    fun <T : Any> ColumnRegistration<T>.index(): ColumnRegistration<T> {
        val columnName = getColumn().name
        val metadata = metadataMap[columnName]
        if (metadata == null) {
            metadataMap[columnName] = ColumnMetadata(index = true)
        } else {
            metadataMap[columnName] = metadata.copy(index = true)
        }

        return this
    }

    /**
     * Define a created_at column typed of [InstantSqlType].
     */
    fun createdAt(
            name: String = "created_at",
            nullable: Boolean = true,
            useCurrent: Boolean = true
    ): ColumnRegistration<Instant> {
        return registerAndBind(name, InstantSqlType).apply {
            if (nullable) {
                nullable()
            }
            if (useCurrent) {
                useCurrent()
            }
        }
    }

    /**
     * Define a updated_at column typed of [InstantSqlType].
     */
    fun updatedAt(
            name: String = "updated_at",
            nullable: Boolean = true,
            useCurrent: Boolean = true
    ): ColumnRegistration<Instant> {
        return registerAndBind(name, InstantSqlType).apply {
            if (nullable) {
                nullable()
            }
            if (useCurrent) {
                useCurrent()
            }
        }
    }

    /**
     * Automatically register and bind a column under the given name and type.
     */
    internal fun <T : Any> registerAndBind(name: String, type: SqlType<T>): ColumnRegistration<T> {
        return registerColumn(name, type).also {
            if (shouldAutoBind(name)) {
                autoBind(it)
            }
        }
    }

    /**
     * Automatically bind a column after inferring its corresponding member name from the entity.
     */
    private fun <T : Any> autoBind(column: ColumnRegistration<T>): ColumnRegistration<T> {
        val entityClass = this.entityClass ?: error("No entity class configured for table: $tableName")
        return column.apply {
            val colName = columnName.toCamelCase()
            val props = entityClass.memberProperties.find { prop ->
                prop.name == colName
            } ?: throw IllegalStateException("Entity ${entityClass.simpleName} doesn't contain property $colName.")
            doBindInternal(NestedBinding(listOf(props)))
        }
    }

    /**
     * Bind this column to the reference table basically setting the belongsTo relationship.
     */
    inline fun <C : Any, R : Ozone<R>> ColumnRegistration<C>.belongsTo(
            referenceTable: Table<R>,
            selector: (E) -> R?
    ) = apply { references(referenceTable, selector) }

    /**
     * Find an entity with the given [whereAttributes]. If the entity doesn't exist, create a
     * new entity by combining both [whereAttributes] and [attributes]. Any extra
     * attributes will be skipped inserting into the database.
     *
     * @param whereAttributes The attributes to use for looking up an entity.
     * @param attributes The attributes to use while creating an entity.
     *
     * @return A existing or a new entity.
     *
     */
    @Suppress("UNCHECKED_CAST")
    fun findOrCreate(whereAttributes: Map<String, Any?>, attributes: Map<String, Any?> = emptyMap()): E {
        if (whereAttributes.isEmpty()) {
            throw IllegalArgumentException("whereAttributes cannot be empty.")
        }
        return findOne(whereAttributes) ?: create(whereAttributes.plus(attributes))
    }

    /**
     * Find an entity with the given [whereAttributes]. If the entity doesn't exist, create a
     * new entity by combining both [whereAttributes] and the [assignmentBlock]. Any extra
     * attributes will be skipped inserting into the database.
     *
     * @param whereAttributes The attributes to use for looking up an entity.
     * @param assignmentBlock An assignment block for adding extra attributes while creating an entity.
     *
     * @return A existing or a new entity.
     *
     */
    @Suppress("UNCHECKED_CAST")
    fun findOrCreate(
            whereAttributes: Map<String, Any?>,
            assignmentBlock: AssignmentsBuilder.(OzoneTable<E>) -> Unit
    ): E {
        return findOne(whereAttributes) ?: create(whereAttributes, assignmentBlock)
    }

    /**
     * Find an entity with the given [whereAttributes].
     *
     * @param whereAttributes The attributes to use for looking up an entity.
     *
     * @return A existing entity if exists otherwise null.
     */
    @Suppress("UNCHECKED_CAST")
    fun findOne(whereAttributes: Map<String, Any?>): E? {
        val table = this
        val whereConditions = whereAttributes.map {
            val col = table[it.key] as Column<Any>
            when (val value = it.value) {
                null -> col.isNull()
                else -> col.eq(value)
            }
        }
        return findOne { whereConditions.combineConditions() }
    }

    /**
     * Find many entities with the given [whereAttributes].
     *
     * @param whereAttributes The attributes to use for looking up an entity.
     *
     * @return A list of entities that satisfy the given [whereAttributes].
     */
    @Suppress("UNCHECKED_CAST")
    fun findMany(whereAttributes: Map<String, Any?>): List<E> {
        val table = this
        val whereConditions = whereAttributes.map {
            val col = table[it.key] as Column<Any>
            when (val value = it.value) {
                null -> col.isNull()
                else -> col.eq(value)
            }
        }
        return findList { whereConditions.combineConditions() }
    }

    /**
     * Create an entity with the given attributes. Any non-existent column names in the attributes map will be skipped.
     *
     * @return A new entity.
     */
    @Suppress("UNCHECKED_CAST")
    fun create(attributes: Map<String, Any?>): E {
        val table = this
        return create { builder ->
            for ((name, value) in attributes) {
                table.columns.find { it.name == name }?.let {
                    builder[name] to value
                }
            }
        }
    }
}

/**
 * Create an entity with the given attributes combined with the provided [AssignmentsBuilder]
 * block. This is useful esp. when creating an entity with some HTTP field params and
 * some additional assignments for the missing fields.
 *
 * ```kotlin
 *
 *  val input = call.params("name", "description", "website")
 *  val app = Apps.create(input) {
 *      it.ownerId to call.user.id
 *  }
 *
 * ```
 *
 * @return A new entity.
 */
fun <E : Ozone<E>, T : OzoneTable<E>> T.create(
        attributes: Map<String, Any?> = emptyMap(),
        block: AssignmentsBuilder.(T) -> Unit
): E {
    val combinedAttributes = attributes.toMutableMap()
    val assignments = ArrayList<ColumnAssignmentExpression<*>>()
    AssignmentsBuilder(assignments).block(this)
    assignments.map {
        combinedAttributes[it.column.name] = (it.expression as ArgumentExpression).value
    }
    return create(combinedAttributes)
}

/**
 * A map of an entity's column name to the actual corresponding column name in the table.
 */
fun <T : Ozone<T>> OzoneTable<T>.propertyNamesToColumnNames(): Map<String, String> {
    return columns.map { col ->
        val colNameInTable = when (val binding = col.binding) {
            is NestedBinding -> {
                binding.properties[0].name
            }
            else -> col.name
        }
        Pair(colNameInTable, col.name)
    }.toMap()
}
