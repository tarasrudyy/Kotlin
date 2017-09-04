import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response

class FuelTestClient(val testResponse: Response) : Client {
    override fun executeRequest(request: Request): Response {
        return testResponse
    }
}
