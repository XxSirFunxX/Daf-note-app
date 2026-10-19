package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.CustomSymbolEntity
import com.example.data.NoteDefinitionEntity
import com.example.model.ScoreSymbol
import com.example.model.SymbolCategory
import com.example.model.SymbolType
import com.example.render.ScoreRenderer
import java.io.File

@Composable
fun SymbolLibrarySheet(
    categories: List<CategoryEntity>,
    activeCategoryId: String,
    activeSymbol: SymbolType?,
    activeCustomSymbolId: String? = null,
    customSymbols: List<CustomSymbolEntity>,
    noteDefinitions: List<NoteDefinitionEntity>,
    onSelectCategory: (String) -> Unit,
    onSelectSymbol: (SymbolType) -> Unit,
    onSelectCustomSymbol: (CustomSymbolEntity) -> Unit,
    onOpenNoteManager: () -> Unit,
    onOpenCategoryManager: () -> Unit,
    onOpenCustomManager: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayCategories = if (categories.isNotEmpty()) {
        categories
    } else {
        listOf(
            CategoryEntity("NOTES", "نت‌ها", 0, isDefault = true),
            CategoryEntity("RESTS", "سکوت‌ها", 1, isDefault = true),
            CategoryEntity("RING_SOUNDS", "صدای حلقه‌ها", 2, isDefault = true),
            CategoryEntity("TECHNIQUES", "تکنیک و مضراب", 3, isDefault = true),
            CategoryEntity("BAR_LINES", "خطوط میزان", 4, isDefault = true),
            CategoryEntity("TIME_SIGNATURES", "میزان‌نما", 5, isDefault = true),
            CategoryEntity("TEMPO_TEXT", "متن و علائم", 6, isDefault = true),
            CategoryEntity("CUSTOM", "سفارشی", 7, isDefault = true)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Category Tabs Header with Management Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayCategories.forEach { category ->
                        val isSelected = category.id == activeCategoryId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCategory(category.id) },
                            label = {
                                Text(
                                    text = category.titleFa,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("category_chip_${category.id}")
                        )
                    }
                }

                // Quick buttons to open Note Definitions and Category Managers
                IconButton(
                    onClick = onOpenNoteManager,
                    modifier = Modifier.size(36.dp).testTag("btn_open_note_manager")
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "مدیریت تعاریف نت‌ها",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenCategoryManager,
                    modifier = Modifier.size(36.dp).testTag("btn_open_category_manager")
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "مدیریت دسته‌بندی‌ها",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Special Banner for Ring Sounds Rule
            if (activeCategoryId == "RING_SOUNDS") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBE6))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "قانون خط چهارم",
                        tint = Color(0xFFB23A22),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "قانون اختصاصی: نمادهای صدای حلقه‌ها منحصراً روی خط چهارم حامل قرار می‌گیرند.",
                        fontSize = 11.sp,
                        color = Color(0xFF8C2812),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Symbols Horizontal Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Standard SymbolTypes matching current category
                val standardSymbols = SymbolType.values().filter { it.defaultCategory.name == activeCategoryId }
                items(standardSymbols) { symbolType ->
                    val isSelected = symbolType == activeSymbol
                    SymbolCard(
                        title = symbolType.defaultTitleFa,
                        symbolType = symbolType,
                        isSelected = isSelected,
                        onClick = { onSelectSymbol(symbolType) }
                    )
                }

                // 2. Custom Symbols matching current category (or when in CUSTOM)
                val filteredCustom = if (activeCategoryId == "CUSTOM") {
                    customSymbols
                } else {
                    customSymbols.filter { it.categoryId == activeCategoryId }
                }

                items(filteredCustom) { customSym ->
                    val isSelected = customSym.id == activeCustomSymbolId
                    CustomSymbolCard(
                        symbol = customSym,
                        isSelected = isSelected,
                        onClick = { onSelectCustomSymbol(customSym) }
                    )
                }

                // 3. Add Custom Symbol Button
                item {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onOpenCustomManager() }
                            .padding(4.dp)
                            .testTag("btn_add_custom_symbol"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "افزودن نماد سفارشی",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("جدید", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymbolCard(
    title: String,
    symbolType: SymbolType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFD6D0C5)
    val bgColor = if (isSelected) Color(0xFFFFFAF0) else Color(0xFFFFFFFF)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .testTag("symbol_card_${symbolType.name}"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawIntoCanvas { composeCanvas ->
                    val canvas = composeCanvas.nativeCanvas
                    canvas.save()
                    canvas.translate(size.width / 2f, size.height / 2f)
                    canvas.scale(0.85f, 0.85f)
                    ScoreRenderer.drawSingleSymbolPreview(canvas, symbolType, 14f)
                    canvas.restore()
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CustomSymbolCard(
    symbol: CustomSymbolEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFD6D0C5)
    val bgColor = if (isSelected) Color(0xFFFFFAF0) else Color(0xFFFFFFFF)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .testTag("custom_symbol_card_${symbol.id}"),
            contentAlignment = Alignment.Center
        ) {
            val imgPath = symbol.imagePath
            val fileExists = imgPath?.let { File(it).exists() } == true
            val bitmap: Bitmap? = remember(imgPath) {
                if (fileExists && imgPath != null) BitmapFactory.decodeFile(imgPath) else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = symbol.name,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Text(symbol.name.take(3), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = symbol.name,
            fontSize = 10.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
