package services

import com.github.kittinunf.fuel.httpGet
import com.github.salomonbrys.kotson.double
import com.github.salomonbrys.kotson.get
import com.github.salomonbrys.kotson.obj
import com.google.gson.JsonParser
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import java.nio.charset.Charset

class WeatherService {

    fun getTemperature(lat: Double, lon: Double): Promise<Double, Exception> = task {
        val weatherURL = "http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon" +
                "&units=metric&appId=4d16bc7d121828c578c00b5f4c5dbd58"
        val (_, response) = weatherURL.httpGet().responseString()
        val jsonStr = String(response.data, Charset.forName("UTF-8"))
        val json = JsonParser().parse(jsonStr).obj
        json["main"]["temp"].double
    }
}