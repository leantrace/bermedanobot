import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpResponse
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

fun main() {
    ApiContextInitializer.init()
    TelegramBotsApi().apply {
        registerBot(Bot())
    }
}

class Bot : TelegramLongPollingBot() {

    private val rnd = Random
    private val react = listOf("kite", "surf", "wind").map { Regex(it) }
    private val members = mapOf(
        "alex" to listOf("Meinst du Alex den Kite-Wolverine?!", "JESUS IS COMING 0xF00x9F0x9A0x80"),
        "stibu" to listOf("Yo dr Stibu chas drum o!", "Stibu? Dä wo immer mitem Kite umefailed?"),
        "isa" to listOf("Die Isa? o.O Die Upwind-Isa?!?", "Die Isa? Hei Sie trinkt imfau ou mate!")
    )

    init {
        println("=== Init Bot ===")
        if (System.getenv("BOT_USER") == null) {
            throw RuntimeException("Set BOT_USER environment variable. If you need to generate it, contact: https://telegram.me/botfather")
        }
        if (System.getenv("BOT_TOKEN") == null) {
            throw RuntimeException("Set BOT_TOKEN environment variable. If you need to generate it, contact: https://telegram.me/botfather")
        }
        if (System.getenv("IMGFLIP_PWD") == null) {
            throw RuntimeException("Set IMGFLIP_PWD environment variable.")
        }
        if (System.getenv("IMGFLIP_USR") == null) {
            throw RuntimeException("Set IMGFLIP_USR environment variable.")
        }
    }

    override fun getBotToken() = System.getenv("BOT_TOKEN")
    override fun getBotUsername() = System.getenv("BOT_USER")

    fun sendPostRequest(chatId: Long, userName: String, password: String) {

        var reqParam = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(userName, "UTF-8")
        reqParam += "&" + URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8")
        reqParam += "&template_id=438680&text0=yes&text1=no"
        val mURL = URL("https://api.imgflip.com/caption_image")

        with(mURL.openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "POST"

            val wr = OutputStreamWriter(getOutputStream());
            wr.write(reqParam);
            wr.flush();

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response : $response")
                execute(SendMessage(chatId, "$response"))
            }
        }
    }

    override fun onUpdateReceived(update: Update) {
        if (update.message?.text != null) {
            val text = update.message.text.toLowerCase()
            println(text)
            val hated = hates.find { it in text }
            val loved = loves.find { it in text }?.let { it.find(text)!!.value }
            val chatId = update.message.chatId

            fun send(text: String) = execute(SendMessage(chatId, text))

            // https://imgflip.com/api
            data class Box(
                val text: String, val x: Int, val y: Int, val width: Int, val height: Int,
                val color: String, val outline_color: String
            )

            data class ImageFlip(
                val template_id: String, val username: String, val password: String,
                val text0: String, val text1: String, val font: String? = null,
                val max_font_size: String? = null, val boxes: List<Box>? = null
            )

            data class ImageFlipResponseData(val url: String, val page_url: String)
            data class ImageFlipResponse(
                val success: String,
                val data: ImageFlipResponseData? = null,
                val error_message: String? = null
            )

            fun sendImage(name: String, caption: String) = execute(SendPhoto().apply {
                println("Send Photo: " + name)
                setChatId(chatId)
                setCaption(caption)
                runBlocking {
                    val client = HttpClient(Apache) { install(JsonFeature) }
                    send("send request to https://api.imgflip.com/caption_image/")
                    println(System.getenv("IMGFLIP_USR"))
                    println(System.getenv("IMGFLIP_PWD"))
                    val response = client.submitForm<ImageFlipResponse>(
                        url = "https://api.imgflip.com/caption_image",
                        formParameters = Parameters.build {
                            append("template_id", "123482963")
                            append("username", System.getenv("IMGFLIP_USR"))
                            append("password", System.getenv("IMGFLIP_PWD"))
                            append("text0", "Uff d..da..das habe ich nicht gewusst...")
                            append("text1", "...das tut mir leid")
                        })
                    print(response.toString())
                    /* send(response.success)
                    send(response.error_message ?: "")
                    send(response.data?.url ?: "")
                    send(response.data?.page_url ?: "")*/
                    client.close()
                }
                // setPhoto(name, URL("https://i.imgflip.com/22bdq6.jpg").openStream())
                setPhoto(caption, Thread.currentThread().contextClassLoader.getResourceAsStream(name))
            })

            fun sendCatImage(mood: String?, caption: String?) = execute(SendPhoto().apply {
                setChatId(chatId)
                setCaption(caption)
                val m = if (mood != null) "/$mood" else ""
                val c = if (caption != null) "/says/${caption.replace("cat\\s*".toRegex(), "")}" else ""
                setPhoto("cat", URL("https://cataas.com/cat${m}${c}").openStream())
            })

            when {
                text.startsWith("/help") -> send("Chatte einfach ganz normal, ich werd schon etwas sagen, wenn ich etwas zu sagen habe...")
                listOf("cat").any { it in text } -> sendCatImage("", text)
                listOf("meme").any { it in text } -> sendImage(memes.choose(), "meme")
                react.any { it in text } -> send(quotes.choose())
                members.any { it.key in text } -> {
                    val memberKey = members.keys.find { it in text }
                    members[memberKey]?.let { send(it.random()) }
                }
                text == "yes" -> {
                    send("No")
                }
                text == "marion" -> {
                    send("Hello Marion")
                }
                text == "memetest" -> {
                    sendPostRequest(chatId, "", "")
                }
                text == "no" -> send("Yes")
                hated != null -> send("Ich mag nicht ${hated}!")
                loved != null -> send("Ich liebe ${loved}!")
                listOf("joke", "witz").any { it in text } -> jokes.choose().let {
                    if (it.image == null) send(it.text)
                    else sendImage(it.image, it.text)
                }
                rnd.nextDouble() < .1 -> send(quotes.choose())
            }
        }
    }

    private fun <T> List<T>.choose() = this[rnd.nextInt(size)]

}
