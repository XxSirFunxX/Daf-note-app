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
import androidx.compose.foundation.layout.fillMaxSize
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.CustomSymbolEntity
import com.example.data.NoteDefinitionEntity
import com.example.model.PaperSize
import com.example.model.ScoreProject
import com.example.model.SymbolType
import java.io.File

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
// 6. CUSTOM SYMBOLS COMPREHENSIVE MANAGER
// ==========================================

@Composable
fun CustomSymbolsManagerDialog(
    categories: List<CategoryEntity>,
    customSymbols: List<CustomSymbolEntity>,
    onDismiss: () -> Unit,
    onCreateFromUri: (Uri, String, String, Boolean) -> Unit,
    onCreateCustom: (String, String, Boolean) -> Unit,
    onUpdateCustom: (CustomSymbolEntity) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onSelectForPlacement: (CustomSymbolEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (customSymbols.isEmpty()) 0 else 1) }

    // Creation State
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull { it.id == "CUSTOM" }?.id ?: categories.firstOrNull()?.id ?: "CUSTOM") }
    var isRingSound by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Editing State
    var editingSymbol by remember { mutableStateOf<CustomSymbolEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editIsRingSound by remember { mutableStateOf(false) }

    // Deletion Confirmation State
    var symbolToDelete by remember { mutableStateOf<CustomSymbolEntity?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    previewBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("کتابخانه نمادهای سفارشی", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("افزودن نماد جدید", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("نمادهای ذخیره‌شده (${customSymbols.size})", fontSize = 12.sp) }
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                if (selectedTab == 0) {
                    // --- TAB 0: CREATE NEW SYMBOL ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Image Picker Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { imagePickerLauncher.launch("image/*") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (previewBitmap != null) {
                                    Image(
                                        bitmap = previewBitmap!!.asImageBitmap(),
                                        contentDescription = "تصویر نماد",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تصویر انتخاب شد (PNG/SVG)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("برای تغییر تصویر کلیک کنید", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { selectedImageUri = null; previewBitmap = null }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف تصویر", tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("وارد کردن تصویر PNG یا SVG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("از گالری یا فایل‌های دستگاه انتخاب کنید", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Name input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("نام نماد سفارشی") },
                            placeholder = { Text("مثلاً: اشاره چپ ترکیبی") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_custom_symbol_name")
                        )

                        // Category selector
                        Text("دسته‌بندی نماد:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val availableCats = if (categories.isNotEmpty()) categories else listOf(
                                CategoryEntity("CUSTOM", "سفارشی"),
                                CategoryEntity("RING_SOUNDS", "صدای حلقه‌ها"),
                                CategoryEntity("NOTES", "نت‌ها"),
                                CategoryEntity("TECHNIQUES", "تکنیک و مضراب")
                            )
                            availableCats.forEach { cat ->
                                val isSelected = cat.id == selectedCategoryId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(cat.titleFa, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Ring Sound Rule Checkbox
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
                                Text("تعلق به گروه صدای حلقه‌ها (خط ۴)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isRingSound) Color(0xFF8C2812) else Color.Unspecified)
                                Text("در صورت فعال بودن، نماد منحصراً روی خط چهارم حامل قرار می‌گیرد.", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    // --- TAB 1: LIST OF STORED CUSTOM SYMBOLS ---
                    if (customSymbols.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("هنوز هیچ نماد سفارشی ذخیره نشده است.", color = Color.Gray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = { selectedTab = 0 }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("افزودن اولین نماد")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(customSymbols) { sym ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Image or initial
                                        val imgPath = sym.imagePath
                                        val fileExists = imgPath?.let { File(it).exists() } == true
                                        val bmp = remember(imgPath) {
                                            if (fileExists && imgPath != null) BitmapFactory.decodeFile(imgPath) else null
                                        }

                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = sym.name,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, Color(0xFFD6D0C5), RoundedCornerShape(6.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(sym.name.take(2), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sym.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val catTitle = categories.firstOrNull { it.id == sym.categoryId }?.titleFa ?: sym.categoryId
                                                Text(catTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                if (sym.isRingSound) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("• صدای حلقه (خط ۴)", fontSize = 10.sp, color = Color(0xFF8C2812), fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }

                                        // Select button
                                        OutlinedButton(
                                            onClick = {
                                                onSelectForPlacement(sym)
                                                onDismiss()
                                            },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("درج", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                editingSymbol = sym
                                                editName = sym.name
                                                editCategoryId = sym.categoryId
                                                editIsRingSound = sym.isRingSound
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }

                                        // Delete button
                                        IconButton(
                                            onClick = { symbolToDelete = sym },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val uri = selectedImageUri
                            if (uri != null) {
                                onCreateFromUri(uri, name, selectedCategoryId, isRingSound)
                            } else {
                                onCreateCustom(name, selectedCategoryId, isRingSound)
                            }
                            onDismiss()
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.testTag("btn_save_custom_symbol")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ذخیره در کتابخانه")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("بستن") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )

    // Sub-dialog: Edit custom symbol info
    if (editingSymbol != null) {
        val sym = editingSymbol!!
        AlertDialog(
            onDismissRequest = { editingSymbol = null },
            title = { Text("ویرایش اطلاعات نماد سفارشی", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("نام نماد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("دسته‌بندی:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSel = cat.id == editCategoryId
                            FilterChip(
                                selected = isSel,
                                onClick = { editCategoryId = cat.id },
                                label = { Text(cat.titleFa, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (editIsRingSound) Color(0xFFFFEBE6) else Color(0xFFF2EFE8))
                            .clickable { editIsRingSound = !editIsRingSound }
                            .padding(8.dp)
                    ) {
                        Checkbox(checked = editIsRingSound, onCheckedChange = { editIsRingSound = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تعلق به صدای حلقه‌ها (خط ۴)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateCustom(sym.copy(name = editName, categoryId = editCategoryId, isRingSound = editIsRingSound))
                            editingSymbol = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("ذخیره تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSymbol = null }) { Text("انصراف") }
            }
        )
    }

    // Sub-dialog: Confirm deletion
    if (symbolToDelete != null) {
        val sym = symbolToDelete!!
        AlertDialog(
            onDismissRequest = { symbolToDelete = null },
            title = { Text("تأیید حذف نماد") },
            text = { Text("آیا از حذف نماد '${sym.name}' از کتابخانه اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustom(sym.id)
                        symbolToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { symbolToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

// Simple fallback version
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

// ==========================================
// 7. CATEGORY MANAGER DIALOG
// ==========================================

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var newTitle by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مدیریت دسته‌بندی‌های نمادها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Add new category row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("نام دسته‌بندی جدید") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                onAddCategory(newTitle.trim())
                                newTitle = ""
                            }
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text("افزودن")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.titleFa,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (cat.isDefault) {
                                    Text("پیش‌فرض", fontSize = 11.sp, color = Color.Gray)
                                } else {
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = cat
                                            editTitle = cat.titleFa
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )

    // Edit Category modal
    if (categoryToEdit != null) {
        val cat = categoryToEdit!!
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("ویرایش عنوان دسته‌بندی") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("عنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            onUpdateCategory(cat.copy(titleFa = editTitle.trim()))
                            categoryToEdit = null
                        }
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) { Text("انصراف") }
            }
        )
    }

    // Delete Category modal
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف دسته‌بندی") },
            text = { Text("آیا از حذف دسته‌بندی '${cat.titleFa}' اطمینان دارید؟ تمام نمادهای داخل آن به دسته‌بندی عمومی منتقل خواهند شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

// ==========================================
// 8. NOTE DEFINITIONS MANAGER DIALOG
// ==========================================

@Composable
fun NoteDefinitionsManagerDialog(
    noteDefinitions: List<NoteDefinitionEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onToggleHidden: (String, Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("مدیریت و پنهان‌سازی نت‌ها و نمادها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Text(
                    text = "می‌توانید نمادهای کم‌کاربرد را از پالت ابزار موقتاً مخفی کنید:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(noteDefinitions) { noteDef ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(noteDef.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    val catTitle = categories.firstOrNull { it.id == noteDef.categoryId }?.titleFa ?: noteDef.categoryId
                                    Text("دسته: $catTitle | کشش: ${noteDef.durationValue}", fontSize = 11.sp, color = Color.Gray)
                                }

                                Switch(
                                    checked = !noteDef.isHidden,
                                    onCheckedChange = { isVisible ->
                                        onToggleHidden(noteDef.id, !isVisible)
                                    }
                                )
                            }
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
