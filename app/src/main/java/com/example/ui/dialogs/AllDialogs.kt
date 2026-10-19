package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaperSize
import com.example.model.ScoreProject

// ==========================================
// 1. PAGE MANAGER DIALOG
// ==========================================

@Composable
fun PageManagerDialog(
    project: ScoreProject,
    onDismiss: () -> Unit,
    onSwitchPage: (Int) -> Unit,
    onAddNewPage: () -> Unit,
    onDeleteCurrentPage: () -> Unit,
    onReorderPages: (Int, Int) -> Unit
) {
    var confirmDeleteOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مدیریت صفحات (${project.pages.size} صفحه)", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(project.pages) { index, page ->
                        val isActive = index == project.activePageIndex
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSwitchPage(index) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "صفحه ${page.pageNumber}",
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${page.symbols.size} نماد",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                // Move up
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onReorderPages(index, index - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "انتقال به بالا", modifier = Modifier.size(16.dp))
                                    }
                                }

                                // Move down
                                if (index < project.pages.size - 1) {
                                    IconButton(
                                        onClick = { onReorderPages(index, index + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "انتقال به پایین", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onAddNewPage,
                        modifier = Modifier.testTag("btn_dialog_add_page")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صفحه جدید")
                    }

                    if (project.pages.size > 1) {
                        OutlinedButton(
                            onClick = { confirmDeleteOpen = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("btn_dialog_delete_page")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف صفحه")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )

    if (confirmDeleteOpen) {
        AlertDialog(
            onDismissRequest = { confirmDeleteOpen = false },
            title = { Text("تأیید حذف صفحه") },
            text = { Text("آیا از حذف صفحه فعلی اطمینان دارید؟ تمام نمادهای داخل این صفحه پاک خواهند شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteOpen = false
                        onDeleteCurrentPage()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteOpen = false }) { Text("انصراف") }
            }
        )
    }
}

// ==========================================
// 2. PAGE & STAFF SETTINGS DIALOG
// ==========================================

@Composable
fun PageSettingsDialog(
    project: ScoreProject,
    onDismiss: () -> Unit,
    onSaveSettings: (PaperSize, Int, Float, Float, String, String, String) -> Unit
) {
    val activePage = project.activePage
    var paperSize by remember { mutableStateOf(activePage.paperSize) }
    var stavesCount by remember { mutableIntStateOf(activePage.stavesCount) }
    var lineSpacing by remember { mutableFloatStateOf(activePage.lineSpacing) }
    var line4SpacingRatio by remember { mutableFloatStateOf(activePage.line4SpacingRatio) }
    var title by remember { mutableStateOf(activePage.title) }
    var subtitle by remember { mutableStateOf(activePage.subtitle) }
    var lessonNumber by remember { mutableStateOf(activePage.lessonNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیمات صفحه و حامل ۴خطی", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Paper Size
                Text("اندازه برگه چاپ:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = paperSize == PaperSize.A4, onClick = { paperSize = PaperSize.A4 })
                    Text("A4 (استاندارد)", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = paperSize == PaperSize.A3, onClick = { paperSize = PaperSize.A3 })
                    Text("A3 (بزرگ)", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Staves Count
                Text("تعداد حامل‌های صفحه: $stavesCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Slider(
                    value = stavesCount.toFloat(),
                    onValueChange = { stavesCount = it.toInt() },
                    valueRange = 1f..8f,
                    steps = 6
                )

                // Line Spacing
                Text("فاصله بین خطوط حامل: ${lineSpacing.toInt()} pt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Slider(
                    value = lineSpacing,
                    onValueChange = { lineSpacing = it },
                    valueRange = 12f..24f
                )

                // Line 4 Distance Multiplier
                Text(
                    text = "ضریب فاصله خط ۴ (حلقه‌ها): ${"%.2f".format(line4SpacingRatio)}x",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF8C2812)
                )
                Slider(
                    value = line4SpacingRatio,
                    onValueChange = { line4SpacingRatio = it },
                    valueRange = 0.9f..1.8f
                )
                Text(
                    text = "فاصله خط ۴ می‌تواند برای تفکیک صدای زیر (بَک) و صدای حلقه‌ها تنظیم شود.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Text Fields
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان قطعه / درس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("توضیحات و تمپو") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lessonNumber,
                    onValueChange = { lessonNumber = it },
                    label = { Text("شماره درس") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSaveSettings(paperSize, stavesCount, lineSpacing, line4SpacingRatio, title, subtitle, lessonNumber)
                onDismiss()
            }) {
                Text("ذخیره تنظیمات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

// ==========================================
// 3. EXPORT DIALOG
// ==========================================

@Composable
fun ExportDialog(
    project: ScoreProject,
    onDismiss: () -> Unit,
    onExportPdf: () -> Unit,
    onExportImage: (Boolean) -> Unit,
    onExportProjectFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("خروجی گرفتن از پروژه", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PDF
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportPdf(); onDismiss() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("خروجی PDF چاپی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("سایز دقیق A4/A3، تمام صفحات، کیفیت برداری", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // PNG
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportImage(true); onDismiss() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("خروجی تصویر PNG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("صفحه فعلی با کیفیت و وضوح بالا برای اشتراک‌گذاری", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // JPG
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportImage(false); onDismiss() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("خروجی تصویر JPG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("حجم کم‌تر مناسب پیام‌رسان‌ها", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                // Editable Project File
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onExportProjectFile(); onDismiss() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("فایل پروژه قابل ویرایش (.chaharkhat)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("امکان ارسال و بازگشایی مجدد در برنامه با حفظ تمام اشیا", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

// ==========================================
// 4. IMPORT DIALOG
// ==========================================

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportContent: (String) -> Unit
) {
    var inputContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("وارد کردن فایل پروژه یا MusicXML", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "متن JSON پروژه اختصاصی یا فایل MusicXML را اینجا قرار دهید یا از نمونه‌های آماده استفاده کنید:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputContent,
                    onValueChange = { inputContent = it },
                    label = { Text("محتوای فایل (JSON / MusicXML)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            inputContent = """
                                <?xml version="1.0" encoding="UTF-8"?>
                                <score-partwise version="3.1">
                                  <part id="P1">
                                    <measure number="1">
                                      <attributes>
                                        <time><beats>3</beats><beat-type>4</beat-type></time>
                                      </attributes>
                                      <note><pitch><step>C</step><octave>3</octave></pitch><type>quarter</type></note>
                                      <note><pitch><step>E</step><octave>4</octave></pitch><type>quarter</type></note>
                                      <note><pitch><step>G</step><octave>5</octave></pitch><type>quarter</type></note>
                                    </measure>
                                  </part>
                                </score-partwise>
                            """.trimIndent()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("نمونه MusicXML", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputContent.isNotBlank()) {
                        onImportContent(inputContent)
                        onDismiss()
                    }
                },
                enabled = inputContent.isNotBlank()
            ) {
                Text("پردازش و وارد کردن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

// ==========================================
// 5. GOOGLE DRIVE SYNC DIALOG
// ==========================================

@Composable
fun DriveSyncDialog(
    projectsCount: Int,
    onDismiss: () -> Unit,
    onBackupToDrive: () -> Unit,
    onRestoreFromDrive: () -> Unit
) {
    var simulatedDriveStatus by remember { mutableStateOf("حالت آفلاین (مستقل - بدون نیاز به حساب گوگل)") }
    var conflictStrategy by remember { mutableStateOf("نگهداری هر دو نسخه (پیشنهادی)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("همگام‌سازی و Google Drive", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF3FC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("وضعیت ذخیره‌سازی محلی:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("$projectsCount پروژه روی حافظه داخلی دستگاه شما محفوظ است.", fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("نیازمندی امنیتی: استفاده از تمامی امکانات برنامه کاملاً محلی و بدون اجبار به ورود حساب گوگل فعال است.", fontSize = 11.sp, color = Color(0xFF1565C0))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("مدیریت تعارض‌ها (Conflict Handling):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                listOf("نگهداری هر دو نسخه (پیشنهادی)", "اولویت با نسخه محلی دستگاه", "اولویت با نسخه ابری Drive").forEach { strategy ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { conflictStrategy = strategy }
                    ) {
                        RadioButton(selected = conflictStrategy == strategy, onClick = { conflictStrategy = strategy })
                        Text(strategy, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            simulatedDriveStatus = "پشتیبان‌گیری ابری با موفقیت در پوشه امن ذخیره شد."
                            onBackupToDrive()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پشتیبان‌گیری", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            simulatedDriveStatus = "بازیابی پروژه‌ها انجام شد."
                            onRestoreFromDrive()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازیابی", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(simulatedDriveStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

// ==========================================
// 6. CUSTOM SYMBOL DIALOG
// ==========================================

@Composable
fun CustomSymbolDialog(
    onDismiss: () -> Unit,
    onCreateCustom: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isRingSound by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن نماد اختصاصی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام نماد سفارشی") },
                    placeholder = { Text("مثلاً: اشاره چپ ترکیبی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRingSound) Color(0xFFFFEBE6) else Color(0xFFF2EFE8))
                        .clickable { isRingSound = !isRingSound }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = isRingSound,
                        onCheckedChange = { isRingSound = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("تعلق به گروه صدای حلقه‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("اگر فعال باشد، این نماد دقیقاً مطابق قانون خط چهارم محدود خواهد شد.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreateCustom(name, isRingSound)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("افزودن به کتابخانه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
