package verticles

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.ThymeleafTemplateEngine
import nl.komponents.kovenant.functional.bind
import nl.komponents.kovenant.functional.map
import org.slf4j.LoggerFactory
import services.SunService
import services.WeatherService
import uy.klutter.vertx.VertxInit
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainVerticle : AbstractVerticle() {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Australia/Sydney"))
    }

    data class ServerConfig(val port: Int, val caching: Boolean)

    data class SunInfo(val sunrise: String, val sunset: String)
    data class SunWeatherInfo(val sunInfo: SunInfo, val temperature: Double)

    private val weatherService = WeatherService()
    private val sunService = SunService()

    override fun start(startFuture: Future<Void>?) {
        VertxInit.ensure()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        val logger = LoggerFactory.getLogger("VertxServer")
        val templateEngine = ThymeleafTemplateEngine.create()

        val jsonMapper = jacksonObjectMapper()

        router.get("/api/data").handler { ctx ->
            val lat = -33.8830
            val lon = 151.2167

            val sunInfoP = sunService.getSunInfo(lat, lon)
            val temperatureP = weatherService.getTemperature(lat, lon)

            val sunWeatherInfoP = sunInfoP.bind { sunInfo ->
                temperatureP.map { temp -> SunWeatherInfo(sunInfo, temp) }
            }

            sunWeatherInfoP.success { info ->
                val json = jsonMapper.writeValueAsString(info)
                val response = ctx.response()
                response.end(json)
            }
        }

        router.get("/home").handler { ctx ->
            templateEngine.render(ctx, "public/templates/", "index.html", { buf ->
                if (buf.failed()) {
                    logger.error("Template rendering failed", buf.cause())
                } else {
                    val response = ctx.response()
                    response.end(buf.result())
                }
            })
        }

        val serverConfig = jsonMapper.readValue(config().getJsonObject("server").encode(), ServerConfig::class.java)
        val serverPort = serverConfig.port
        val enableCaching = serverConfig.caching

        val staticHandler = StaticHandler.create().setWebRoot("public").setCachingEnabled(enableCaching)
        router.route("/public/*").handler(staticHandler)

        server.requestHandler { router.accept(it) }.listen(serverPort, { handler ->
            if (!handler.succeeded()) {
                logger.error("Failed to listen on port $serverPort")
            }
        })
    }
}
