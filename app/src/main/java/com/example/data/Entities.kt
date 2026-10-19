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

@Entity(tableName = "custom_symbols")
data class CustomSymbolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isRingSound: Boolean,
    val vectorData: String? = null,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
