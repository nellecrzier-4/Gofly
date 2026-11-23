package com.nellecrzier.gofly

import java.io.File

/**
 * Pengganti [AppStrings] untuk unit test (tanpa Context Android). Membaca
 * `res/values-id` langsung dari berkas XML, jadi pesan yang diuji sama dengan
 * yang dilihat pengguna berbahasa Indonesia. Kunci yang tidak ada di sana jatuh
 * ke `res/values` (Inggris), meniru perilaku fallback Android.
 */
object TestStrings {
    private val idByName: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private fun load(dir: String): Map<String, String> {
        val out = HashMap<String, String>()
        val folder = File("src/main/res/$dir")
        val entry = Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        folder.listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                entry.findAll(file.readText()).forEach { out[it.groupValues[1]] = unescape(it.groupValues[2].trim()) }
            }
        return out
    }

    private fun unescape(raw: String): String = raw
        .replace("\\n", "\n")
        .replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")

    fun install(language: String = "id") {
        val primary = load("values-$language")
        val fallback = load("values")
        AppStrings.resolver = { id, args ->
            val name = idByName[id]
            val template = name?.let { primary[it] ?: fallback[it] }.orEmpty()
            if (args.isEmpty()) template else String.format(template, *args)
        }
    }

    fun uninstall() {
        AppStrings.resolver = null
    }
}
