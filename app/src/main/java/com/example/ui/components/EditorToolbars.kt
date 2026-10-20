package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScoreProject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    project: ScoreProject,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenPageSettings: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenDriveSync: () -> Unit,
    onOpenCustomSymbols: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " (ص ${project.activePageIndex + 1}/${project.pages.size})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_to_projects")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت به فهرست پروژه‌ها"
                )
            }
        },
        actions = {
            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("btn_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "لغو عملیات",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }

            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("btn_redo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "انجام مجدد",
                    tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                )
            }

            // Save
            IconButton(
                onClick = onSave,
                modifier = Modifier.testTag("btn_quick_save")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "ذخیره پروژه",
                    tint = if (isSaving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // More Options Dropdown
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("btn_editor_menu")
                ) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "منوی امکانات بیشتر")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("مدیریت صفحات (${project.pages.size})") },
                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenPageManager() },
                        modifier = Modifier.testTag("menu_page_manager")
                    )
                    DropdownMenuItem(
                        text = { Text("تنظیمات صفحه و حامل ۴خطی") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenPageSettings() },
                        modifier = Modifier.testTag("menu_page_settings")
                    )
                    DropdownMenuItem(
                        text = { Text("خروجی (PDF، تصویر، پروژه)") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenExport() },
                        modifier = Modifier.testTag("menu_export")
                    )
                    DropdownMenuItem(
                        text = { Text("وارد کردن فایل (پروژه، MusicXML)") },
                        leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenImport() },
                        modifier = Modifier.testTag("menu_import")
                    )
                    DropdownMenuItem(
                        text = { Text("پشتیبان‌گیری و Google Drive") },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenDriveSync() },
                        modifier = Modifier.testTag("menu_drive_sync")
                    )
                    DropdownMenuItem(
                        text = { Text("مدیریت نمادهای سفارشی") },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpenCustomSymbols() },
                        modifier = Modifier.testTag("menu_custom_symbols")
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

@Composable
fun FloatingQuickToolbar(
    isSnapMode: Boolean,
    isMultiSelectMode: Boolean,
    hasSelection: Boolean,
    showGuides: Boolean,
    highlightLine4: Boolean,
    onToggleSnap: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onResizeSelected: (Float) -> Unit,
    onFlipStem: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleGuides: () -> Unit,
    onToggleLine4: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Snap Mode Chip (distinct On / Off icons)
            FilterChip(
                selected = isSnapMode,
                onClick = onToggleSnap,
                label = { Text(if (isSnapMode) "چسبان به خطوط" else "جایگذاری آزاد", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSnapMode) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = if (isSnapMode) "چسبیدن به خطوط فعال است" else "چسبیدن به خطوط غیرفعال است",
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.testTag("btn_toggle_snap")
            )

            // 2. Multi-Select Chip (distinct Checklist / TouchApp icons)
            FilterChip(
                selected = isMultiSelectMode,
                onClick = onToggleMultiSelect,
                label = { Text(if (isMultiSelectMode) "چندانتخابی: فعال" else "تک‌انتخابی", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isMultiSelectMode) Icons.Default.Checklist else Icons.Default.TouchApp,
                        contentDescription = if (isMultiSelectMode) "حالت چندانتخابی فعال است" else "حالت تک‌انتخابی فعال است",
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.testTag("btn_toggle_multiselect")
            )

            // 3. Line 4 Highlight Chip (distinct Star / HorizontalRule icons)
            FilterChip(
                selected = highlightLine4,
                onClick = onToggleLine4,
                label = { Text(if (highlightLine4) "خط ۴ برجسته" else "خط ۴ عادی", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (highlightLine4) Icons.Default.Star else Icons.Default.HorizontalRule,
                        contentDescription = if (highlightLine4) "تمایز رنگی خط ۴ فعال است" else "تمایز خط ۴ غیرفعال است",
                        modifier = Modifier.size(15.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFEBE6),
                    selectedLabelColor = Color(0xFF8C2812)
                ),
                modifier = Modifier.testTag("btn_toggle_line4")
            )

            // 4. Note Guides Toggle (distinct Visibility / VisibilityOff icons)
            FilterChip(
                selected = showGuides,
                onClick = onToggleGuides,
                label = { Text(if (showGuides) "راهنما: روشن" else "راهنما: خاموش", fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = if (showGuides) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showGuides) "خطوط راهنمای نت‌گذاری فعال است" else "خطوط راهنما مخفی است",
                        modifier = Modifier.size(15.dp)
                    )
                },
                modifier = Modifier.testTag("btn_toggle_guides")
            )

            // Zoom Reset
            IconButton(
                onClick = onResetZoom,
                modifier = Modifier.size(36.dp).testTag("btn_reset_zoom")
            ) {
                Icon(Icons.Default.ZoomOutMap, contentDescription = "اندازه پیش‌فرض ۱۰۰٪", modifier = Modifier.size(18.dp))
            }

            // Selection-dependent actions
            if (hasSelection) {
                Box(
                    modifier = Modifier
                        .size(1.dp, 24.dp)
                        .background(Color(0xFFD6D0C5))
                )

                // Scale up
                IconButton(
                    onClick = { onResizeSelected(1.15f) },
                    modifier = Modifier.size(36.dp).testTag("btn_scale_up")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "بزرگ‌تر", modifier = Modifier.size(18.dp))
                }

                // Scale down
                IconButton(
                    onClick = { onResizeSelected(0.85f) },
                    modifier = Modifier.size(36.dp).testTag("btn_scale_down")
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "کوچک‌تر", modifier = Modifier.size(18.dp))
                }

                // Flip stem
                IconButton(
                    onClick = onFlipStem,
                    modifier = Modifier.size(36.dp).testTag("btn_flip_stem")
                ) {
                    Icon(Icons.Default.Flip, contentDescription = "تغییر جهت دم نت", modifier = Modifier.size(18.dp))
                }

                // Duplicate
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier.size(36.dp).testTag("btn_duplicate_selection")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "کپی نمادها", modifier = Modifier.size(18.dp))
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("btn_delete_selection")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف نمادها",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
