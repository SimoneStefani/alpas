package dev.alpas.ozone

import com.zaxxer.hikari.HikariDataSource
import dev.alpas.Environment
import dev.alpas.mustStartWith
import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.support.sqlite.SQLiteDialect
import java.io.File
import java.nio.file.Paths

open class SqliteConnection(private val env: Environment, config: ConnectionConfig? = null) : DatabaseConnection {
    open val dialect = config?.sqlDialect ?: SQLiteDialect()
    open val extraParams = config?.extraParams ?: emptyMap()

    private val db by lazy {
        val database = if (config?.database == ":memory:") {
            ":memory:"
        } else {
            (config?.database ?: defaultDatabase()).also(::createDatabaseFile)
        }
        val params = combineParams(extraParams)
        val ds = HikariDataSource().also { it.jdbcUrl = "jdbc:sqlite:$database?$params" }
        Database.connect(ds, dialect)
    }

    private fun defaultDatabase(): String {
        val db = "${env("DB_DATABASE", "dev")}.sqlite"
        return Paths.get(env.rootDir, "database", db).toAbsolutePath().toString()
    }

    private fun createDatabaseFile(path: String) {
        File(path).apply {
            parentFile.mkdirs()
            createNewFile()
        }
    }

    override fun connect(): Database = db
}
