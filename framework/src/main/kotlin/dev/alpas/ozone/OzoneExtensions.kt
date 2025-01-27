@file:Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")

package dev.alpas.ozone

import dev.alpas.http.HttpCall
import dev.alpas.orAbort
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.DialectFeatureNotSupportedException
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.entity.*
import me.liuwj.ktorm.expression.SelectExpression
import me.liuwj.ktorm.expression.UnionExpression
import me.liuwj.ktorm.schema.BaseTable
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.ColumnDeclaring
import me.liuwj.ktorm.schema.SqlType
import org.eclipse.jetty.http.HttpStatus
import java.time.Instant

/**
 * Checks whether this database is of the given name type.
 */
fun Database.isOfType(name: String): Boolean {
    return this.productName.lowercase() == name.lowercase()
}

/**
 * Checks whether this database is of type MySQL.
 */
fun Database.isMySql(): Boolean {
    return isOfType("mysql")
}

/**
 * Checks whether this database is of type SQLite.
 */
fun Database.isSqlite(): Boolean {
    return isOfType("sqlite")
}

/**
 * Checks whether this database is of type PostgreSQL.
 */
fun Database.isPostgreSQL(): Boolean {
    return isOfType("postgresql")
}

/**
 * Obtain an entity object by its ID, auto left joining all the reference tables.
 *
 * This function will throw a NotFoundException if no records found, and throw an exception if there are more than one record.
 */
@Suppress("UNCHECKED_CAST")
fun <E : Any> BaseTable<E>.findOrFail(
    id: Any, message: String? = null,
    statusCode: Int = HttpStatus.NOT_FOUND_404
): E {
    return this.findById(id).orAbort(message ?: "Record with id $id doesn't exist.", statusCode)
}

fun <E : OzoneEntity<E>, T : OzoneTable<E>> T.create(
    timestamp: Instant? = Instant.now(),
    block: AssignmentsBuilder.(T) -> Unit
): E {
    return create(emptyMap(), timestamp, block)
}

/**
 * Obtain an entity object matching the given [predicate], auto left joining all the reference tables.
 *
 * This function will abort if no records found, and throw an exception if there are more than one record.
 */
inline fun <E : Any, T : BaseTable<E>> T.findOneOrFail(
    message: String? = null,
    statusCode: Int = HttpStatus.NOT_FOUND_404,
    predicate: (T) -> ColumnDeclaring<Boolean>
): E {
    return this.findOne(predicate).orAbort(message, statusCode)
}

internal fun <T : Any> SqlType<T>.isVarChar(): Boolean = this.typeName.lowercase() == "varchar"

fun Query.selectFirst() = apply {
    try {
        limit(0, 1).firstOrNull()
    } catch (e: DialectFeatureNotSupportedException) {
        firstOrNull()
    }
}

internal fun Query.forUpdate(): Query {
    val expr = this.expression

    val exprForUpdate = when (expr) {
        is SelectExpression -> expr.copy(extraProperties = expr.extraProperties + Pair("forUpdate", true))
        is UnionExpression -> expr.copy(extraProperties = expr.extraProperties + Pair("forUpdate", true))
    }

    return this.copy(expression = exprForUpdate)
}

/**
 * Return the latest entities of an entity sequence as determined by sorting the
 * sequence in a descending order using a column returned by the [selector].
 *
 * @param selector The column to be selected for sorting.
 *
 * @return The latest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.latest(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return sorted { listOf(selector(it).desc()) }
}

/**
 * Return the latest entities of an entity sequence as determined by sorting the sequence in a descending order using,
 * by default, the `created_at` column. Pass the name of a different column to sort by that column instead.
 *
 * @param column The name of the column to use for sorting. `created_at` by default.
 *
 * @return The latest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.latest(column: String = "created_at"): EntitySequence<E, T> {
    return sorted { listOf(it[column].desc()) }
}


/**
 * Return the oldest entities of an entity sequence as determined by sorting the
 * sequence in an ascending order using a column returned by the [selector].
 *
 * @param selector The column to be selected for sorting.
 *
 * @return The oldest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.oldest(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return sorted { listOf(selector(it).asc()) }
}

/**
 * Return the oldest entities of an entity sequence as determined by sorting the sequence in an ascending order using,
 * by default, the `created_at` column. Pass the name of a different column to sort by that column instead.
 *
 * @param column The name of the column to use for sorting. `created_at` by default.
 *
 * @return The oldest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> EntitySequence<E, T>.oldest(column: String = "created_at"): EntitySequence<E, T> {
    return sorted { listOf(it[column].asc()) }
}

/**
 * Return the latest entities of a table as determined by sorting the table in a
 * descending order using a column returned by the [selector].
 *
 * @param selector The column to be selected for sorting.
 *
 * @return The latest entities as an entity sequence.
 */

inline fun <E : OzoneEntity<E>, T : OzoneTable<E>> T.latest(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return asSequence().sorted { listOf(selector(it).desc()) }
}

/**
 * Return the latest entities of a table as determined by sorting the table in a descending order using, by
 * default, the `created_at` column. Pass the name of a different column to sort by that column instead.
 *
 * @param column The name of the column to use for sorting. `created_at` by default.
 *
 * @return The latest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> T.latest(column: String = "created_at"): EntitySequence<E, T> {
    return asSequence().sorted { listOf(it[column].desc()) }
}

/**
 * Return the oldest entities of a table as determined by sorting the table in
 * an ascending order using a column returned by the [selector].
 *
 * @param selector The column to be selected for sorting.
 *
 * @return The oldest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> T.oldest(selector: (T) -> ColumnDeclaring<*>): EntitySequence<E, T> {
    return asSequence().sorted { listOf(selector(it).asc()) }
}

/**
 * Return the oldest entities of a table as determined by sorting the table in an ascending order using, by
 * default, the `created_at` column. Pass the name of a different column to sort by that column instead.
 *
 * @param column The name of the column to use for sorting. `created_at` by default.
 *
 * @return The oldest entities as an entity sequence.
 */
inline fun <E : Any, T : BaseTable<E>> T.oldest(column: String = "created_at"): EntitySequence<E, T> {
    return asSequence().sorted { listOf(it[column].asc()) }
}

@Deprecated("Deprecated", ReplaceWith("entity(table, paramKey, primaryKey)"))
inline fun <E : OzoneEntity<E>, T : OzoneTable<E>> HttpCall.entityParam(
    table: T,
    paramKey: String = "id",
    primaryKey: String = "id"
): E {
    return entity(table, paramKey, primaryKey)
}

/**
 * A convenience method to find an entity in a [table] by matching its primary key to
 * the [paramKey] parameter of an [HttpCall]. This aborts the call If the [paramKey]
 * is not present in the call or an entity cannot be found in the table.
 *
 * An attempt will be made to convert the corresponding parameter to a [Long].
 * If it cannot be converted to long, it is left as it is.
 *
 * @param table The table to find the entity in.
 * @param paramKey The key to look in the call. Set to "id" by default.
 *
 * @return An entity instance or a [NotFoundHttpException].
 */
inline fun <E : OzoneEntity<E>, T : OzoneTable<E>> HttpCall.entity(
    table: T,
    paramKey: String = "id",
    primaryKey: String = "id"
): E {
    val missingKeyError = if (env.isDev)
        "Couldn't fetch an entity from the table '${table.tableName}' because of missing parameter '$paramKey'." else null
    val key =
        param(paramKey).orAbort(missingKeyError)
            .let {
                val param = it.toString()
                param.toLongOrNull() ?: it
            }
    val noEntityError =
        if (env.isDev) "Entity with $primaryKey $key doesn't exist in the table '${table.tableName}'." else null
    return table.findOne {
        (it[primaryKey] as Column<Any>) eq key
    }.orAbort(noEntityError)
}
