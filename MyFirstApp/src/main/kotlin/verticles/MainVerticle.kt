package verticles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import models.DataSourceConfig
import models.ServerConfig
import models.SunInfo
import models.SunWeatherInfo
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import org.slf4j.LoggerFactory
import security.DatabaseAuthProvider
import services.MigrationService
import services.SunService
import services.WeatherService
import uy.klutter.vertx.VertxInit

class MainVerticle : AbstractVerticle() {

    private val weatherService = WeatherService()
    private val sunService = SunService()

    private var maybeDataSource: HikariDataSource? = null

    override fun start(startFuture: Future<Void>?) {
        val jsonMapper = jacksonObjectMapper()

        VertxInit.ensure()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        val logger = LoggerFactory.getLogger("VertxServer")
        val templateEngine = ThymeleafTemplateEngine.create()

        val dataSourceConfig = jsonMapper.readValue(config().getJsonObject("dataSource").encode(), DataSourceConfig::class.java)
        val dataSource = initDataSource(dataSourceConfig)
        val migrationService = MigrationService(dataSource)
        val migrationResult = migrationService.migrate()
        migrationResult.fold({ exc ->
            logger.error("Exception occurred while performing migration", exc)
            vertx.close()
        }, { _ ->
            logger.debug("Migration successful or not needed")
        })

        val serverConfig = jsonMapper.readValue(config().getJsonObject("server").encode(), ServerConfig::class.java)
        val serverPort = serverConfig.port
        val enableCaching = serverConfig.caching

        val authProvider = DatabaseAuthProvider(dataSource, jsonMapper)
        router.route().handler(CookieHandler.create())
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
        router.route().handler(UserSessionHandler.create(authProvider))

        router.route("/public/*").handler(StaticHandler.create().setWebRoot("public").setCachingEnabled(enableCaching))
        router.route("/hidden/*").handler(RedirectAuthHandler.create(authProvider))
        router.route("/login").handler(BodyHandler.create())
        router.route("/login").handler(FormLoginHandler.create(authProvider))

        fun renderTemplate(ctx: RoutingContext, templateDirectory: String, templateFileName: String) {
            templateEngine.render(ctx, templateDirectory, templateFileName, { buf ->
                val response = ctx.response()
                if (buf.failed()) {
                    logger.error("Template rendering failed", buf.cause())
                    response.setStatusCode(500).end()
                } else {
                    response.end(buf.result())
                }
            })
        }
        router.get("/loginpage").handler { ctx ->
            renderTemplate(ctx, "public/templates/", "login.html")
        }
        router.get("/home").handler { ctx ->
            renderTemplate(ctx, "public/templates/", "index.html")
        }
        router.get("/hidden/admin").handler { ctx ->
            renderTemplate(
                    ctx.put("username", ctx.user().principal().getString("username")),
                    "public/templates/",
                    "admin.html"
            )
        }

        router.get("/api/data").handler { ctx ->
            val lat = -33.8830
            val lon = 151.2167

            val sunInfoP = sunService.getSunInfo(lat, lon)
            val temperatureP = weatherService.getTemperature(lat, lon)

            val sunWeatherInfoP = sunInfoP.bind { sunInfo: SunInfo ->
                temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp) }
            }

            sunWeatherInfoP.success { info: SunWeatherInfo ->
                val json = jsonMapper.writeValueAsString(info)
                val response = ctx.response()
                response.end(json)
            }
        }

        server.requestHandler { router.accept(it) }.listen(serverPort, { handler ->
            if (!handler.succeeded()) {
                logger.error("Failed to listen on port $serverPort")
            }
        })
    }

    override fun stop(stopFuture: Future<Void>?) {
        maybeDataSource?.close()
    }

    private fun initDataSource(config: DataSourceConfig): HikariDataSource {
        val hikariDS = HikariDataSource()
        hikariDS.username = config.user
        hikariDS.password = config.password
        hikariDS.jdbcUrl = config.jdbcUrl
        maybeDataSource = hikariDS
        return hikariDS
    }
}
