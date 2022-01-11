package dev.alpas.ozone.migration

import dev.alpas.ozone.ColumnInfo
import dev.alpas.ozone.ColumnKey
import dev.alpas.ozone.ColumnMetadata
import dev.alpas.ozone.ColumnReferenceConstraint
import dev.alpas.ozone.OzoneEntity
import dev.alpas.printAsSuccess
import me.liuwj.ktorm.jackson.JsonSqlType
import me.liuwj.ktorm.schema.BlobSqlType
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.TextSqlType

internal class PostgreSqlAdapter(isDryRun: Boolean, quiet: Boolean) : DbAdapter(isDryRun, quiet) {
    override fun createTable(tableBuilder: TableBuilder, ifNotExists: Boolean) {
        val notExists = if (ifNotExists) " IF NOT EXISTS " else " "
        val sb = StringBuilder("CREATE TABLE$notExists\"${tableBuilder.tableName}\"")
        sb.appendLine(" (")
        val colDef = tableBuilder.columnsToAdd.joinToString(",\n") {
            columnDefinition(it)
        }
        sb.appendLine(colDef)

        val keysDef = tableBuilder.keys.joinToString(",\n") {
            columnKeysDefinition(it)
        }
        if (keysDef.isNotEmpty()) {
            sb.append(",")
        }
        sb.appendLine(keysDef)

        val constraintsDef = tableBuilder.constraints.joinToString(",\n") {
            columnConstraints(it, tableBuilder.tableName)
        }
        if (constraintsDef.isNotEmpty()) {
            sb.append(",")
        }
        sb.appendLine(constraintsDef)

        sb.append(");")
        execute(sb.toString())
    }

    override fun createDatabase(name: String): Boolean {
        return execute("CREATE DATABASE \"$name\"")
    }

    private fun columnDefinition(colInfo: ColumnInfo): String {
        return "\"${colInfo.col.name}\" ${toColTypeName(colInfo)}${colInfo.meta.def(colInfo)}"
    }

    private fun columnKeysDefinition(key: ColumnKey): String {
        val columns = key.colNames.joinToString(",", prefix = "\"", postfix = "\"")
        return "${key.type} ${key.name} ($columns)"
    }

    private fun columnConstraints(constraint: ColumnReferenceConstraint, tableName: String): String {
        val sql =
            "CONSTRAINT \"${tableName}_${constraint.foreignKey}_foreign\" FOREIGN KEY (\"${constraint.foreignKey}\") REFERENCES \"${constraint.tableToRefer}\" (\"${constraint.columnToRefer}\")"
        return constraint.onDelete?.let {
            "$sql ON DELETE $it"
        } ?: sql
    }

    private fun toColTypeName(colInfo: ColumnInfo): String {
        return if (colInfo.meta?.autoIncrement == true) "" else colInfo.col.sqlType.typeName.lowercase()
    }

    private fun ColumnMetadata?.def(colInfo: ColumnInfo): String {
        if (this == null) {
            return ""
        }
        val sb = StringBuilder()
        size?.let {
            sb.append("($it)")
        }
        if (autoIncrement) {
            sb.append(" SERIAL")
        }
        if (nullable) {
            sb.append(" NULL DEFAULT NULL")
        } else {
            sb.append(" NOT NULL")
        }
        if (useCurrentTimestamp) {
            sb.append(" DEFAULT CURRENT_TIMESTAMP")
        } else {
            defaultValue?.let { dval ->
                sb.append(" DEFAULT ")
                val sqlType = colInfo.col.sqlType
                if (sqlType is JsonSqlType || sqlType is BlobSqlType || sqlType is TextSqlType) {
                    sb.append("('$dval')")
                } else {
                    sb.append("$dval")
                }
            }
        }
        return sb.toString()
    }

    override fun dropTable(tableName: String) {
        execute(""" DROP TABLE "$tableName" """.trim())
    }

    override fun dropAllTables() {
        val query = """
                DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT USAGE ON SCHEMA public to PUBLIC; GRANT CREATE ON SCHEMA public to PUBLIC; COMMENT ON SCHEMA public IS 'standard public schema';
            """.trimIndent()

        execute(query)

        if (shouldTalk) {
            "Done!".printAsSuccess()
        }
    }

    override fun <E : OzoneEntity<E>> modifyTable(builder: TableModifier<E>) {
        val sb = StringBuilder("ALTER TABLE \"${builder.tableName}\"")
        addNewColumns(sb, builder)
        dropColumns(sb, builder)
        sb.append(";")
        execute(sb.toString())
    }

    private fun <E : OzoneEntity<E>> addNewColumns(sb: StringBuilder, builder: TableModifier<E>) {
        if (builder.columnsToAdd.isEmpty()) {
            return
        }
        val colDef = builder.columnsToAdd.joinToString(",\n", prefix = "\n") {
            "ADD COLUMN ${columnDefinition(it)}"
        }
        sb.appendLine(colDef)

        val keysDef = builder.keys.joinToString(",\n") {
            columnKeysDefinition(it)
        }
        if (keysDef.isNotEmpty()) {
            sb.append(",")
            sb.appendLine(keysDef)
        }

        val constraintsDef = builder.constraints.joinToString(",\n") {
            columnConstraints(it, builder.tableName)
        }
        if (constraintsDef.isNotEmpty()) {
            sb.append(",")
            sb.appendLine(constraintsDef)
        }
    }

    private fun <E : OzoneEntity<E>> dropColumns(sb: StringBuilder, builder: TableModifier<E>) {
        if (builder.columnsToDrop.isEmpty()) {
            return
        }

        val dropColumnsDef = builder.columnsToDrop.joinToString(",\n", prefix = "\n") {
            "DROP COLUMN $it"
        }
        sb.appendLine(dropColumnsDef)
    }
}
