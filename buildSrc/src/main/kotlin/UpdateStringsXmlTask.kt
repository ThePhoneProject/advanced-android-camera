import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

abstract class ExtractTranslatableStringsTask : DefaultTask() {

    init {
        group = "i18n"
        description = "Extracts translatable strings and writes them to strings.xml"
    }

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputFile
    abstract val stringsFile: DirectoryProperty

    @TaskAction
    fun extractStrings() {
        val sourceDir = sourceDir.get().asFile
        val stringsXmlFile = stringsFile.get().asFile
        val translatableStrings = mutableSetOf<String>()

        // Scan files for @Translatable annotations and extract strings
        sourceDir.walkTopDown()
            .filter { it.isFile && it.extension in listOf("kt", "java") }
            .forEach { file ->
                val regex = Regex("@Translatable\\s*\\\"(.*?)\\\"")
                file.readLines().forEach { line ->
                    regex.findAll(line).forEach { match ->
                        translatableStrings.add(match.groupValues[1])
                    }
                }
            }

        // Load existing strings.xml or create a new one
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val document = if (stringsXmlFile.exists()) {
            docBuilder.parse(stringsXmlFile)
        } else {
            docBuilder.newDocument().apply {
                appendChild(createElement("resources"))
            }
        }

        val resources = document.documentElement

        // Collect existing keys and their values
        val existingStrings = mutableMapOf<String, String>()
        val existingNodes = mutableListOf<org.w3c.dom.Node>()
        for (i in 0 until resources.childNodes.length) {
            val node = resources.childNodes.item(i)
            if (node.nodeName == "string" && node.hasAttributes()) {
                val key = node.attributes.getNamedItem("name").nodeValue
                existingStrings[key] = node.textContent.trim()
                existingNodes.add(node)
            }
        }

        // Update or add new strings
        translatableStrings.forEach { rawKey ->
            val sanitizedKey = sanitizeKey(rawKey)
            if (!existingStrings.containsKey(sanitizedKey)) {
                val stringNode = document.createElement("string").apply {
                    setAttribute("name", sanitizedKey)
                    textContent = rawKey.trim()
                }
                resources.appendChild(stringNode)
            } else {
                // Mark as used by removing unused="true"
                val node = existingNodes.firstOrNull { it.attributes.getNamedItem("name").nodeValue == sanitizedKey }
                try {
                    node?.attributes?.removeNamedItem("unused")
                } catch (e: Exception) {
                    // Ignore if attribute doesn't exist
                }
            }
        }

        // Mark unused strings
        existingNodes.forEach { node ->
            val key = node.attributes.getNamedItem("name").nodeValue
            if (key !in translatableStrings.map { sanitizeKey(it) }) {
                node.attributes.setNamedItem(document.createAttribute("unused").apply { nodeValue = "true" })
            }
        }

        // Write back to strings.xml
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        }
        transformer.transform(DOMSource(document), StreamResult(stringsXmlFile))

        println("strings.xml updated with ${translatableStrings.size} translatable strings.")
    }

    /**
     * Sanitizes a string to make it a valid XML key.
     */
    private fun sanitizeKey(key: String): String {
        val sanitized = key.replace(Regex("[^a-zA-Z0-9]"), "_")
        return if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
    }
}
