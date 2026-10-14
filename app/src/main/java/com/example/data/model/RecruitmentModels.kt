package com.example.data.model

import java.util.UUID

enum class SkillType {
    HARD,
    SOFT
}

data class BehavioralQuestion(
    val id: Int,
    val competency: String,
    val skillType: SkillType,
    val question: String,
    val whatToListenFor: String,
    val redFlags: String,
    val followUpProbe: String,
    var candidateScore: Int = 0, // 0 to 5 stars
    var interviewerNotes: String = ""
)

data class JobDescription(
    val title: String,
    val department: String,
    val experienceLevel: String,
    val locationType: String, // e.g. "Hybrid - New York, NY", "Remote (US)", "On-site"
    val headline: String,
    val aboutRole: String,
    val responsibilities: List<String>,
    val hardSkills: List<String>,
    val softSkills: List<String>,
    val perksAndBenefits: List<String>,
    val hashtags: List<String>,
    val rawFormattedText: String = ""
)

data class RecruitmentPack(
    val id: String = UUID.randomUUID().toString(),
    val roleTitle: String,
    val department: String,
    val experienceLevel: String,
    val rawNotes: String,
    val jobDescription: JobDescription,
    val questions: List<BehavioralQuestion>,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val isThinking: Boolean = false
)

enum class MessageSender {
    USER,
    AI,
    SYSTEM
}

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val description: String,
    val supportsHighThinking: Boolean
) {
    FLASH_38("gemini-3.8-flash", "Gemini 3.8 Flash", "Standard general tasks (Balanced)", false),
    FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite", "Fast generation", false),
    ARGON_THINKING("gemini-4-argon", "Gemini 4 Argon (High Thinking)", "Deep reasoning with ThinkingLevel.HIGH", true)
}
