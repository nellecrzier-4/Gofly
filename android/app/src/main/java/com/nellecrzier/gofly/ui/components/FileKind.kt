package com.nellecrzier.gofly.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.vector.ImageVector
import com.nellecrzier.gofly.GItem
import com.nellecrzier.gofly.vm.FILTER_ALL
import com.nellecrzier.gofly.vm.FILTER_ARCHIVE
import com.nellecrzier.gofly.vm.FILTER_AUDIO
import com.nellecrzier.gofly.vm.FILTER_DOCUMENT
import com.nellecrzier.gofly.vm.FILTER_IMAGE
import com.nellecrzier.gofly.vm.FILTER_VIDEO
import java.util.Locale

/** Jenis file untuk ikon dan filter. Ditentukan dari `mimetype` + ekstensi. */
enum class FileKind {
    FOLDER, IMAGE, VIDEO, AUDIO, PDF, DOCUMENT, ARCHIVE, CODE, APK, OTHER;

    val icon: ImageVector
        get() = when (this) {
            FOLDER -> Icons.Filled.Folder
            IMAGE -> Icons.Filled.Image
            VIDEO -> Icons.Filled.Movie
            AUDIO -> Icons.Filled.MusicNote
            PDF -> Icons.Filled.PictureAsPdf
            DOCUMENT -> Icons.Filled.Description
            ARCHIVE -> Icons.Filled.FolderZip
            CODE -> Icons.Filled.Code
            APK -> Icons.Filled.Android
            OTHER -> Icons.Filled.InsertDriveFile
        }
}

private val ARCHIVE_EXT = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso")
private val CODE_EXT = setOf(
    "kt", "kts", "java", "js", "ts", "tsx", "jsx", "py", "rb", "go", "rs", "c", "h",
    "cpp", "hpp", "cs", "swift", "php", "sh", "bash", "zsh", "json", "xml", "yml",
    "yaml", "toml", "ini", "sql", "html", "css", "scss", "md", "txt",
)

fun GItem.fileKind(): FileKind {
    if (isFolder) return FileKind.FOLDER
    val mime = mimetype.lowercase(Locale.ROOT)
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    return when {
        mime.startsWith("image/") -> FileKind.IMAGE
        mime.startsWith("video/") -> FileKind.VIDEO
        mime.startsWith("audio/") -> FileKind.AUDIO
        mime == "application/pdf" || ext == "pdf" -> FileKind.PDF
        ext == "apk" || ext == "aab" -> FileKind.APK
        ext in ARCHIVE_EXT -> FileKind.ARCHIVE
        ext in CODE_EXT -> FileKind.CODE
        mime.startsWith("text/") ||
            mime.contains("word") ||
            mime.contains("excel") ||
            mime.contains("spreadsheet") ||
            mime.contains("presentation") ||
            mime.contains("powerpoint") -> FileKind.DOCUMENT
        else -> FileKind.OTHER
    }
}

/** Filter memakai mimetype, bukan sekadar ekstensi. */
fun GItem.matchesFilter(filter: String): Boolean {
    if (filter == FILTER_ALL) return true
    val kind = fileKind()
    if (kind == FileKind.FOLDER) return false
    return when (filter) {
        FILTER_IMAGE -> kind == FileKind.IMAGE
        FILTER_VIDEO -> kind == FileKind.VIDEO
        FILTER_AUDIO -> kind == FileKind.AUDIO
        FILTER_DOCUMENT -> kind == FileKind.DOCUMENT || kind == FileKind.PDF || kind == FileKind.CODE
        FILTER_ARCHIVE -> kind == FileKind.ARCHIVE
        else -> true
    }
}

/** Urutkan dengan folder selalu di atas. */
fun sortItems(items: List<GItem>, field: String, ascending: Boolean): List<GItem> {
    val comparator: Comparator<GItem> = when (field) {
        "size" -> compareBy { it.size }
        "created" -> compareBy { it.createTime }
        "modified" -> compareBy { it.modTime }
        "type" -> compareBy { it.fileKind().ordinal }
        else -> compareBy { it.name.lowercase(Locale.ROOT) }
    }
    val ordered = items.sortedWith(comparator)
    return (if (ascending) ordered else ordered.reversed())
        .sortedBy { if (it.isFolder) 0 else 1 }
}
