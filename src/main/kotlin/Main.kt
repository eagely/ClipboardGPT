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
fun main() = runBlocking {
    val historyFile = File("history.txt")
    if(!File("history.txt").exists())
        withContext(Dispatchers.IO) {
            historyFile.createNewFile()
        }
    val history = historyFile.readText()
    val apiKey = System.getenv("OPENAI_API_KEY")
    val token = requireNotNull(apiKey) { throw IllegalArgumentException("OPENAI_API_KEY environment variable not set.") }
    val openAI = OpenAI(OpenAIConfig(token, LogLevel.None))
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    var output = ""

    val transferable = clipboard.getContents(null)
    val chatCompletionRequest = ChatCompletionRequest(
        model = ModelId("gpt-3.5-turbo"),
        messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = history
            ),
            ChatMessage(
                role = ChatRole.User,
                content = if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) (withContext(
                    Dispatchers.IO
                ) {
                    transferable.getTransferData(DataFlavor.stringFlavor)
                } as String).also { println(it); File("history.txt").appendText(it + "\n") } else throw IllegalArgumentException("Clipboard does not contain text.")
            )
        )
    )

    openAI.chatCompletions(chatCompletionRequest)
        .onEach { output += it.choices.first().delta?.content.orEmpty() }
        .launchIn(this)
        .join()

    clipboard.setContents(StringSelection(output), StringSelection(output))
    println(output)
}
