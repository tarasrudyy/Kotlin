package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import verticles.MainVerticle
import java.nio.charset.Charset
import java.time.ZonedDateTime

class SunService {

    fun getSunInfo(lat: Double, lon: Double): Promise<MainVerticle.SunInfo, Exception> = task {
        val sunIfoURL = "http://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&formatted=0"
        val (_, response) = sunIfoURL.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))
        val json = JsonParser().parse(jsonStr).obj
        val sunrise = json["results"]["sunrise"].string
        val sunset = json["results"]["sunset"].string

        val sunriseTime = ZonedDateTime.parse(sunrise)
        val sunsetTime = ZonedDateTime.parse(sunset)

        MainVerticle.SunInfo(sunriseTime.format(MainVerticle.formatter), sunsetTime.format(MainVerticle.formatter))
    }
}