package dstd.github.keepitquiet.utils

import android.content.Context
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class SimplePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(this::class.java.simpleName, Context.MODE_PRIVATE)

    inner class IntPreference(private val default: Int): ReadWriteProperty<Any, Int> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Int {
            return prefs.getInt(property.name, default)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            prefs.edit().putInt(property.name, value).apply()
        }
    }

    inner class BoolPreference(private val default: Boolean): ReadWriteProperty<Any, Boolean> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return prefs.getBoolean(property.name, default)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            prefs.edit().putBoolean(property.name, value).apply()
        }
    }

    val optionalIntPreference = object : ReadWriteProperty<Any, Int?> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Int? {
            return if (prefs.contains(property.name)) prefs.getInt(property.name, 0) else null
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Int?) {
            if (value != null)
                prefs.edit().putInt(property.name, value).apply()
            else
                prefs.edit().remove(property.name).apply()
        }
    }

    val optionalBoolPreference = object : ReadWriteProperty<Any, Boolean?> {
        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean? {
            return if (prefs.contains(property.name)) prefs.getBoolean(property.name, false) else null
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean?) {
            if (value != null)
                prefs.edit().putBoolean(property.name, value).apply()
            else
                prefs.edit().remove(property.name).apply()
        }
    }
}
