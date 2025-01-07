package co.stonephone.stonecamera.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A Compose-aware property delegate to automatically persist a value in SharedPreferences.
 *
 * How it works:
 *  1. We store the value in a Compose [MutableState].
 *  2. Whenever you set the property, we update the state *and* write to SharedPreferences.
 *  3. Composables that read the property (e.g. `val myVal by someVM::prop`) will automatically
 *     recompose if the property value changes.
 *
 * Usage in a ViewModel:
 *
 *   class MyViewModel(context: Context) : ViewModel() {
 *       var flashMode by PrefStateDelegate(context, "flash_mode", "OFF")
 *       var selectedAspectRatio by PrefStateDelegate(context, "aspect_ratio", "16:9")
 *       // ...
 *   }
 *
 *   @Composable
 *   fun SomeScreen(viewModel: MyViewModel = viewModel()) {
 *       // Because we do `val flash = viewModel.flashMode`, Compose will recompose if flashMode changes
 *       val flash = viewModel.flashMode
 *       Text("Flash is $flash")
 *   }
 */
class PrefStateDelegate<T>(
    context: Context,
    private val key: String,
    private val defaultValue: T
) : ReadWriteProperty<Any?, T> {

    // In production, you might pass the SharedPreferences name or mode as constructor params.
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("stone_camera_prefs", Context.MODE_PRIVATE)
    }

    // We read from SharedPreferences **once** at init, then store in a MutableState for Compose
    private val state = mutableStateOf(loadValueFromPrefs())

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // Compose will watch this `state.value`.
        // We only need to read from SharedPreferences once at initialization,
        // so here we just return the in-memory state.
        return state.value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        // Update the Compose state so the UI will recompose
        state.value = value
        // Write the new value to SharedPreferences
        saveValueToPrefs(value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadValueFromPrefs(): T {
        return when (defaultValue) {
            is String  -> prefs.getString(key, defaultValue) as T
            is Boolean -> prefs.getBoolean(key, defaultValue) as T
            is Int     -> prefs.getInt(key, defaultValue) as T
            is Float   -> prefs.getFloat(key, defaultValue) as T
            is Long    -> prefs.getLong(key, defaultValue) as T
            else       -> throw IllegalArgumentException("Unsupported type ${defaultValue!!::class.java}")
        }
    }

    private fun saveValueToPrefs(value: T) {
        with(prefs.edit()) {
            when (value) {
                is String  -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int     -> putInt(key, value)
                is Float   -> putFloat(key, value)
                is Long    -> putLong(key, value)
                else       -> throw IllegalArgumentException("Unsupported type ${value!!::class.java}")
            }
            apply()
        }
    }
}
