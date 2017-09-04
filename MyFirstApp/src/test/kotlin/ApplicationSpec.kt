import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Response
import io.kotlintest.Duration
import io.kotlintest.eventually
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec
import services.SunService
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class ApplicationSpec : StringSpec() {
    init {
        "SunService must retrieve correct sunset and sunrise information" {
            val json = """{
                "results":{
                    "sunrise":"2016-04-14T20:18:12+00:00",
                    "sunset":"2016-04-15T07:31:52+00:00"
                }
            }"""
            val testResponse = Response()
            testResponse.dataStream = ByteArrayInputStream(json.toByteArray())
            val testClient = FuelTestClient(testResponse)
            FuelManager.instance.client = testClient

            val lat = -33.8830
            val lon = 151.2167
            val sunService = SunService()
            val resultP = sunService.getSunInfo(lat, lon)

            eventually(Duration(5, TimeUnit.SECONDS)) {
                val sunInfo = resultP.get()
                sunInfo.sunrise shouldBe "06:18:12"
            }
        }
    }
}
