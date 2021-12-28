package dev.alpas.ozone.console

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alpas.Application
import dev.alpas.PackageClassLoader
import dev.alpas.asMagenta
import dev.alpas.console.Command
import dev.alpas.deleteLastLine
import dev.alpas.load
import dev.alpas.ozone.Seeder

class DatabaseSeedCommand(private val app: Application) : Command(name = "db:seed", help = "Seed a database") {
    private val defaultSeederName = "DatabaseSeeder"
    private val name by argument(help = "The name of the seeder to run. Uses ${defaultSeederName.asMagenta()} by default.").optional()
    private val packageClassLoader: PackageClassLoader by lazy { PackageClassLoader(app.srcPackage) }

    override fun run() {
        val seederName = name ?: defaultSeederName
        val classInfo = packageClassLoader.classesExtending(Seeder::class).firstOrNull { it.simpleName == seederName }
        if (classInfo == null) {
            error("Cannot find the seeder $seederName to run.")
        } else {
            withColors {
                println()
                echo(yellow("Seeding the database using $seederName"))
            }
            classInfo.load<Seeder>().run(app)
            withColors {
                deleteLastLine()
                echo("${green("✓")} ${brightGreen("Seeded the database using $seederName")}")
            }
        }
    }
}
