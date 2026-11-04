package com.quicknotes.shahid.app.data.local.storage

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NoteFrontMatter(
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val color: String = "default",
    val createdIso: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val updatedIso: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val isArchived: Boolean = false,
    val isTrash: Boolean = false,
    val trashedAtEpoch: Long? = null,
    val folder: String = ""
)

object FrontMatterParser {

    private val FRONT_MATTER_REGEX = Regex("^---\\s*\\R([\\s\\S]*?)\\R---\\s*\\R?", RegexOption.MULTILINE)

    fun parse(rawContent: String): Pair<NoteFrontMatter, String> {
        val match = FRONT_MATTER_REGEX.find(rawContent)
        if (match == null) {
            val nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            return Pair(NoteFrontMatter(createdIso = nowIso, updatedIso = nowIso), rawContent)
        }

        val yamlContent = match.groupValues[1]
        val bodyContent = rawContent.substring(match.range.last + 1)

        var tags = listOf<String>()
        var pinned = false
        var color = "default"
        var created = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        var updated = created
        var isArchived = false
        var isTrash = false
        var trashedAt: Long? = null
        var folder = ""

        val lines = yamlContent.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().lowercase(Locale.ROOT)
                val value = line.substring(colonIndex + 1).trim()
                when (key) {
                    "tags" -> {
                        if (value.startsWith("[") && value.endsWith("]")) {
                            tags = value.removeSurrounding("[", "]")
                                .split(",")
                                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                .filter { it.isNotEmpty() }
                        } else if (value.isEmpty()) {
                            val collected = mutableListOf<String>()
                            while (i + 1 < lines.size && lines[i + 1].trim().startsWith("-")) {
                                i++
                                val tagItem = lines[i].trim().removePrefix("-").trim()
                                if (tagItem.isNotEmpty()) collected.add(tagItem)
                            }
                            tags = collected
                        } else {
                            tags = listOf(value.removeSurrounding("\"").removeSurrounding("'"))
                        }
                    }
                    "pinned" -> pinned = value.toBoolean()
                    "color" -> color = value.removeSurrounding("\"").removeSurrounding("'")
                    "created" -> if (value.isNotEmpty()) created = value
                    "updated" -> if (value.isNotEmpty()) updated = value
                    "archived" -> isArchived = value.toBoolean()
                    "trash", "trashed" -> isTrash = value.toBoolean()
                    "trashedat" -> trashedAt = value.toLongOrNull()
                    "folder" -> folder = value.removeSurrounding("\"").removeSurrounding("'")
                }
            }
            i++
        }

        return Pair(
            NoteFrontMatter(
                tags = tags,
                pinned = pinned,
                color = color,
                createdIso = created,
                updatedIso = updated,
                isArchived = isArchived,
                isTrash = isTrash,
                trashedAtEpoch = trashedAt,
                folder = folder
            ),
            bodyContent
        )
    }

    fun serialize(frontMatter: NoteFrontMatter, body: String): String {
        val tagListStr = frontMatter.tags.joinToString(", ") { it.trim() }
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("tags: [$tagListStr]\n")
        sb.append("pinned: ${frontMatter.pinned}\n")
        sb.append("color: ${frontMatter.color}\n")
        sb.append("created: ${frontMatter.createdIso}\n")
        sb.append("updated: ${frontMatter.updatedIso}\n")
        if (frontMatter.isArchived) {
            sb.append("archived: true\n")
        }
        if (frontMatter.isTrash) {
            sb.append("trash: true\n")
            frontMatter.trashedAtEpoch?.let { sb.append("trashedAt: $it\n") }
        }
        if (frontMatter.folder.isNotEmpty()) {
            sb.append("folder: ${frontMatter.folder}\n")
        }
        sb.append("---\n")
        sb.append(body)
        return sb.toString()
    }

    fun extractTitle(body: String, fallback: String): String {
        for (line in body.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                val clean = trimmed.replace(Regex("^#+\\s*"), "").trim()
                if (clean.isNotEmpty()) return clean
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("---")) {
                return trimmed.take(60)
            }
        }
        return fallback
    }

    fun stripMarkdownForPreview(body: String, maxChars: Int = 180): String {
        var clean = body
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("!\\[[^\\]]*\\]\\([^)]*\\)"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]*\\)"), "$1")
            .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^[\\s*\\-\\+]+(\\[[\\sxX]\\])?\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("^\\d+\\.\\s*", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("*", "")
            .replace("~~", "")
            .replace("==", "")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

        return if (clean.length > maxChars) {
            clean.take(maxChars).trimEnd() + "..."
        } else {
            clean
        }
    }
}
