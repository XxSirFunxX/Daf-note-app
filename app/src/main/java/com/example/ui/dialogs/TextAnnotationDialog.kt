package com.example.ui.dialogs

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScoreSymbol

@Composable
fun TextAnnotationDialog(
    initialSymbol: ScoreSymbol? = null,
    onDismiss: () -> Unit,
    onConfirm: (text: String, fontSizeSp: Float, isBold: Boolean, isItalic: Boolean, alignment: String, colorHex: String) -> Unit
) {
    // Parse initial values if editing an existing symbol
    var initialBold = false
    var initialItalic = false
    var initialAlign = "right"
    var initialFontSize = 14f

    initialSymbol?.customData?.let { data ->
        for (part in data.split(";")) {
            val kv = part.split("=")
            if (kv.size == 2) {
                when (kv[0].trim().lowercase()) {
                    "bold" -> initialBold = kv[1].trim().toBoolean()
                    "italic" -> initialItalic = kv[1].trim().toBoolean()
                    "align" -> initialAlign = kv[1].trim().lowercase()
                    "fontsize" -> initialFontSize = kv[1].trim().toFloatOrNull() ?: 14f
                }
            }
        }
    }

    var text by remember { mutableStateOf(initialSymbol?.textValue ?: "") }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var isBold by remember { mutableStateOf(initialBold) }
    var isItalic by remember { mutableStateOf(initialItalic) }
    var alignment by remember { mutableStateOf(initialAlign) } // "right", "center", "left"
    var selectedColorHex by remember { mutableStateOf(initialSymbol?.colorHex ?: "#1C1A18") }

    val presetColors = listOf(
        "#1C1A18" to "مشکی",
        "#8C2812" to "زرشکی دف",
        "#1565C0" to "آبی",
        "#2E7D32" to "سبز",
        "#6A1B9A" to "بنفش",
        "#E65100" to "نارنجی",
        "#555555" to "طوسی"
    )

    val quickPresets = listOf(
        "Andante ♩=80",
        "Moderato ♩=100",
        "Allegro ♩=120",
        "بند اول",
        "بند دوم",
        "تکرار",
        "اشاره ملایم",
        "فرود"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialSymbol != null) "ویرایش متن و نشانه" else "افزودن متن و نشانه به حامل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Text Input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("متن یادداشت / نشانه") },
                    placeholder = { Text("مثال: تمپو، بند اول، تکرار، شعر...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_annotation_text")
                )

                // Quick presets
                Text("عبارات و نشانه‌های پرکاربرد:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.forEach { preset ->
                        FilterChip(
                            selected = text == preset,
                            onClick = { text = preset },
                            label = { Text(preset, fontSize = 10.sp) }
                        )
                    }
                }

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("اندازه قلم: ${fontSize.toInt()} sp", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(11f, 14f, 18f, 24f).forEach { sz ->
                            FilterChip(
                                selected = fontSize.toInt() == sz.toInt(),
                                onClick = { fontSize = sz },
                                label = { Text("${sz.toInt()}", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..36f,
                    steps = 25,
                    modifier = Modifier.fillMaxWidth().testTag("slider_font_size")
                )

                // Style Controls (Bold, Italic, Alignment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bold & Italic
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconToggleButton(
                            checked = isBold,
                            onCheckedChange = { isBold = it },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .testTag("btn_toggle_bold")
                        ) {
                            Icon(
                                Icons.Default.FormatBold,
                                contentDescription = "ضخیم (Bold)",
                                tint = if (isBold) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconToggleButton(
                            checked = isItalic,
                            onCheckedChange = { isItalic = it },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .testTag("btn_toggle_italic")
                        ) {
                            Icon(
                                Icons.Default.FormatItalic,
                                contentDescription = "کج (Italic)",
                                tint = if (isItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Alignment
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "right" to Icons.Default.FormatAlignRight,
                            "center" to Icons.Default.FormatAlignCenter,
                            "left" to Icons.Default.FormatAlignLeft
                        ).forEach { (alignKey, icon) ->
                            val isSel = alignment == alignKey
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { alignment = alignKey }
                                    .testTag("btn_align_$alignKey"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = alignKey,
                                    tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Colors
                Text("رنگ متن:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { (hex, _) ->
                        val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            Color.Black
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSel) 3.dp else 1.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFFD6D0C5),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("color_chip_$hex")
                        )
                    }
                }

                // Live Preview
                Spacer(modifier = Modifier.height(4.dp))
                Text("پیش‌نمایش روی حامل:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        val previewColor = try {
                            Color(android.graphics.Color.parseColor(selectedColorHex))
                        } catch (e: Exception) {
                            Color(0xFF1C1A18)
                        }

                        val textAlign = when (alignment) {
                            "left" -> TextAlign.Left
                            "center" -> TextAlign.Center
                            else -> TextAlign.Right
                        }

                        Text(
                            text = if (text.isNotBlank()) text else "متن نمونه",
                            fontSize = fontSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            color = previewColor,
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth().testTag("text_preview")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim(), fontSize, isBold, isItalic, alignment, selectedColorHex)
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_text_annotation")
            ) {
                Text(if (initialSymbol != null) "ذخیره تغییرات" else "افزودن به قطعه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
