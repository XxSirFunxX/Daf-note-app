package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val createdAt: Long,
    val updatedAt: Long,
    val folderName: String = "عمومی",
    val jsonContent: String
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val titleFa: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Entity(tableName = "note_definitions")
data class NoteDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val symbolTypeName: String,
    val categoryId: String,
    val durationValue: String = "1/4", // e.g. "1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64"
    val isRingSound: Boolean = false,
    val isDefault: Boolean = false,
    val isHidden: Boolean = false,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_symbols")
data class CustomSymbolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String = "CUSTOM",
    val isRingSound: Boolean = false,
    val isHidden: Boolean = false,
    val vectorData: String? = null,
    val imagePath: String? = null,
    val durationValue: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "help_topics")
data class HelpTopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val orderIndex: Int = 0,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)


