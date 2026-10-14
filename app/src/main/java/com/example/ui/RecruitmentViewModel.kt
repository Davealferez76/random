package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BehavioralQuestion
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiModelOption
import com.example.data.model.MessageSender
import com.example.data.model.RecruitmentPack
import com.example.data.remote.GeminiClient
import com.example.data.repository.RecruitmentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.repository.FirestoreRecruitmentRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

data class RecruitmentUiState(
    val rawNotes: String = "",
    val roleLevel: String = "Senior (5+ yrs)",
    val industryOrTone: String = "High-Growth Tech / Modern",
    val selectedModel: GeminiModelOption = GeminiModelOption.FLASH_38,
    val enableThinking: Boolean = false,
    val isGenerating: Boolean = false,
    val generationProgressText: String = "",
    val currentPack: RecruitmentPack? = null,
    val activeTab: Int = 0, // 0: Raw Notes & Settings, 1: LinkedIn JD, 2: Interview Guide, 3: AI Recruiter Chat
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false,
    val chatInputText: String = "",
    val chatModel: GeminiModelOption = GeminiModelOption.FLASH_38,
    val chatEnableThinking: Boolean = false,
    val snackbarMessage: String? = null,
    val isApiKeyConfigured: Boolean = false,
    val currentUser: FirebaseUser? = null
)

class RecruitmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecruitmentRepository
    private val firestoreRepo: FirestoreRecruitmentRepository
    private val _uiState = MutableStateFlow(RecruitmentUiState())
    val uiState: StateFlow<RecruitmentUiState> = _uiState.asStateFlow()

    val savedPacks: StateFlow<List<RecruitmentPack>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RecruitmentRepository(db.recruitmentDao())
        firestoreRepo = FirestoreRecruitmentRepository(application)
        savedPacks = repository.savedPacks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


        // Seed with a rich default template so user can immediately see and generate
        val defaultNotes = """
            Need a Senior Lead Backend Engineer for our Payments & Core Ledgers team.
            Requirements:
            - 5+ years with Golang or Java/Kotlin, microservices architecture
            - High volume transactions, distributed idempotency, PostgreSQL, Redis caching
            - AWS or GCP cloud infra, Docker, Kubernetes
            - Must lead system design reviews and mentor 3 junior devs
            - Soft skills: calm under pressure during incidents, strong communicator across business teams
            - Remote or Hybrid in NYC. Equity + $185k-$220k base.
        """.trimIndent()

        _uiState.value = _uiState.value.copy(
            rawNotes = defaultNotes,
            isApiKeyConfigured = GeminiClient.isApiKeyConfigured(),
            chatMessages = listOf(
                ChatMessage(
                    sender = MessageSender.AI,
                    text = "👋 Hello! I am your AI Talent Acquisition Lead & Interview Coach. Once you generate a recruitment pack, I can simulate mock candidate responses to your 10 behavioral questions, help calibrate scoring rubrics, or polish the LinkedIn JD."
                )
            )
        )
    }

    fun updateRawNotes(notes: String) {
        _uiState.value = _uiState.value.copy(rawNotes = notes)
    }

    fun updateRoleLevel(level: String) {
        _uiState.value = _uiState.value.copy(roleLevel = level)
    }

    fun updateIndustryOrTone(tone: String) {
        _uiState.value = _uiState.value.copy(industryOrTone = tone)
    }

    fun updateSelectedModel(model: GeminiModelOption) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            enableThinking = if (model.supportsHighThinking) true else _uiState.value.enableThinking
        )
    }

    fun toggleThinking(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            enableThinking = enabled,
            selectedModel = if (enabled) GeminiModelOption.ARGON_THINKING else _uiState.value.selectedModel
        )
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(activeTab = tabIndex)
    }

    fun setTemplate(title: String, notes: String, level: String, tone: String) {
        _uiState.value = _uiState.value.copy(
            rawNotes = notes,
            roleLevel = level,
            industryOrTone = tone,
            snackbarMessage = "Loaded '$title' template"
        )
    }

    fun generatePack() {
        val currentState = _uiState.value
        if (currentState.rawNotes.isBlank()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Please enter raw role notes first")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                generationProgressText = "Analyzing raw notes & extracting competencies..."
            )

            delay(600)
            _uiState.value = _uiState.value.copy(
                generationProgressText = if (currentState.enableThinking)
                    "Deep reasoning (gemini-4-argon with High Thinking)..."
                else
                    "Drafting LinkedIn Job Description & structuring..."
            )

            val result = repository.generatePack(
                rawNotes = currentState.rawNotes,
                roleLevel = currentState.roleLevel,
                industryOrTone = currentState.industryOrTone,
                modelOption = currentState.selectedModel,
                enableThinking = currentState.enableThinking
            )

            result.fold(
                onSuccess = { pack ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        currentPack = pack,
                        activeTab = 1, // Navigate directly to LinkedIn JD
                        snackbarMessage = "Generated LinkedIn JD & 10-Question Guide successfully!"
                    )
                    viewModelScope.launch {
                        if (Firebase.auth.currentUser != null) {
                            firestoreRepo.savePack(pack)
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        snackbarMessage = "Generation error: ${error.localizedMessage ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    fun updateQuestionScore(questionId: Int, score: Int) {
        val pack = _uiState.value.currentPack ?: return
        val updatedQuestions = pack.questions.map { q ->
            if (q.id == questionId) q.copy(candidateScore = score) else q
        }
        val updatedPack = pack.copy(questions = updatedQuestions)
        _uiState.value = _uiState.value.copy(currentPack = updatedPack)
        viewModelScope.launch {
            repository.savePack(updatedPack)
            if (Firebase.auth.currentUser != null) {
                firestoreRepo.savePack(updatedPack)
            }
        }
    }

    fun updateQuestionNotes(questionId: Int, notes: String) {
        val pack = _uiState.value.currentPack ?: return
        val updatedQuestions = pack.questions.map { q ->
            if (q.id == questionId) q.copy(interviewerNotes = notes) else q
        }
        val updatedPack = pack.copy(questions = updatedQuestions)
        _uiState.value = _uiState.value.copy(currentPack = updatedPack)
        viewModelScope.launch {
            repository.savePack(updatedPack)
            if (Firebase.auth.currentUser != null) {
                firestoreRepo.savePack(updatedPack)
            }
        }
    }

    fun updateJobDescriptionText(newFormattedText: String) {
        val pack = _uiState.value.currentPack ?: return
        val updatedJd = pack.jobDescription.copy(rawFormattedText = newFormattedText)
        val updatedPack = pack.copy(jobDescription = updatedJd)
        _uiState.value = _uiState.value.copy(
            currentPack = updatedPack,
            snackbarMessage = "Job description updated"
        )
        viewModelScope.launch {
            repository.savePack(updatedPack)
            if (Firebase.auth.currentUser != null) {
                firestoreRepo.savePack(updatedPack)
            }
        }
    }

    fun updateChatInput(text: String) {
        _uiState.value = _uiState.value.copy(chatInputText = text)
    }

    fun updateChatModel(model: GeminiModelOption) {
        _uiState.value = _uiState.value.copy(
            chatModel = model,
            chatEnableThinking = if (model.supportsHighThinking) true else _uiState.value.chatEnableThinking
        )
    }

    fun toggleChatThinking(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            chatEnableThinking = enabled,
            chatModel = if (enabled) GeminiModelOption.ARGON_THINKING else _uiState.value.chatModel
        )
    }

    fun sendChatMessage(customPrompt: String? = null) {
        val textToSend = (customPrompt ?: _uiState.value.chatInputText).trim()
        if (textToSend.isBlank() || _uiState.value.isChatLoading) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = textToSend
        )

        val updatedHistory = _uiState.value.chatMessages + userMessage
        _uiState.value = _uiState.value.copy(
            chatMessages = updatedHistory,
            chatInputText = "",
            isChatLoading = true
        )

        viewModelScope.launch {
            val responseResult = repository.sendChatMessage(
                history = updatedHistory,
                newMessage = textToSend,
                currentPack = _uiState.value.currentPack,
                modelOption = _uiState.value.chatModel,
                enableThinking = _uiState.value.chatEnableThinking
            )

            val aiResponseText = responseResult.getOrElse { "Could not generate response: ${it.message}" }
            val aiMessage = ChatMessage(
                sender = MessageSender.AI,
                text = aiResponseText,
                modelUsed = if (_uiState.value.chatEnableThinking) "gemini-4-argon (High Thinking)" else _uiState.value.chatModel.displayName,
                isThinking = _uiState.value.chatEnableThinking
            )

            _uiState.value = _uiState.value.copy(
                chatMessages = _uiState.value.chatMessages + aiMessage,
                isChatLoading = false
            )
        }
    }

    fun testQuestionInMockInterview(question: BehavioralQuestion) {
        val prompt = "Simulate a candidate answering Question #${question.id} targeting competency '${question.competency}': \"${question.question}\". Then provide your recruiter assessment on whether it hits the green flags or shows red flags."
        setActiveTab(3) // Switch to AI Chat tab
        sendChatMessage(prompt)
    }

    fun loadSavedPack(pack: RecruitmentPack) {
        _uiState.value = _uiState.value.copy(
            currentPack = pack,
            rawNotes = pack.rawNotes,
            roleLevel = pack.experienceLevel,
            activeTab = 1,
            snackbarMessage = "Loaded '${pack.roleTitle}'"
        )
    }

    fun deleteSavedPack(packId: String) {
        viewModelScope.launch {
            repository.deletePack(packId)
            if (Firebase.auth.currentUser != null) {
                firestoreRepo.deletePack(packId)
            }
            if (_uiState.value.currentPack?.id == packId) {
                _uiState.value = _uiState.value.copy(currentPack = null)
            }
            _uiState.value = _uiState.value.copy(snackbarMessage = "Requisition deleted")
        }
    }

    fun refreshAuthUser() {
        val user = Firebase.auth.currentUser
        _uiState.value = _uiState.value.copy(
            currentUser = user,
            snackbarMessage = if (user != null) "Signed in as ${user.displayName ?: user.email}" else "Signed out"
        )
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}

