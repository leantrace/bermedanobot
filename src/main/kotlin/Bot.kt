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
    private val react = listOf("virus", "viral", "corona", "sick", "ill", "krank", "flu", "grippe", "fever", "fieber", "hust", "keuch", "cough", "gesundheit", "health", """home\s*office""").map { Regex(it) }
    private val beer = listOf("beer", "bier", "cerveza", "biere", "birra", "öl", "øl", "ale")
    private val country = listOf("mexican", "dutch", "swiss", "german", "czech", "spanish", "japanese", "chinese")
    private val taste = listOf("taste", "geschmack", "durst", "drink", "hungry", "hunger", "eat", "essen")

    private val members = mapOf (
            "sabi" to listOf("musst du nicht ins Beans & Nuts?"),
            "tinu" to listOf("geh Broforce zocken, bljat")
    )

    init {
        if (System.getenv("BOT_USER") == null) {
            throw RuntimeException("Set BOT_USER environment variable. If you need to generate it, contact: https://telegram.me/botfather")
        }
        if (System.getenv("BOT_TOKEN") == null) {
            throw RuntimeException("Set BOT_USER environment variable. If you need to generate it, contact: https://telegram.me/botfather")
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
                setChatId(chatId)
                setCaption(caption)
                setPhoto(name, URL("https://i.imgflip.com/22bdq6.jpg").openStream())
            })

            when {
                text.startsWith("/help") -> send("Chatte einfach ganz normal im WG chat, ich werd schon etwas sagen, wenn ich etwas zu sagen habe...")
                react.any { it in text } -> send(quotes.choose())
                beer.any { it in text } -> send("Can I have a ${country.choose()} beer, please?")
                /*members.any { it in text } -> {
                    val member = members.find { it in text }
                    if (member == "sabi") {
                        send("${member} musst du nicht ins Beans & Nuts?")
                    }
                    if (member == "tinu") {
                        send("${member} geh Broforce zocken, bljat")
                    }
                }*/
                text == "yes" -> {
                    sendPostRequest(chatId, "","")
                    // send ("No")
                }
                text == "no" -> send ("Yes")
                hated != null -> send("I hate ${hated}!")
                loved != null -> send("I love ${loved}!")
                taste.any { it in text } -> send("Try me! I have an excellent taste!")
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
