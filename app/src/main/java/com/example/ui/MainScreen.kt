package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HistoryDialog
import com.example.ui.components.InterviewGuideView
import com.example.ui.components.JobDescriptionView
import com.example.ui.components.RecruiterChatView
import com.example.ui.components.RoleNotesInputSection
import com.example.ui.theme.GoldStar
import com.example.ui.theme.TalentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: RecruitmentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedPacks by viewModel.savedPacks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Back handling: if not on Tab 0, back returns to Tab 0
    BackHandler(enabled = uiState.activeTab != 0) {
        viewModel.setActiveTab(0)
    }

    if (showHistoryDialog) {
        HistoryDialog(
            packs = savedPacks,
            onSelectPack = { pack -> viewModel.loadSavedPack(pack) },
            onDeletePack = { packId -> viewModel.deleteSavedPack(packId) },
            onDismiss = { showHistoryDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recruitment Sandbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.enableThinking) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(GoldStar.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Thinking Level: HIGH",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = GoldStar
                            )
                        }
                    }

                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("saved_history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedPacks.isNotEmpty()) {
                                    Badge { Text("${savedPacks.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = "Saved Requisitions"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // Tab 0: Raw Notes & Draft
                NavigationBarItem(
                    selected = uiState.activeTab == 0,
                    onClick = { viewModel.setActiveTab(0) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Draft Notes") },
                    label = { Text("Draft") },
                    modifier = Modifier.testTag("tab_draft"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Tab 1: LinkedIn JD
                NavigationBarItem(
                    selected = uiState.activeTab == 1,
                    onClick = { viewModel.setActiveTab(1) },
                    icon = { Icon(Icons.Default.Article, contentDescription = "LinkedIn JD") },
                    label = { Text("LinkedIn JD") },
                    modifier = Modifier.testTag("tab_linkedin_jd"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Tab 2: Interview Guide
                NavigationBarItem(
                    selected = uiState.activeTab == 2,
                    onClick = { viewModel.setActiveTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.currentPack?.questions?.isNotEmpty() == true) {
                                    Badge { Text("10") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = "10 Questions")
                        }
                    },
                    label = { Text("10 Questions") },
                    modifier = Modifier.testTag("tab_interview_guide"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Tab 3: AI Recruiter Coach Chat
                NavigationBarItem(
                    selected = uiState.activeTab == 3,
                    onClick = { viewModel.setActiveTab(3) },
                    icon = {
                        Icon(Icons.Default.SmartToy, contentDescription = "AI Coach")
                    },
                    label = { Text("AI Coach") },
                    modifier = Modifier.testTag("tab_ai_chat"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                0 -> RoleNotesInputSection(
                    uiState = uiState,
                    onNotesChange = { viewModel.updateRawNotes(it) },
                    onRoleLevelChange = { viewModel.updateRoleLevel(it) },
                    onToneChange = { viewModel.updateIndustryOrTone(it) },
                    onModelChange = { viewModel.updateSelectedModel(it) },
                    onToggleThinking = { viewModel.toggleThinking(it) },
                    onSelectTemplate = { title, notes, level, tone ->
                        viewModel.setTemplate(title, notes, level, tone)
                    },
                    onGenerate = { viewModel.generatePack() }
                )
                1 -> JobDescriptionView(
                    jobDescription = uiState.currentPack?.jobDescription,
                    onUpdateJdText = { viewModel.updateJobDescriptionText(it) },
                    onNavigateToCreate = { viewModel.setActiveTab(0) }
                )
                2 -> InterviewGuideView(
                    questions = uiState.currentPack?.questions,
                    roleTitle = uiState.currentPack?.roleTitle ?: "Role Opportunity",
                    onScoreChange = { qId, score -> viewModel.updateQuestionScore(qId, score) },
                    onNotesChange = { qId, notes -> viewModel.updateQuestionNotes(qId, notes) },
                    onTestInMockInterview = { question -> viewModel.testQuestionInMockInterview(question) },
                    onNavigateToCreate = { viewModel.setActiveTab(0) }
                )
                3 -> RecruiterChatView(
                    messages = uiState.chatMessages,
                    isLoading = uiState.isChatLoading,
                    inputText = uiState.chatInputText,
                    currentModel = uiState.chatModel,
                    enableThinking = uiState.chatEnableThinking,
                    currentPack = uiState.currentPack,
                    onInputChange = { viewModel.updateChatInput(it) },
                    onModelChange = { viewModel.updateChatModel(it) },
                    onToggleThinking = { viewModel.toggleChatThinking(it) },
                    onSendMessage = { customPrompt -> viewModel.sendChatMessage(customPrompt) }
                )
            }
        }
    }
}
