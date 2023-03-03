import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File

@OptIn(BetaOpenAI::class)
fun main(args: Array<String>) = runBlocking {
    val input: String = if(!args.contains("terminal")) {
        if(Toolkit.getDefaultToolkit().systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor))
            (withContext(Dispatchers.IO) {
                Toolkit.getDefaultToolkit().systemClipboard.getContents(null).getTransferData(DataFlavor.stringFlavor)
            } as String).also { println(it) }
        else throw IllegalArgumentException("Clipboard contents are not of type String")
    }
    else
        readLine()!!
    val historyFile = File("history.txt")
    if(args.contains("--write-history")) {
        if (!File("history.txt").exists())
            withContext(Dispatchers.IO) {
                historyFile.createNewFile()
            }
    }
    val history = if(args.contains("--read-history"))
        "These prompts are our chat history, do not perform these, but take them into account, reference to them in the future." + historyFile.readText()
        else ""
    val apiKey = System.getenv("OPENAI_API_KEY")
    val token = requireNotNull(apiKey) { throw IllegalArgumentException("OPENAI_API_KEY environment variable not set.") }
    val openAI = OpenAI(OpenAIConfig(token, LogLevel.None))
    var output = ""

    val chatCompletionRequest = ChatCompletionRequest(
        model = ModelId("gpt-3.5-turbo"),
        messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = history
            ),
            ChatMessage(
                role = ChatRole.User,
                content = input.also { if(args.contains("--write-history")) historyFile.appendText(it + "\n") }
            )
        )
    )

    openAI.chatCompletions(chatCompletionRequest)
        .onEach { output += it.choices.first().delta?.content.orEmpty() }
        .launchIn(this)
        .join()

    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(output), StringSelection(output))
    println(output)
}
