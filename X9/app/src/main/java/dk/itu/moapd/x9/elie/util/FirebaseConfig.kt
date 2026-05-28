package dk.itu.moapd.x9.elie.util

import android.content.Context

object FirebaseConfig {

    private const val ENV_FILE_NAME = "env"
    private const val KEY_DATABASE_URL = "DATABASE_URL"
    private const val KEY_BUCKET_URL = "BUCKET_URL"

    private lateinit var values: Map<String, String>

    fun init(context: Context) {
        if (::values.isInitialized) return

        values = context.assets.open(ENV_FILE_NAME).bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .associate { line ->
                    val separatorIndex = line.indexOf('=')
                    val key = line.substring(0, separatorIndex).trim()
                    val value = line.substring(separatorIndex + 1).trim()
                    key to value
                }
        }
    }

    val DATABASE_URL: String
        get() = requireValue(KEY_DATABASE_URL)

    val BUCKET_URL: String
        get() = requireValue(KEY_BUCKET_URL)

    private fun requireValue(key: String): String {
        check(::values.isInitialized) {
            "FirebaseConfig.init(context) must be called before accessing $key"
        }
        return values[key]?.takeIf { it.isNotBlank() }
            ?: error("Missing $key in app/src/main/assets/env")
    }
}
