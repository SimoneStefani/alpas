package dev.alpas.ozone.migration

import dev.alpas.Container
import dev.alpas.ozone.OzoneEntity
import dev.alpas.ozone.OzoneTable

// todo: support adding composite keys
// todo: support collation
// todo: support adapters other than MySQL
// todo: support altering table

abstract class Migration {
    // internal var isDryRun = false
    internal lateinit var adapter: DbAdapter
    internal lateinit var givenName: String
    open val name: String? = null

    fun <E : OzoneEntity<E>> createTable(
        table: OzoneTable<E>,
        ifNotExists: Boolean = false,
        block: (TableBuilder.() -> Unit)? = null
    ) {
        val cols = table.columns
        adapter.createTable(table, ifNotExists) {
            cols.forEach {
                addColumn(it, table)
            }

            table.primaryKey?.name?.let {
                primaryKey(it)
            }
            if (block != null) {
                block()
            }
        }
    }

    fun <E : OzoneEntity<E>> modifyTable(table: OzoneTable<E>, block: TableModifier<E>.() -> Unit) {
        adapter.modifyTable(TableModifier(table).also(block))
    }

    fun <E : OzoneEntity<E>> dropTable(table: OzoneTable<E>) {
        adapter.dropTable(table.tableName)
    }

    open fun shouldSkipBatch(batch: Int) = false

    open fun up() {}
    open fun up(container: Container) {
        up()
    }

    open fun down() {}
    open fun down(container: Container) {
        down()
    }

    protected fun execute(query: String) {
        adapter.execute(query)
    }
}
