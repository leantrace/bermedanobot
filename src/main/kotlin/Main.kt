import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

data class ImageFlipResponseData(val url: String, val page_url: String)
data class ImageFlipResponse(
    val success: Boolean,
    val data: ImageFlipResponseData? = null,
    val error_message: String? = null
)
fun main(args: Array<String>) {
    runBlocking {
        val client = HttpClient(Apache) { install(JsonFeature) }
        val response = client.submitForm<ImageFlipResponse>(
            url = "https://api.imgflip.com/caption_image",
            formParameters = Parameters.build {
                append("template_id", "123482963")
                append("username", System.getenv("u"))
                append("password", System.getenv("p"))
                append("text0", "Uff d..da..das habe ich nicht gewusst...")
                append("text1", "...Uff d..da..das tut mir leid")
            })
        print(response.toString())
        /* send(response.success)
        send(response.error_message ?: "")
        send(response.data?.url ?: "")
        send(response.data?.page_url ?: "")*/
        client.close()
    }
}
