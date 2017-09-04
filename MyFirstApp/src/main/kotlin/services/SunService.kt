package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonParser
import models.SunInfo
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.nio.charset.Charset
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SunService {

    companion object {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Australia/Sydney"))
    }

    fun getSunInfo(lat: Double, lon: Double): Promise<SunInfo, Exception> = task {
        val sunIfoURL = "http://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&formatted=0"
        val (_, response) = sunIfoURL.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))
        val json = JsonParser().parse(jsonStr).obj
        val sunrise = json["results"]["sunrise"].string
        val sunset = json["results"]["sunset"].string

        val sunriseTime = ZonedDateTime.parse(sunrise)
        val sunsetTime = ZonedDateTime.parse(sunset)

        SunInfo(sunriseTime.format(SunService.formatter), sunsetTime.format(SunService.formatter))
    }
}
