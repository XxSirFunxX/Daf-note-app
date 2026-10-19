package com.example.model

import java.util.UUID

data class CustomSymbol(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isRingSound: Boolean = false,
    val vectorPathData: String? = null,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
