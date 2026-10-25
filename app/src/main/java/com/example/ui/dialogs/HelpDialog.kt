package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.HelpTopicEntity

@Composable
fun HelpDialog(
    topics: List<HelpTopicEntity>,
    onDismiss: () -> Unit,
    onAddTopic: (String, String) -> Unit,
    onUpdateTopic: (HelpTopicEntity) -> Unit,
    onDeleteTopic: (String) -> Unit,
    onReorderTopics: (List<HelpTopicEntity>) -> Unit,
    onResetToDefaults: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedTopicId by remember { mutableStateOf<String?>(topics.firstOrNull()?.id) }
    var isAddingTopic by remember { mutableStateOf(false) }
    var editingTopic by remember { mutableStateOf<HelpTopicEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<HelpTopicEntity?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Add Topic State
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }

    // Edit Topic State
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    val filteredTopics = if (searchQuery.isBlank()) {
        topics
    } else {
        topics.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(640.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "راهنمای جامع نت‌نگار دف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${topics.size} مبحث آموزشی و راهنما",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_help")) {
                        Icon(Icons.Default.Close, contentDescription = "بستن")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در مباحث و مفاهیم...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar (Add Topic + Reset Defaults)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            newTitle = ""
                            newContent = ""
                            isAddingTopic = true
                        },
                        modifier = Modifier.testTag("btn_add_help_topic")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("افزودن مبحث جدید", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.testTag("btn_reset_help_defaults")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بازنشانی پیش‌فرض‌ها", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Topics List
                if (filteredTopics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "مبحثی با عبارت '$searchQuery' یافت نشد." else "هیچ مبحث راهنمایی وجود ندارد.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredTopics, key = { _, item -> item.id }) { index, topic ->
                            val isExpanded = expandedTopicId == topic.id
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("help_card_${topic.id}")
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedTopicId = if (isExpanded) null else topic.id
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${topic.title}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Reorder Up
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = topics.toMutableList()
                                                    val item = mutable.removeAt(index)
                                                    mutable.add(index - 1, item)
                                                    onReorderTopics(mutable)
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp).testTag("btn_help_up_${topic.id}")
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "انتقال به بالا", modifier = Modifier.size(15.dp))
                                        }

                                        // Reorder Down
                                        IconButton(
                                            onClick = {
                                                if (index < topics.size - 1) {
                                                    val mutable = topics.toMutableList()
                                                    val item = mutable.removeAt(index)
                                                    mutable.add(index + 1, item)
                                                    onReorderTopics(mutable)
                                                }
                                            },
                                            enabled = index < topics.size - 1,
                                            modifier = Modifier.size(28.dp).testTag("btn_help_down_${topic.id}")
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "انتقال به پایین", modifier = Modifier.size(15.dp))
                                        }

                                        // Edit Button
                                        IconButton(
                                            onClick = {
                                                editingTopic = topic
                                                editTitle = topic.title
                                                editContent = topic.content
                                            },
                                            modifier = Modifier.size(28.dp).testTag("btn_help_edit_${topic.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        }

                                        // Delete Button
                                        IconButton(
                                            onClick = { topicToDelete = topic },
                                            modifier = Modifier.size(28.dp).testTag("btn_help_delete_${topic.id}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                        }

                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = topic.content,
                                                fontSize = 12.sp,
                                                lineHeight = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sub-Dialog: Add Topic
    if (isAddingTopic) {
        AlertDialog(
            onDismissRequest = { isAddingTopic = false },
            title = { Text("افزودن مبحث راهنما", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("عنوان مبحث") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_help_title")
                    )

                    OutlinedTextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        label = { Text("توضیحات و محتوای راهنما") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_help_content")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                            onAddTopic(newTitle, newContent)
                            isAddingTopic = false
                        }
                    },
                    enabled = newTitle.isNotBlank() && newContent.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_add_help")
                ) {
                    Text("ثبت مبحث")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingTopic = false }) { Text("انصراف") }
            }
        )
    }

    // Sub-Dialog: Edit Topic
    if (editingTopic != null) {
        val topic = editingTopic!!
        AlertDialog(
            onDismissRequest = { editingTopic = null },
            title = { Text("ویرایش مبحث راهنما", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان مبحث") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_help_title")
                    )

                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("توضیحات و محتوای راهنما") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_help_content")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editContent.isNotBlank()) {
                            onUpdateTopic(topic.copy(title = editTitle.trim(), content = editContent.trim()))
                            editingTopic = null
                        }
                    },
                    enabled = editTitle.isNotBlank() && editContent.isNotBlank(),
                    modifier = Modifier.testTag("btn_confirm_edit_help")
                ) {
                    Text("ذخیره تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTopic = null }) { Text("انصراف") }
            }
        )
    }

    // Sub-Dialog: Confirm Deletion
    if (topicToDelete != null) {
        val topic = topicToDelete!!
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            title = { Text("تأیید حذف مبحث", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از حذف مبحث '${topic.title}' اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTopic(topic.id)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_help")
                ) {
                    Text("بله، حذف شود")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) { Text("انصراف") }
            }
        )
    }

    // Sub-Dialog: Confirm Reset to Defaults
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("بازنشانی به مباحث پیش‌فرض", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مایلید تمام مباحث راهنما به تنظیمات و سرفصل‌های جامع پیش‌فرض نت‌نگار دف بازگردانده شوند؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetToDefaults()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("بازنشانی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("انصراف") }
            }
        )
    }
}
