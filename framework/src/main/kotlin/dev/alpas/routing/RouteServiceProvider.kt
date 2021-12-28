package dev.alpas.routing

import dev.alpas.Application
import dev.alpas.Container
import dev.alpas.PackageClassLoader
import dev.alpas.ServiceProvider
import dev.alpas.appConfig
import dev.alpas.auth.AuthConfig
import dev.alpas.auth.console.MakeAuthCommand
import dev.alpas.config
import dev.alpas.console.Command
import dev.alpas.make
import dev.alpas.routing.console.MakeControllerCommand
import dev.alpas.routing.console.MakeMiddlewareCommand
import dev.alpas.routing.console.MakeValidationGuardCommand
import dev.alpas.routing.console.MakeValidationRuleCommand
import dev.alpas.routing.console.RouteInfoCommand
import java.net.URI

@Suppress("unused")
open class RouteServiceProvider : ServiceProvider {
    override fun register(app: Application) {
        val router = Router()
        app.singleton(router)
        loadRoutes(router)
        app.registerAppHook(this)
    }

    protected open fun loadRoutes(router: Router) {}

    override fun commands(app: Application): List<Command> {
        val routeListCommand = RouteInfoCommand(app.srcPackage, app.make(), app.config { AuthConfig(app.make()) })
        return if (app.env.isDev) {
            return listOf(
                MakeControllerCommand(app.srcPackage),
                MakeAuthCommand(app.srcPackage),
                MakeMiddlewareCommand(app.srcPackage),
                routeListCommand,
                MakeValidationGuardCommand(app.srcPackage),
                MakeValidationRuleCommand(app.srcPackage)
            )
        } else {
            listOf(routeListCommand)
        }
    }

    override fun boot(app: Application, loader: PackageClassLoader) {
        app.logger.debug { "Compiling Router" }
        app.make<Router>().compile(loader)
    }

    override fun onAppStarted(app: Application, uri: URI) {
        val actualUri = if (app.env.isProduction) {
            URI(app.appConfig().appUrl)
        } else {
            uri
        }
        bindUrlGenerator(app, actualUri)
    }

    private fun bindUrlGenerator(container: Container, uri: URI) {
        // todo: re-initialize URL generator in dev mode
        container.singleton(UrlGenerator(uri, container.make(), container.make()))
    }
}
