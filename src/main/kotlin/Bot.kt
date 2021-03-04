import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random
import org.glassfish.grizzly.Grizzly.logger
import org.telegram.telegrambots.meta.api.methods.BotApiMethod

import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import java.io.Serializable
import java.math.BigDecimal


fun main() {
    ApiContextInitializer.init()
    TelegramBotsApi().apply {
        registerBot(Bot())
    }
}

// https://imgflip.com/api
data class Box(val text: String, val x: Int, val y: Int, val width: Int, val height: Int, val color: String, val outline_color: String)
data class ImageFlip(val template_id: String, val username: String, val password: String, val text0: String, val text1: String, val font: String? = null, val max_font_size: String? = null, val boxes: List<Box>? = null)
data class ImageFlipResponseData(val url: String, val page_url: String)
data class ImageFlipResponse(val success: Boolean, val data: ImageFlipResponseData? = null, val error_message: String? = null)
data class DeepaiResponse(val status: String?, val err: String?, val id: String?, val output: String?)
data class QAOpenAIRequest(val prompt: String, val temperature: Int = 0, val max_tokens: Int = 100, val top_p: Int = 1, val frequency_penalty: Double = 0.0, val presence_penalty: Double = 0.0, val stop: List<String> = listOf("\n"))
data class QAOpenAIResponse(val id: String?, val `object`: String?, val created: Long?, val model: String?, val choices: List<QAOpenAIChoicesResponse>)
data class QAOpenAIChoicesResponse(val text: String?, val index: String?, val logprobs: String?, val finish_reason: String?)

class Bot : TelegramLongPollingBot() {

    private val rnd = Random
    private val react = listOf("kite", "surf", "wind")
    private val members = mapOf(
        "alex" to listOf("Meinst du Alex den Kite-Wolverine?!", "JESUS IS COMING 0xF00x9F0x9A0x80"),
        "stibu" to listOf("Yo dr Stibu chas drum o!", "Stibu? Dä wo immer mitem Kite umefailed?"),
        "isa" to listOf("Die Isa? o.O Die Upwind-Isa?!?", "Die Isa? Hei Sie trinkt imfau ou mate!"),
        "eli" to listOf("Hey Eli schreibt mal mit Doppel-ll! ;)"),
        "elli" to listOf("Übrigens Elli hat Koreander mega gern!")
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
    val templates = mapOf(
        "lama" to "123482963",
        "batman" to "438680",
        "monkey" to "148909805",
        "disaster" to "97984",
        "notsimple" to "61579",
        "smart" to "89370399",
        "notsure" to "61520",
        "yoda" to "14371066",
        "cheers" to "5496396",
        "hugall" to "61533",
        "sad" to "61539",
        "money" to "176908",
        "koala" to "27920",
        "suit" to "922147",
        "whatif" to "61516",
        "sparta" to "195389",
        "magic" to "32399536",
        "ned" to "18552174",
        "choice" to "87743020",
        "meeting" to "1035805",
        "skeleton" to "4087833",
        "itsfine" to "55311130",
        "grumpy" to "405658",
        "suggest" to "61581",
        "drevil" to "40945639",
        "math" to "80996545",
        "lonely" to "67452763",
        "luke" to "19194965",
        "pulp" to "124212",
        "drake" to "91998305"
    )

    private fun qaOpenai(chatId: Long, prompt: String): Message = execute(SendMessage().apply {
        setChatId(chatId)
        val fiveMinutes = 5*60*1000L
        runBlocking {
            val client = HttpClient(Apache) {
                install(JsonFeature)
                install(HttpTimeout) {
                    requestTimeoutMillis = fiveMinutes // 5 minutes
                    socketTimeoutMillis = fiveMinutes
                    connectTimeoutMillis = fiveMinutes
                }}
            val response = client.post<QAOpenAIResponse>("https://api.openai.com/v1/engines/davinci/completions"){
                body = QAOpenAIRequest(prompt = prompt)
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${System.getenv("OPENAI_API_KEY")}")
            }
            text = if (response.choices.isNotEmpty() && response.choices[0].text?.isNotBlank() == true) {
                if (response.choices.size > 1) {
                    response.choices.mapIndexed { i, v -> "$i: ${v.text}"}.joinToString { "\n" }
                } else {
                    response.choices[0].text
                }
                //setText(response.choices.joinToString {  })
            } else {
                "could not find any answer"
            }
            println(response)
            client.close()
        }
    })

    private fun sendBullshit(chatId: Long, text: String): Message = execute(SendMessage().apply {
        setChatId(chatId)
        val fiveMinutes = 5*60*1000L
        runBlocking {
            val client = HttpClient(Apache) {
                install(JsonFeature)
                install(HttpTimeout) {
                    requestTimeoutMillis = fiveMinutes // 5 minutes
                    socketTimeoutMillis = fiveMinutes
                    connectTimeoutMillis = fiveMinutes
                }}
            val response = client.submitForm<DeepaiResponse>(
                url = "https://api.deepai.org/api/text-generator",
                formParameters = Parameters.build {
                    append("text", text)
                }){
                    header("api-key", System.getenv("DEEPAI_API_KEY"))
                }
            if (response.err == null && response.output != null) {
                if (response.output.length > 400) // max 400 allowed
                    setText("${response.output.substring(0,390)}...")
                else
                    setText(response.output)
            }
            client.close()
        }
    })

    fun sendImage(chatId: Long, template: String, texts: List<String> = emptyList()) = execute(SendPhoto().apply {
        setChatId(chatId)
        setCaption(caption)
        runBlocking {
            val client = HttpClient(Apache) { install(JsonFeature) }
            val response = client.submitForm<ImageFlipResponse>(
                url = "https://api.imgflip.com/caption_image",
                formParameters = Parameters.build {
                    append("template_id", templates.getOrDefault(template, "123482963"))
                    append("username", System.getenv("IMGFLIP_USR"))
                    append("password", System.getenv("IMGFLIP_PWD"))
                    if (texts.size <= 2) {
                        append("text0", texts.getOrElse(0){""})
                        append("text1", texts.getOrElse(1){""})
                    } else {
                        texts.forEachIndexed { idx, text ->
                            append("boxes[$idx][text]", text)
                            append("boxes[$idx][color]", "#ffffff")
                            append("boxes[$idx][outline_color]", "#000000")
                        }
                    }
                })
            if (response.success && response.data != null) {
                setPhoto("-", URL(response.data.url).openStream())
            }
            print(response.toString())
            client.close()
        }
        // setPhoto(name, URL("https://i.imgflip.com/22bdq6.jpg").openStream())
        // setPhoto(caption, Thread.currentThread().contextClassLoader.getResourceAsStream(name))
    })

    override fun onUpdateReceived(update: Update) {
        if (update.message?.text != null) {
            val text = update.message.text.toLowerCase()
            println(text)
            val hated = hates.find { it in text }
            val loved = loves.find { it in text }?.let { it.find(text)!!.value }
            val chatId = update.message.chatId

            fun send(text: String) = execute(SendMessage(chatId, text))

            fun sendCatImage(mood: String?, caption: String?) = execute(SendPhoto().apply {
                setChatId(chatId)
                setCaption(caption)
                val m = if (mood != null) "/$mood" else ""
                val c = if (caption != null) "/says/${caption.replace("cat\\s*".toRegex(), "")}" else ""
                setPhoto("cat", URL("https://cataas.com/cat${m}${c}").openStream())
            })

            when {
                text.startsWith("/help") -> send("Chatte einfach ganz normal, ich werd schon etwas sagen, wenn ich etwas zu sagen habe...")
                text.startsWith("/meme") -> send(Bot().templates.keys.toString())
                listOf("cat").any { it in text } -> sendCatImage("", text)
                text.startsWith("meme") -> {
                    val l = text.split("/")
                    if (text.split("/").size > 1) {
                        sendImage(chatId, l[1], l.subList(2, l.size))
                    }
                }
                react.any { it in text } -> send(quotes.choose())
                members.any { it.key in text } -> {
                    val memberKey = members.keys.find { it in text }
                    members[memberKey]?.let { send(it.random()) }
                }
                text.toLowerCase() == "yes" -> send("No")
                text.toLowerCase() == "no" -> send("Yes")
                text.startsWith("bs/") || text.startsWith("bullshit/") -> {
                    val l = text.split("/")
                    if (text.split("/").size > 1) {
                        try {
                            sendBullshit(chatId, l[1])
                        } catch (e: TelegramApiRequestException) {
                            println(e.apiResponse + ": " + e.errorCode)
                        }
                    }
                }
                text.startsWith("q/") || text.startsWith("question/") || text.startsWith("frage/") -> {
                    val l = text.split("/")
                    if (text.split("/").size > 1) {
                        try {
                            qaOpenai(chatId, l[1])
                        } catch (e: TelegramApiRequestException) {
                            println(e.apiResponse + ": " + e.errorCode)
                        }
                    }
                }
                hated != null -> send("Ich mag nicht ${hated}!")
                loved != null -> send("Ich liebe ${loved}!")
                listOf("joke", "witz").any { it in text } -> jokes.choose().let {
                    if (it.image == null) send(it.text)
                    else sendImage(chatId, "lama")
                }
                //rnd.nextDouble() < .1 -> send(quotes.choose())
            }
        }
    }

    private fun <T> List<T>.choose() = this[rnd.nextInt(size)]

}
