package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScoreProject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsListScreen(
    projects: List<ScoreProject>,
    onOpenProject: (ScoreProject) -> Unit,
    onCreateProject: (String) -> Unit,
    onDuplicateProject: (ScoreProject) -> Unit,
    onDeleteProject: (ScoreProject) -> Unit,
    onOpenImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var newProjectDialogOpen by remember { mutableStateOf(false) }
    var newProjectTitle by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ScoreProject?>(null) }
    var corruptProjectViewing by remember { mutableStateOf<ScoreProject?>(null) }

    val filteredProjects = if (searchQuery.isBlank()) {
        projects
    } else {
        projects.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نت نگار دف",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سیستم حامل اختصاصی و صدای حلقه‌ها",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenImport, modifier = Modifier.testTag("btn_import_file")) {
                        Icon(Icons.Default.FileUpload, contentDescription = "وارد کردن فایل")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    newProjectTitle = "قطعه درس ${projects.size + 1}"
                    newProjectDialogOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_project")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "ایجاد پروژه جدید")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پروژه جدید", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("جست‌وجوی پروژه یا درس...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("input_search_projects")
            )

            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "پروژه‌ای مطابق جست‌وجو یافت نشد" else "هنوز پروژه‌ای ثبت نشده است",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectItemCard(
                            project = project,
                            onClick = {
                                if (project.isCorrupted) {
                                    corruptProjectViewing = project
                                } else {
                                    onOpenProject(project)
                                }
                            },
                            onDuplicate = { if (!project.isCorrupted) onDuplicateProject(project) },
                            onDelete = { projectToDelete = project }
                        )
                    }
                }
            }
        }
    }

    // Corrupt Project Diagnostic & Recovery Dialog
    corruptProjectViewing?.let { proj ->
        AlertDialog(
            onDismissRequest = { corruptProjectViewing = null },
            title = { Text("خطا در ساختار فایل پروژه", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "ساختار JSON این پروژه آسیب‌دیده یا فرمت آن نامعتبر است: ${proj.parseError ?: "خطای ناشناخته"}",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "پیش‌نمایش محتوای خام فایل:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = proj.rawCorruptJson?.take(1000) ?: "فاقد محتوا",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete = proj
                        corruptProjectViewing = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف فایل آسیب‌دیده")
                }
            },
            dismissButton = {
                TextButton(onClick = { corruptProjectViewing = null }) { Text("بستن") }
            }
        )
    }

    // New Project Dialog
    if (newProjectDialogOpen) {
        AlertDialog(
            onDismissRequest = { newProjectDialogOpen = false },
            title = { Text("ساخت پروژه نت‌نویسی جدید", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProjectTitle,
                        onValueChange = { newProjectTitle = it },
                        label = { Text("نام پروژه / درس") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_project_title")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectTitle.isNotBlank()) {
                            onCreateProject(newProjectTitle)
                            newProjectDialogOpen = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_create_project")
                ) {
                    Text("ایجاد و بازگشایی")
                }
            },
            dismissButton = {
                TextButton(onClick = { newProjectDialogOpen = false }) { Text("انصراف") }
            }
        )
    }

    // Confirm Delete Dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("حذف پروژه") },
            text = { Text("آیا مطمئن هستید که می‌خواهید پروژه '${proj.title}' را به طور کامل حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_project")
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("انصراف") }
            }
        )
    }
}

@Composable
fun ProjectItemCard(
    project: ScoreProject,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()) }
    val formattedDate = remember(project.updatedAt) { dateFormat.format(Date(project.updatedAt)) }
    val totalSymbols = project.pages.sumOf { it.symbols.size }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("project_item_${project.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (project.isCorrupted) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (project.isCorrupted) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "پروژه آسیب‌دیده",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "۴خط",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (project.isCorrupted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (project.isCorrupted) {
                    Text(
                        text = "فایل آسیب‌دیده • ${project.parseError ?: "خطای ساختار"} (برای جزئیات لمس کنید)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "${project.pages.size} صفحه • $totalSymbols نماد • $formattedDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "کپی پروژه",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف پروژه",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
