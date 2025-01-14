import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import java.net.HttpURLConnection
import java.net.URL

abstract class GenerateTranslationsTask : DefaultTask() {

    @get:InputDirectory
    abstract val resDir: DirectoryProperty

    private val apiKey: String =
        System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")

    @TaskAction
    fun generateTranslations() {
        val languages = listOf("es", "fr")
        val resDir = resDir.get().asFile

        val baseStringsFile = resDir.resolve("values/strings.xml")

        val translations = mutableMapOf<String, Map<String, String>>()
        val baseStrings = parseStringsXml(baseStringsFile)

        languages.forEach { lang ->
            val langStringsFile = resDir.resolve("values-$lang/strings.xml")
            val langStrings =
                if (langStringsFile.exists()) parseStringsXml(langStringsFile) else emptyMap()

            val missingKeys = baseStrings.keys - langStrings.keys

            if (missingKeys.isNotEmpty()) {
                val prompt = buildTranslationPrompt(baseStrings, langStrings, missingKeys, lang)
                val response = callGptForTranslations(prompt)

                // Parse GPT response
                val newTranslations = parseGptResponse(baseStrings, response)
                translations[lang] = newTranslations

                // Update the language file with new translations
                updateStringsXml(langStringsFile, langStrings + newTranslations)
            }
        }
    }

    private fun parseStringsXml(file: File): Map<String, String> {
        val doc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val elements = doc.getElementsByTagName("string")
        val strings = mutableMapOf<String, String>()

        for (i in 0 until elements.length) {
            val element = elements.item(i) as Element
            val name = element.getAttribute("name")
            val value = element.textContent
            strings[name] = value
        }
        return strings
    }

    private fun buildTranslationPrompt(
        baseStrings: Map<String, String>,
        existingTranslations: Map<String, String>,
        missingKeys: Set<String>,
        language: String
    ): String {
        val languageName = when (language) {
            "es" -> "Spanish"
            "fr" -> "French"
            else -> language.capitalize()
        }

        val existingPrompt =
            existingTranslations.entries.map { "- ${baseStrings[it.key]} -> ${it.value}" }
                .joinToString("\n")
        val requiredPrompt = missingKeys.map { baseStrings[it] }.joinToString("\n") { "- $it" }

        return """Please translate the following words (in the context of an Android camera app UI) from English to $languageName:

Existing Translations:
$existingPrompt

Required Translations:
$requiredPrompt

Respond with JSON."""
    }

    private fun callGptForTranslations(prompt: String): String {
        val apiUrl = "https://api.openai.com/v1/chat/completions"
        val requestBody = mapOf(
            "model" to "gpt-4o",
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to "You are a helpful assistant that translates UI text for Android apps."
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt
                )
            ),
            "response_format" to mapOf(
                "type" to "json_object"
            )
        )

        val jsonBody = jacksonObjectMapper().writeValueAsString(requestBody)

        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.outputStream.use { it.write(jsonBody.toByteArray()) }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun parseGptResponse(
        baseStrings: Map<String, String>,
        response: String
    ): Map<String, String> {
        val objectMapper = jacksonObjectMapper()
        val jsonResponse: Map<String, Any> = objectMapper.readValue(response)

        val choices = jsonResponse["choices"] as? List<Map<String, Any>> ?: emptyList()
        val messageContent =
            choices.firstOrNull()?.get("message")?.let { (it as Map<*, *>)["content"] } as? String
                ?: error("Invalid GPT response: $response")

        val res = objectMapper.readValue<Map<String, String>>(messageContent)
        val invertedBaseStrings = baseStrings.entries.associateBy({ it.value }, { it.key })
        return res.entries.map { (key, value) -> invertedBaseStrings[key]!! to value }.toMap()
    }

    private fun updateStringsXml(file: File, strings: Map<String, String>) {
        val content = buildStringsXmlContent(strings)
        file.writeText(content)
    }

    private fun buildStringsXmlContent(strings: Map<String, String>): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        builder.append("<resources>\n")
        strings.forEach { (key, value) ->
            val escapedValue = escapeXml(value)
            builder.append("    <string name=\"$key\">$escapedValue</string>\n")
        }
        builder.append("</resources>\n")
        return builder.toString()
    }

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "\\")
            .replace("'", "\\'")
    }
}