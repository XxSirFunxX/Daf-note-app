package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.io.BulkSymbolImporter
import com.example.io.BulkImportPreview
import com.example.io.BulkImportItem
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
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

                // Staves Count (1 to 16 per page)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تعداد حامل‌های صفحه: $stavesCount", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { if (stavesCount > 1) stavesCount-- },
                            enabled = stavesCount > 1,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { if (stavesCount < 16) stavesCount++ },
                            enabled = stavesCount < 16,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                Slider(
                    value = stavesCount.toFloat(),
                    onValueChange = { stavesCount = it.toInt() },
                    valueRange = 1f..16f,
                    steps = 14
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
    onExportProjectFile: () -> Unit,
    onSavePdfToUri: (Uri) -> Unit = {},
    onSaveImageToUri: (Uri, Boolean) -> Unit = { _, _ -> },
    onSaveProjectToUri: (Uri) -> Unit = {}
) {
    var selectedExportTab by remember { mutableIntStateOf(0) } // 0: Save to Device (SAF), 1: Share

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            onSavePdfToUri(uri)
            onDismiss()
        }
    }

    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveImageToUri(uri, true)
            onDismiss()
        }
    }

    val saveJpgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveImageToUri(uri, false)
            onDismiss()
        }
    }

    val saveProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            onSaveProjectToUri(uri)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("خروجی و ذخیره‌سازی پروژه", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedExportTab) {
                    Tab(
                        selected = selectedExportTab == 0,
                        onClick = { selectedExportTab = 0 },
                        text = { Text("ذخیره در دستگاه (SAF)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedExportTab == 1,
                        onClick = { selectedExportTab = 1 },
                        text = { Text("اشتراک‌گذاری مستقیم", fontSize = 12.sp) }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedExportTab == 0) {
                    Text(
                        text = "فایل را در پوشه دلخواه روی حافظه دستگاه با نام انتخابی ذخیره کنید:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Save PDF to Device
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cleanTitle = project.title.replace(" ", "_")
                                savePdfLauncher.launch("$cleanTitle.pdf")
                            }
                            .testTag("btn_save_pdf_device")
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ذخیره PDF در دستگاه", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("انتخاب پوشه و نام فایل چاپی با کیفیت برداری", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Save PNG Image to Device
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cleanTitle = project.title.replace(" ", "_")
                                savePngLauncher.launch("${cleanTitle}_page_${project.activePage.pageNumber}.png")
                            }
                            .testTag("btn_save_image_png_device")
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ذخیره تصویر PNG در دستگاه", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تصویر شفاف با وضوح بالا در پوشه دلخواه شما", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Save JPG Image to Device
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cleanTitle = project.title.replace(" ", "_")
                                saveJpgLauncher.launch("${cleanTitle}_page_${project.activePage.pageNumber}.jpg")
                            }
                            .testTag("btn_save_image_jpg_device")
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ذخیره تصویر JPG در دستگاه", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("حجم بهینه برای بایگانی و استفاده روزمره", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Save Project File to Device
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cleanTitle = project.title.replace(" ", "_")
                                saveProjectLauncher.launch("$cleanTitle.dafnt")
                            }
                            .testTag("btn_save_project_device")
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ذخیره فایل پروژه (.dafnt) در دستگاه", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("فایل کامل قابل ویرایش و تبادل میان دستگاه‌ها", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "ارسال مستقیم فایل به پیام‌رسان‌ها یا برنامه‌های دیگر:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Share PDF
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportPdf(); onDismiss() }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اشتراک‌گذاری PDF چاپی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("تمام صفحات، آماده چاپ در سایز A4/A3", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Share PNG
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportImage(true); onDismiss() }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF1565C0), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اشتراک‌گذاری تصویر PNG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("کیفیت بالا برای ارسال در شبکه‌های اجتماعی", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Share JPG
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportImage(false); onDismiss() }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اشتراک‌گذاری تصویر JPG", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("حجم فشرده‌تر برای ارسال سریع", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Share Project File
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExportProjectFile(); onDismiss() }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("اشتراک‌گذاری فایل پروژه (.dafnt)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("ارسال فایل ویرایش‌پذیر به همکاران یا هنرجویان", fontSize = 11.sp, color = Color.Gray)
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

// ==========================================
// 4. IMPORT DIALOG
// ==========================================

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportContent: (String) -> Unit
) {
    val context = LocalContext.current
    var inputContent by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    if (content.isNotBlank()) {
                        onImportContent(content)
                        onDismiss()
                    } else {
                        errorMessage = "فایل انتخاب‌شده خالی است"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "خطا در خواندن فایل: ${e.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("وارد کردن فایل پروژه یا MusicXML", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "می‌توانید فایل پروژه (.dafnt، .musicxml، .json) را از حافظه دستگاه انتخاب کنید یا محتوای آن را وارد نمایید:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        try {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            errorMessage = "امکان باز کردن انتخابگر فایل وجود ندارد"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_import_file")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتخاب فایل از حافظه دستگاه")
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputContent,
                    onValueChange = {
                        inputContent = it
                        errorMessage = null
                    },
                    label = { Text("یا چسباندن متن فایل (JSON / MusicXML)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
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
// 5. CUSTOM SYMBOLS COMPREHENSIVE MANAGER
// ==========================================

@Composable
fun CustomSymbolsManagerDialog(
    categories: List<CategoryEntity>,
    customSymbols: List<CustomSymbolEntity>,
    onDismiss: () -> Unit,
    onCreateFromUri: (Uri, String, String, Boolean, String?) -> Unit,
    onCreateCustom: (String, String, Boolean, String?) -> Unit,
    onUpdateCustom: (CustomSymbolEntity) -> Unit,
    onDeleteCustom: (String) -> Unit,
    onSelectForPlacement: (CustomSymbolEntity) -> Unit,
    onReplaceImage: (CustomSymbolEntity, Uri) -> Unit = { _, _ -> },
    onToggleHiddenCustomSymbol: (String, Boolean) -> Unit = { _, _ -> },
    onBulkImport: ((List<BulkImportItem>, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(if (customSymbols.isEmpty()) 0 else 1) }

    val commonDurations = listOf("پیش‌فرض", "1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64")

    // Bulk Import State
    var bulkImportPreview by remember { mutableStateOf<BulkImportPreview?>(null) }
    var isInspectingZip by remember { mutableStateOf(false) }
    var bulkImportError by remember { mutableStateOf<String?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isInspectingZip = true
            bulkImportError = null
            coroutineScope.launch {
                val res = BulkSymbolImporter.inspectZip(context, uri, customSymbols, categories)
                isInspectingZip = false
                if (res.isSuccess) {
                    bulkImportPreview = res.getOrNull()
                } else {
                    bulkImportError = res.exceptionOrNull()?.localizedMessage ?: "خطا در پردازش فایل ZIP"
                }
            }
        }
    }

    // Creation State
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull { it.id == "CUSTOM" }?.id ?: categories.firstOrNull()?.id ?: "CUSTOM") }
    var isRingSound by remember { mutableStateOf(false) }
    var durationValue by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageErrorMessage by remember { mutableStateOf<String?>(null) }

    // Editing State
    var editingSymbol by remember { mutableStateOf<CustomSymbolEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editIsRingSound by remember { mutableStateOf(false) }
    var editDurationValue by remember { mutableStateOf<String?>(null) }
    var editNewImageUri by remember { mutableStateOf<Uri?>(null) }
    var editPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var editImageErrorMessage by remember { mutableStateOf<String?>(null) }

    // Deletion Confirmation State
    var symbolToDelete by remember { mutableStateOf<CustomSymbolEntity?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val mime = context.contentResolver.getType(uri)
                    val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, mime)
                    if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Success) {
                        imageErrorMessage = null
                        previewBitmap = if (validation.isSvg) {
                            com.example.util.CustomSymbolImageHelper.renderSvgToBitmap(bytes.inputStream(), 120, 120)
                        } else {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } else {
                        imageErrorMessage = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Error).messageFa
                        previewBitmap = null
                    }
                }
            } catch (e: Exception) {
                imageErrorMessage = "عدم امکان خواندن فایل انتخاب شده: ${e.localizedMessage}"
                previewBitmap = null
            }
        }
    }

    val replaceImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        editNewImageUri = uri
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val mime = context.contentResolver.getType(uri)
                    val validation = com.example.util.CustomSymbolImageHelper.validateImage(bytes, mime)
                    if (validation is com.example.util.CustomSymbolImageHelper.ValidationResult.Success) {
                        editImageErrorMessage = null
                        editPreviewBitmap = if (validation.isSvg) {
                            com.example.util.CustomSymbolImageHelper.renderSvgToBitmap(bytes.inputStream(), 120, 120)
                        } else {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } else {
                        editImageErrorMessage = (validation as com.example.util.CustomSymbolImageHelper.ValidationResult.Error).messageFa
                        editPreviewBitmap = null
                    }
                }
            } catch (e: Exception) {
                editImageErrorMessage = "عدم امکان پردازش تصویر جدید: ${e.localizedMessage}"
                editPreviewBitmap = null
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
                        // Bulk ZIP Import Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream", "*/*"))
                                    } catch (e: Exception) {
                                        bulkImportError = "عدم امکان باز کردن انتخابگر فایل: ${e.localizedMessage}"
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("درون‌ریزی دسته‌ای از فایل ZIP", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("شامل manifest.json و تصاویر نمادها", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                }
                                if (isInspectingZip) {
                                    Text("در حال بررسی...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (bulkImportError != null) {
                            Text(
                                text = bulkImportError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Divider text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                            Text("یا افزودن تکی", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                            Spacer(modifier = Modifier.weight(1f).height(1.dp).background(Color.LightGray))
                        }

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
                                        Text("تصویر انتخاب شد (PNG یا SVG)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("برای تعویض تصویر کلیک کنید", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { selectedImageUri = null; previewBitmap = null; imageErrorMessage = null }) {
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

                        if (imageErrorMessage != null) {
                            Text(
                                text = imageErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
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

                        // Duration selector
                        Text("میزان کشش (Duration):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonDurations.forEach { dur ->
                                val isSelected = if (dur == "پیش‌فرض") durationValue == null else durationValue == dur
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { durationValue = if (dur == "پیش‌فرض") null else dur },
                                    label = { Text(dur, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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
                                            if (fileExists && imgPath != null) com.example.util.CustomSymbolImageHelper.loadBitmap(File(imgPath), 96) else null
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
                                                val catTitle = categories.firstOrNull { it.id == sym.categoryId || it.titleFa == sym.categoryId }?.titleFa ?: sym.categoryId
                                                Text(catTitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                if (sym.durationValue != null) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("• کشش: ${sym.durationValue}", fontSize = 10.sp, color = Color.Gray)
                                                }
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
                                                editDurationValue = sym.durationValue
                                                editNewImageUri = null
                                                editPreviewBitmap = null
                                                editImageErrorMessage = null
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

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Hide / Show Switch
                                        Switch(
                                            checked = !sym.isHidden,
                                            onCheckedChange = { isVisible ->
                                                onToggleHiddenCustomSymbol(sym.id, !isVisible)
                                            }
                                        )
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
                                onCreateFromUri(uri, name, selectedCategoryId, isRingSound, durationValue)
                            } else {
                                onCreateCustom(name, selectedCategoryId, isRingSound, durationValue)
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
        dismissButton = if (selectedTab == 0) {
            {
                TextButton(onClick = {
                    name = ""
                    selectedImageUri = null
                    previewBitmap = null
                    if (customSymbols.isNotEmpty()) selectedTab = 1 else onDismiss()
                }) { Text("انصراف") }
            }
        } else null
    )


    // Sub-dialog: Edit custom symbol info (includes name, category, ring rule, AND image replacement)
    if (editingSymbol != null) {
        val sym = editingSymbol!!
        AlertDialog(
            onDismissRequest = { editingSymbol = null },
            title = { Text("ویرایش اطلاعات و تصویر نماد سفارشی", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Image replacement section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { replaceImagePickerLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editPreviewBitmap != null) {
                                Image(
                                    bitmap = editPreviewBitmap!!.asImageBitmap(),
                                    contentDescription = "تصویر جدید",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تصویر جدید انتخاب شد", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("برای تغییر مجدد کلیک کنید", fontSize = 10.sp, color = Color.Gray)
                                }
                            } else {
                                val currentPath = sym.imagePath
                                val currentBmp = remember(currentPath) {
                                    currentPath?.let { File(it) }?.takeIf { it.exists() }?.let {
                                        com.example.util.CustomSymbolImageHelper.loadBitmap(it, 96)
                                    }
                                }
                                if (currentBmp != null) {
                                    Image(
                                        bitmap = currentBmp.asImageBitmap(),
                                        contentDescription = "تصویر فعلی",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("تعویض تصویر نماد (PNG/SVG)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("برای انتخاب تصویر جدید کلیک کنید", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    if (editImageErrorMessage != null) {
                        Text(editImageErrorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

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

                    Text("میزان کشش (Duration):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonDurations.forEach { dur ->
                            val isSel = if (dur == "پیش‌فرض") editDurationValue == null else editDurationValue == dur
                            FilterChip(
                                selected = isSel,
                                onClick = { editDurationValue = if (dur == "پیش‌فرض") null else dur },
                                label = { Text(dur, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
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
                            val newUri = editNewImageUri
                            val updatedSym = sym.copy(
                                name = editName.trim(),
                                categoryId = editCategoryId,
                                isRingSound = editIsRingSound,
                                durationValue = editDurationValue
                            )
                            if (newUri != null) {
                                onReplaceImage(updatedSym, newUri)
                            } else {
                                onUpdateCustom(updatedSym)
                            }
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

    // Sub-dialog: Bulk Symbol Import Preview
    if (bulkImportPreview != null) {
        BulkSymbolImportDialog(
            preview = bulkImportPreview!!,
            categories = categories,
            onDismiss = { bulkImportPreview = null },
            onConfirmImport = { items, renameDuplicates ->
                onBulkImport?.invoke(items, renameDuplicates)
                bulkImportPreview = null
            }
        )
    }
}

// ==========================================
// 6. BULK SYMBOL IMPORT PREVIEW DIALOG
// ==========================================

@Composable
fun BulkSymbolImportDialog(
    preview: BulkImportPreview,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirmImport: (itemsToImport: List<BulkImportItem>, renameDuplicates: Boolean) -> Unit
) {
    var renameDuplicates by remember { mutableStateOf(true) }
    var skipDuplicates by remember { mutableStateOf(false) }

    val effectiveValidItems = if (skipDuplicates) {
        preview.validItems.filter { !it.isDuplicate }
    } else {
        preview.validItems
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("پیش‌نمایش درون‌ریزی دسته‌ای نمادها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Summary cards row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کل یافته‌ها", fontSize = 10.sp, color = Color.Gray)
                            Text("${preview.totalFound}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("معتبر", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text("${preview.validItems.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                        }
                    }
                    if (preview.duplicateCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("تکراری", fontSize = 10.sp, color = Color(0xFFE65100))
                                Text("${preview.duplicateCount}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
                            }
                        }
                    }
                    if (preview.invalidItems.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("نامعتبر", fontSize = 10.sp, color = Color(0xFFC62828))
                                Text("${preview.invalidItems.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFC62828))
                            }
                        }
                    }
                }

                // Handling duplicates
                if (preview.duplicateCount > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("مدیریت نمادهای تکراری (${preview.duplicateCount} مورد):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFBF360C))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        renameDuplicates = true
                                        skipDuplicates = false
                                    }
                            ) {
                                RadioButton(selected = renameDuplicates && !skipDuplicates, onClick = { renameDuplicates = true; skipDuplicates = false })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تغییر نام موارد تکراری با پسوند «(کپی)»", fontSize = 11.sp)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        renameDuplicates = false
                                        skipDuplicates = true
                                    }
                            ) {
                                RadioButton(selected = skipDuplicates, onClick = { renameDuplicates = false; skipDuplicates = true })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نادیده گرفتن موارد تکراری (عدم افزودن)", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Valid items list
                Text("نمادهای آماده درون‌ریزی (${effectiveValidItems.size} نماد):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    effectiveValidItems.forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.previewBitmap != null) {
                                    Image(
                                        bitmap = item.previewBitmap.asImageBitmap(),
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (item.isDuplicate) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (renameDuplicates) "تکراری (کپی)" else "تکراری",
                                                fontSize = 9.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    val catTitle = categories.firstOrNull { it.id == item.categoryId }?.titleFa ?: item.categoryId
                                    Text(catTitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Invalid items warning
                if (preview.invalidItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("موارد رد شده (${preview.invalidItems.size} مورد):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        preview.invalidItems.forEach { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                                        Text(item.validationError ?: "خطای ناشناخته", fontSize = 10.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmImport(effectiveValidItems, renameDuplicates)
                },
                enabled = effectiveValidItems.isNotEmpty()
            ) {
                Text("درون‌ریزی (${effectiveValidItems.size} نماد)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
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
    customSymbols: List<CustomSymbolEntity> = emptyList(),
    noteDefinitions: List<NoteDefinitionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategorySafely: (String) -> Unit,
    onDeleteCategoryWithSymbols: (String) -> Unit = onDeleteCategorySafely,
    onDeleteCategoryAndMoveSymbols: (String, String) -> Unit = { src, _ -> onDeleteCategorySafely(src) }
) {
    var newTitle by remember { mutableStateOf("") }
    var addErrorMessage by remember { mutableStateOf<String?>(null) }

    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }

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
                    .height(360.dp)
            ) {
                // Add new category row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = {
                            newTitle = it
                            addErrorMessage = null
                        },
                        label = { Text("نام دسته‌بندی جدید") },
                        singleLine = true,
                        isError = addErrorMessage != null,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = newTitle.trim()
                            if (trimmed.isNotBlank()) {
                                if (categories.any { it.titleFa.trim().equals(trimmed, ignoreCase = true) }) {
                                    addErrorMessage = "دسته‌بندی با این عنوان از قبل وجود دارد."
                                } else {
                                    onAddCategory(trimmed)
                                    newTitle = ""
                                    addErrorMessage = null
                                }
                            }
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text("افزودن")
                    }
                }

                if (addErrorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = addErrorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val symCount = customSymbols.count { it.categoryId == cat.id || it.categoryId == cat.titleFa } +
                                noteDefinitions.count { it.categoryId == cat.id || it.categoryId == cat.titleFa }

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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cat.titleFa,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (symCount > 0) "$symCount نماد در این دسته" else "بدون نماد",
                                        fontSize = 10.sp,
                                        color = if (symCount > 0) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }

                                if (cat.isDefault) {
                                    Text("پیش‌فرض", fontSize = 10.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                IconButton(
                                    onClick = {
                                        categoryToEdit = cat
                                        editTitle = cat.titleFa
                                        editErrorMessage = null
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
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = {
                            editTitle = it
                            editErrorMessage = null
                        },
                        label = { Text("عنوان") },
                        singleLine = true,
                        isError = editErrorMessage != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editErrorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(editErrorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = editTitle.trim()
                        if (trimmed.isNotBlank()) {
                            if (categories.any { it.id != cat.id && it.titleFa.trim().equals(trimmed, ignoreCase = true) }) {
                                editErrorMessage = "دسته‌بندی دیگری با این عنوان وجود دارد."
                            } else {
                                onUpdateCategory(cat.copy(titleFa = trimmed))
                                categoryToEdit = null
                            }
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
        val symCount = customSymbols.count { it.categoryId == cat.id || it.categoryId == cat.titleFa } +
                noteDefinitions.count { it.categoryId == cat.id || it.categoryId == cat.titleFa }

        if (symCount == 0) {
            // Category is empty
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("حذف دسته‌بندی خالی") },
                text = { Text("آیا از حذف دسته‌بندی '${cat.titleFa}' اطمینان دارید؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteCategorySafely(cat.id)
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
        } else {
            // Category has symbols inside
            val otherCategories = categories.filter { it.id != cat.id }
            var deleteOption by remember { mutableIntStateOf(0) } // 0: Move symbols, 1: Delete symbols with category
            var selectedTargetCatId by remember {
                mutableStateOf(otherCategories.firstOrNull { it.id == "NOTES" }?.id ?: otherCategories.firstOrNull()?.id ?: "")
            }

            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف دسته‌بندی دارای نماد", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "دسته‌بندی '${cat.titleFa}' شامل $symCount نماد است. برای جلوگیری از حذف ناخواسته، نحوه مدیریت نمادها را انتخاب کنید:",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )

                        // Option 0: Move symbols to another category
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (deleteOption == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteOption = 0 }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = deleteOption == 0, onClick = { deleteOption = 0 })
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("انتقال نمادها به دسته‌بندی دیگر (توصیه‌شده)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                if (deleteOption == 0 && otherCategories.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("انتقال به دسته‌بندی:", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        otherCategories.forEach { targetCat ->
                                            FilterChip(
                                                selected = selectedTargetCatId == targetCat.id,
                                                onClick = { selectedTargetCatId = targetCat.id },
                                                label = { Text(targetCat.titleFa, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Option 1: Delete all symbols inside category
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (deleteOption == 1) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deleteOption = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = deleteOption == 1, onClick = { deleteOption = 1 })
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("حذف تمام نمادهای درون این دسته ($symCount نماد)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                                    Text("نمادها همراه با دسته‌بندی برای همیشه حذف می‌شوند.", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (deleteOption == 0) {
                        Button(
                            onClick = {
                                onDeleteCategoryAndMoveSymbols(cat.id, selectedTargetCatId)
                                categoryToDelete = null
                            },
                            enabled = selectedTargetCatId.isNotBlank()
                        ) {
                            Text("انتقال نمادها و حذف دسته")
                        }
                    } else {
                        Button(
                            onClick = {
                                onDeleteCategoryWithSymbols(cat.id)
                                categoryToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("حذف کامل دسته و نمادها")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("انصراف") }
                }
            )
        }
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
    onToggleHidden: (String, Boolean) -> Unit,
    onAddNoteDefinition: (String, SymbolType, String, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateNoteDefinition: (NoteDefinitionEntity) -> Unit = {},
    onDeleteNoteDefinition: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: List & Manage, 1: Add New Definition

    // New Definition State
    var newDefName by remember { mutableStateOf("") }
    var selectedSymbolType by remember { mutableStateOf(SymbolType.NOTE_QUARTER) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull { it.id == "NOTES" }?.id ?: "NOTES") }
    var durationValue by remember { mutableStateOf("1/4") }
    var isRingSound by remember { mutableStateOf(false) }

    // Edit Definition State
    var editingDef by remember { mutableStateOf<NoteDefinitionEntity?>(null) }
    var editName by remember { mutableStateOf("") }
    var editCategoryId by remember { mutableStateOf("") }
    var editDuration by remember { mutableStateOf("1/4") }
    var editIsRingSound by remember { mutableStateOf(false) }

    // Delete confirmation state
    var defToDelete by remember { mutableStateOf<NoteDefinitionEntity?>(null) }

    val commonDurations = listOf("1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مدیریت و تعریف نت‌ها و نشانه‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("فهرست و پنهان‌سازی (${noteDefinitions.size})", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("تعریف نت جدید", fontSize = 11.sp) }
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "با کلید سوئیچ می‌توانید نت‌ها یا نشانه‌ها را از پالت ابزار موقتاً مخفی کنید:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
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
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(noteDef.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (noteDef.isDefault) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("(اصلی)", fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                            val catTitle = categories.firstOrNull { it.id == noteDef.categoryId }?.titleFa ?: noteDef.categoryId
                                            Text("دسته: $catTitle | کشش: ${noteDef.durationValue}", fontSize = 11.sp, color = Color.Gray)
                                        }

                                        // Edit button
                                        IconButton(
                                            onClick = {
                                                editingDef = noteDef
                                                editName = noteDef.name
                                                editCategoryId = noteDef.categoryId
                                                editDuration = noteDef.durationValue
                                                editIsRingSound = noteDef.isRingSound
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }

                                        // Delete button (for all definitions including defaults)
                                        IconButton(
                                            onClick = { defToDelete = noteDef },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

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
                } else {
                    // TAB 1: ADD NEW NOTE DEFINITION
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newDefName,
                            onValueChange = { newDefName = it },
                            label = { Text("عنوان تعریف نت") },
                            placeholder = { Text("مثلاً: نت دولاچنگ سرعتی") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("دسته‌بندی نت:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSel = cat.id == selectedCategoryId
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(cat.titleFa, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("میزان کشش (Duration):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonDurations.forEach { dur ->
                                val isSel = dur == durationValue
                                FilterChip(
                                    selected = isSel,
                                    onClick = { durationValue = dur },
                                    label = { Text(dur, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        Text("نماد پایه در نت‌نگاری:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val candidateTypes = listOf(
                                SymbolType.NOTE_WHOLE,
                                SymbolType.NOTE_HALF,
                                SymbolType.NOTE_QUARTER,
                                SymbolType.NOTE_EIGHTH,
                                SymbolType.NOTE_SIXTEENTH,
                                SymbolType.NOTE_THIRTYSECOND,
                                SymbolType.REST_QUARTER,
                                SymbolType.RING_SINGLE,
                                SymbolType.RING_TREMOLO
                            )
                            candidateTypes.forEach { st ->
                                val isSel = st == selectedSymbolType
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedSymbolType = st
                                        if (st.isRingSound || st.defaultCategory.name == "RING_SOUNDS") {
                                            isRingSound = true
                                        }
                                    },
                                    label = { Text(st.defaultTitleFa, fontSize = 10.sp) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isRingSound) Color(0xFFFFEBE6) else Color(0xFFF2EFE8))
                                .clickable { isRingSound = !isRingSound }
                                .padding(8.dp)
                        ) {
                            Checkbox(checked = isRingSound, onCheckedChange = { isRingSound = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("تعلق به صدای حلقه‌ها (قانون خط چهارم)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("محدودیت استقرار منحصراً روی خط چهارم حامل چهارخطی", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                Button(
                    onClick = {
                        if (newDefName.isNotBlank()) {
                            onAddNoteDefinition(
                                newDefName.trim(),
                                selectedSymbolType,
                                selectedCategoryId,
                                durationValue,
                                isRingSound
                            )
                            newDefName = ""
                            selectedTab = 0
                        }
                    },
                    enabled = newDefName.isNotBlank()
                ) {
                    Text("افزودن تعریف نت")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("بستن") }
            }
        },
        dismissButton = if (selectedTab == 1) {
            {
                TextButton(onClick = {
                    newDefName = ""
                    selectedTab = 0
                }) { Text("انصراف") }
            }
        } else null
    )


    // Edit Sub-dialog
    if (editingDef != null) {
        val def = editingDef!!
        AlertDialog(
            onDismissRequest = { editingDef = null },
            title = { Text("ویرایش تعریف نت", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("عنوان") },
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

                    Text("کشش زمانی:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonDurations.forEach { dur ->
                            val isSel = dur == editDuration
                            FilterChip(
                                selected = isSel,
                                onClick = { editDuration = dur },
                                label = { Text(dur, fontSize = 11.sp) }
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قانون خط چهارم (صدای حلقه)", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            onUpdateNoteDefinition(
                                def.copy(
                                    name = editName.trim(),
                                    categoryId = editCategoryId,
                                    durationValue = editDuration,
                                    isRingSound = editIsRingSound
                                )
                            )
                            editingDef = null
                        }
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("ذخیره تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDef = null }) { Text("انصراف") }
            }
        )
    }

    // Delete confirmation
    if (defToDelete != null) {
        val def = defToDelete!!
        AlertDialog(
            onDismissRequest = { defToDelete = null },
            title = {
                Text(
                    text = if (def.isDefault) "حذف تعریف اصلی نت" else "حذف تعریف نت",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (def.isDefault) {
                        "آیا از حذف تعریف اصلی '${def.name}' اطمینان دارید؟\nاین نت از کتابخانه و پالت نمادها حذف خواهد شد، اما نمادهایی که پیش‌تر در صفحات قطعه قرار گرفته‌اند به طور کامل حفظ می‌شوند."
                    } else {
                        "آیا از حذف تعریف '${def.name}' اطمینان دارید؟"
                    },
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNoteDefinition(def.id)
                        defToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { defToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

// ==========================================
// 9. STAFF GUIDE SETTINGS DIALOG
// ==========================================

@Composable
fun StaffGuideSettingsDialog(
    currentLine4Text: String,
    currentLine1Text: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onResetToDefault: () -> Unit
) {
    var line4 by remember { mutableStateOf(currentLine4Text) }
    var line1 by remember { mutableStateOf(currentLine1Text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("متن راهنمای خطوط حامل", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "متن نمایش‌داده‌شده در کنار خطوط ۴ (بالا) و ۱ (پایین) حامل را سفارشی‌سازی کنید:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = line4,
                    onValueChange = { line4 = it },
                    label = { Text("متن راهنمای خط ۴ (پیش‌فرض: ۴ (حلقه‌ها))") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_guide_line4")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = line1,
                    onValueChange = { line1 = it },
                    label = { Text("متن راهنمای خط ۱ (پیش‌فرض: ۱ (تُم))") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_guide_line1")
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        line4 = "۴ (حلقه‌ها)"
                        line1 = "۱ (تُم)"
                        onResetToDefault()
                    },
                    modifier = Modifier.align(Alignment.End).testTag("btn_reset_guide_defaults")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بازیابی متن پیش‌فرض", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(line4.trim(), line1.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_save_guide_text")
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}

