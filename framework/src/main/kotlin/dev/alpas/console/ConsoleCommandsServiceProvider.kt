package dev.alpas.console

import dev.alpas.AppConfig
import dev.alpas.Application
import dev.alpas.ServiceProvider
import dev.alpas.appConfig
import dev.alpas.makeElse

class ConsoleCommandsServiceProvider(private val args: Array<String>) : ServiceProvider {
    override fun register(app: Application) {
        app.bufferDebugLog("Registering $this")
        app.singleton(AlpasCommand(app.appConfig().commandAliases))
        app.kernel.capture(args)
    }

    override fun commands(app: Application): List<Command> {
        return if (app.env.isDev) {
            listOf(
                ServeCommand(app.makeElse { AppConfig(app.env) }),
                MakeCommandCommand(app.srcPackage),
                MakeServiceProviderCommand(app.srcPackage),
                KeyGenerateCommand(),
                LinkWebCommand(app.env, app.makeElse { AppConfig(app.env) }),
                LinkTemplatesCommand(app.env)
            ) + app.kernel.commands(app)
        } else {
            app.kernel.commands(app)
        }
    }
}
