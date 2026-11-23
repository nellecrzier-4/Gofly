package com.nellecrzier.gofly.ui.preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Palet warna Material 3 untuk blok Markdown, diambil dari tema di lapisan
 * composable lalu diteruskan ke penyusun AnnotatedString.
 */
internal data class MarkdownColors(
    val text: Color = Color.Unspecified,
    val muted: Color = Color.Unspecified,
    val faint: Color = Color.Unspecified,
    val accent: Color = Color.Unspecified,
    val codeFill: Color = Color.Unspecified,
)

/**
 * Markdown → AnnotatedString. Tidak ada HTML dan tidak ada parser pihak
 * ketiga: cukup blok yang dipakai di README (judul, tebal/miring, kode
 * inline, blok kode berpagar, daftar, kutipan, tautan, garis pemisah).
 *
 * Tautan hanya diberi gaya aksen bergaris bawah; pratinjau tidak membuka
 * Intent apa pun — aksi "Buka dengan" tetap milik pemanggil.
 */
internal fun buildMarkdownAnnotatedString(
    source: String,
    colors: MarkdownColors = MarkdownColors(),
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val paragraph = StringBuilder()
    val code = StringBuilder()
    var inFence = false
    var listOpen = false

    fun blockGap() {
        if (builder.length > 0) builder.append("\n\n")
    }

    fun flushCode() {
        if (code.isEmpty()) return
        blockGap()
        builder.withStyle(codeBlockStyle(colors)) {
            append(code.toString().trimEnd('\n'))
        }
        code.clear()
        listOpen = false
    }

    fun flushParagraph() {
        if (paragraph.isEmpty()) return
        blockGap()
        builder.appendInline(paragraph.toString(), colors)
        paragraph.clear()
        listOpen = false
    }

    for (raw in lines) {
        val trimmed = raw.trim()

        if (inFence) {
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inFence = false
                flushCode()
            } else {
                code.append("  ").append(raw).append('\n')
            }
            continue
        }

        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            flushParagraph()
            inFence = true
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            listOpen = false
            continue
        }

        val heading = HEADING.find(trimmed)
        if (heading != null) {
            flushParagraph()
            blockGap()
            val level = heading.groupValues[1].length
            builder.withStyle(headingStyle(level, colors)) {
                appendInline(heading.groupValues[2], colors)
            }
            continue
        }

        if (RULE.matches(trimmed)) {
            flushParagraph()
            blockGap()
            builder.withStyle(ruleStyle(colors)) { append("────────────────────") }
            continue
        }

        if (trimmed.startsWith(">")) {
            flushParagraph()
            blockGap()
            builder.withStyle(quoteStyle(colors)) {
                append("\u258D  ")
                appendInline(trimmed.removePrefix(">").trimStart(), colors)
            }
            continue
        }

        val bullet = BULLET.find(raw)
        if (bullet != null) {
            if (!listOpen) {
                flushParagraph()
                blockGap()
            } else {
                builder.append("\n")
            }
            val depth = (bullet.groupValues[1].length / 2).coerceIn(0, 4)
            builder.append("   ".repeat(depth) + "\u2022  ")
            builder.appendInline(bullet.groupValues[2], colors)
            listOpen = true
            continue
        }

        val ordered = ORDERED.find(raw)
        if (ordered != null) {
            if (!listOpen) {
                flushParagraph()
                blockGap()
            } else {
                builder.append("\n")
            }
            val depth = (ordered.groupValues[1].length / 2).coerceIn(0, 4)
            builder.append("   ".repeat(depth) + ordered.groupValues[2] + ".  ")
            builder.appendInline(ordered.groupValues[3], colors)
            listOpen = true
            continue
        }

        // Baris biasa: gabungkan dengan paragraf berjalan seperti Markdown.
        if (paragraph.isNotEmpty()) paragraph.append(' ')
        paragraph.append(trimmed)
    }

    flushParagraph()
    flushCode()
    return builder.toAnnotatedString()
}

private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET = Regex("^(\\s*)[-*+]\\s+(.*)$")
private val ORDERED = Regex("^(\\s*)(\\d{1,3})[.)]\\s+(.*)$")
private val RULE = Regex("^([-*_])\\1{2,}$")

private val INLINE = Regex(
    "(`[^`]+`)" +
        "|(\\*\\*[^*]+\\*\\*)" +
        "|(__[^_]+__)" +
        "|(~~[^~]+~~)" +
        "|(!\\[[^\\]]*\\]\\([^)\\s]+\\))" +
        "|(\\[[^\\]]+\\]\\([^)\\s]+\\))" +
        "|(\\*[^*\\n]+\\*)" +
        "|(_[^_\\n]+_)",
)

private fun AnnotatedString.Builder.appendInline(text: String, colors: MarkdownColors) {
    var cursor = 0
    for (match in INLINE.findAll(text)) {
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("`") -> withStyle(inlineCodeStyle(colors)) {
                append(token.removeSurrounding("`"))
            }
            token.startsWith("**") -> withStyle(BOLD_STYLE) {
                append(token.removeSurrounding("**"))
            }
            token.startsWith("__") -> withStyle(BOLD_STYLE) {
                append(token.removeSurrounding("__"))
            }
            token.startsWith("~~") -> withStyle(STRIKE_STYLE) {
                append(token.removeSurrounding("~~"))
            }
            token.startsWith("![") -> withStyle(linkStyle(colors)) {
                append(token.substringAfter("![").substringBefore("]"))
            }
            token.startsWith("[") -> withStyle(linkStyle(colors)) {
                append(token.substringAfter("[").substringBefore("]"))
            }
            token.startsWith("*") -> withStyle(ITALIC_STYLE) {
                append(token.removeSurrounding("*"))
            }
            else -> withStyle(ITALIC_STYLE) {
                append(token.removeSurrounding("_"))
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

private fun headingStyle(level: Int, colors: MarkdownColors): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 27.sp, fontWeight = FontWeight.Bold, color = colors.text)
    2 -> SpanStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold, color = colors.text)
    3 -> SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.text)
    4 -> SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
    else -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
}

private val BOLD_STYLE = SpanStyle(fontWeight = FontWeight.Bold)
private val ITALIC_STYLE = SpanStyle(fontStyle = FontStyle.Italic)
private val STRIKE_STYLE = SpanStyle(textDecoration = TextDecoration.LineThrough)

private fun linkStyle(colors: MarkdownColors): SpanStyle =
    SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline)

private fun quoteStyle(colors: MarkdownColors): SpanStyle = SpanStyle(color = colors.muted)

private fun ruleStyle(colors: MarkdownColors): SpanStyle = SpanStyle(color = colors.faint)

private fun inlineCodeStyle(colors: MarkdownColors): SpanStyle = SpanStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    color = colors.text,
    background = colors.codeFill,
)

private fun codeBlockStyle(colors: MarkdownColors): SpanStyle = SpanStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    color = colors.text,
    background = colors.codeFill,
)
