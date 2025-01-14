package co.stonephone.stonecamera.utils

import android.content.Context
import android.content.res.Configuration
import co.stonephone.stonecamera.R
import java.util.*

/**
 * A global object to manage translations.
 */
object TranslationManager {

    private var translations: Map<String, String> = emptyMap()

    /**
     * Loads translations for the current device locale.
     */
    fun loadTranslationsForLocale(context: Context, locale: Locale = Locale.getDefault()) {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        val localizedContext = context.createConfigurationContext(configuration)

        translations = loadStringsFromResources(localizedContext)
    }

    private fun sanitizeKey(key: String): String {
        val sanitized = key.replace(Regex("[^a-zA-Z0-9]"), "_")
        return if (sanitized.firstOrNull()?.isDigit() == true) "_$sanitized" else sanitized
    }

    /**
     * Retrieves a translation for a given key, falling back to the key itself if not found.
     */
    fun getTranslation(key: String): String {
        return translations[sanitizeKey(key)] ?: key
    }

    /**
     * Loads all string resources from the given context's `strings.xml`.
     */
    private fun loadStringsFromResources(context: Context): Map<String, String> {
        val stringMap = mutableMapOf<String, String>()
        val resources = context.resources
        val fields = R.string::class.java.declaredFields

        for (field in fields) {
            try {
                val resourceId = field.getInt(null)
                val value = resources.getString(resourceId)
                stringMap[field.name] = value
            } catch (e: Exception) {
                // Skip any non-string fields
            }
        }

        return stringMap
    }
}

/**
 * Represents a translatable string with optional parameters for placeholders.
 */
class TranslatableString(
    val raw: String,
) {

    /**
     * Resolves the string using the global TranslationManager.
     */
    fun resolve(params: Map<String, String>? = null): String {
        val template = TranslationManager.getTranslation(raw)
        return params?.entries?.fold(template) { acc, (placeholder, value) ->
            acc.replace("{$placeholder}", value)
        } ?: template
    }

    /**
     * Overrides equality to compare based on raw string and parameters.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranslatableString) return false

        return raw == other.raw
    }

    /**
     * Overrides hashCode to ensure consistent hashing based on raw and params.
     */
    override fun hashCode(): Int {
        var result = raw.hashCode()
        result *= 31
        return result
    }

    /**
     * Returns a readable representation for debugging.
     */
    override fun toString(): String {
        return raw
    }
}

/**
 * Annotation to mark a translatable string.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.EXPRESSION, AnnotationTarget.LOCAL_VARIABLE
)
annotation class Translatable

/**
 * Extension function for creating a translatable string.
 */
fun String.i18n() =
    TranslatableString(this)