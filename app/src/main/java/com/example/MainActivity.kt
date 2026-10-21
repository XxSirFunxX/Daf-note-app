package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dialogs.ImportDialog
import com.example.ui.screens.ProjectsListScreen
import com.example.ui.screens.ScoreEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ScoreViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Ensure RTL layout for Persian language
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ScoreApp()
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreApp(scoreViewModel: ScoreViewModel = viewModel()) {
    val state by scoreViewModel.uiState.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }

    if (state.currentProject != null) {
        ScoreEditorScreen(
            state = state,
            viewModel = scoreViewModel,
            onBackToProjects = {
                scoreViewModel.closeCurrentProject()
            }
        )
    } else {
        ProjectsListScreen(
            projects = state.projectsList,
            onOpenProject = { scoreViewModel.openProject(it) },
            onCreateProject = { scoreViewModel.createNewProject(it) },
            onDuplicateProject = { scoreViewModel.duplicateProject(it) },
            onDeleteProject = { scoreViewModel.deleteProject(it) },
            onOpenImport = { showImportDialog = true },
            onOpenDriveSync = {
                scoreViewModel.showNotification("سرویس ابری گوگل درایو در حالت محلی همگام‌سازی شده است")
            }
        )

        if (showImportDialog) {
            ImportDialog(
                onDismiss = { showImportDialog = false },
                onImportContent = { content ->
                    scoreViewModel.importProjectContent(content)
                }
            )
        }
    }
}
