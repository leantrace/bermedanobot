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
    private val members = mapOf (
        "alex" to listOf("Meinst du Alex den Kite-Wolverine?!"),
        "stibu" to listOf("Yo dr Stibu has drum o!"),
        "isa" to listOf("Die Isa? o.O Die Upwind-Isa?!?")
    )

    init {
        println("=== Init Bot ===")
        if (System.getenv("BOT_USER") == null) {
            throw RuntimeException("Set BOT_USER environment variable. If you need to generate it, contact: https://telegram.me/botfather")
        }
        if (System.getenv("BOT_TOKEN") == null) {
            throw RuntimeException("Set BOT_TOKEN environment variable. If you need to generate it, contact: https://telegram.me/botfather")
        }
    }

    override fun getBotToken() = System.getenv("BOT_TOKEN")
    override fun getBotUsername() = System.getenv("BOT_USER")

    fun sendPostRequest(chatId: Long, userName:String, password:String) {

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

            fun sendImage(name: String, caption: String) = execute(SendPhoto().apply {
                println("Send Photo: "+name)
                setChatId(chatId)
                setCaption(caption)
                setPhoto(caption, Thread.currentThread().contextClassLoader.getResourceAsStream(name))
                // setPhoto(name, URL("https://i.imgflip.com/22bdq6.jpg").openStream())
            })

            fun sendCatImage(mood: String?, caption: String?) = execute(SendPhoto().apply {
                setChatId(chatId)
                setCaption(caption)
                val m = if (mood != null) "/$mood" else ""
                val c = if (caption != null) "/says/$caption" else ""
                setPhoto("cat", URL("https://cataas.com/cat${m}${c}").openStream())
            })

            when {
                text.startsWith("/help") -> send("Chatte einfach ganz normal, ich werd schon etwas sagen, wenn ich etwas zu sagen habe...")
                react.any { it in text } -> send(quotes.choose())
                members.any { it.key in text } -> {
                    val memberKey = members.keys.find { it in text }
                    members[memberKey]?.let { send(it.random()) }
                }
                text == "yes" -> {
                    send ("No")
                }
                text == "memetest" -> {
                    sendPostRequest(chatId, "","")
                }
                text == "no" -> send ("Yes")
                hated != null -> send("I hate ${hated}!")
                loved != null -> send("I love ${loved}!")
                listOf("cat").any { it in text } -> sendCatImage("",text)
                listOf("meme").any { it in text } -> sendImage(memes.choose(), "meme")
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
