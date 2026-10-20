package com.example.model

import java.util.UUID

data class ScoreProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "پروژه نت‌نویسی جدید",
    val author: String = "هنرمند",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderId: String? = null,
    val folderName: String? = null,
    val pages: List<ScorePage> = listOf(ScorePage()),
    val activePageIndex: Int = 0,
    val version: Int = 1,
    val isCorrupted: Boolean = false,
    val rawCorruptJson: String? = null,
    val parseError: String? = null
) {
    val activePage: ScorePage
        get() = pages.getOrElse(activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))) {
            ScorePage()
        }

    fun updateActivePage(update: (ScorePage) -> ScorePage): ScoreProject {
        val updatedPages = pages.toMutableList()
        val index = activePageIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        if (index < updatedPages.size) {
            updatedPages[index] = update(updatedPages[index])
        }
        return copy(pages = updatedPages, updatedAt = System.currentTimeMillis())
    }
}
