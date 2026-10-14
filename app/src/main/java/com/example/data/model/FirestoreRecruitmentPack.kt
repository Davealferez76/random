package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreRecruitmentPack(
    val id: String = "",
    val userId: String = "",
    val roleTitle: String = "",
    val department: String = "",
    val experienceLevel: String = "",
    val rawNotes: String = "",
    val jdTitle: String = "",
    val jdHeadline: String = "",
    val jdAbout: String = "",
    val jdResponsibilities: List<String> = emptyList(),
    val jdHardSkills: List<String> = emptyList(),
    val jdSoftSkills: List<String> = emptyList(),
    val jdPerks: List<String> = emptyList(),
    val jdHashtags: List<String> = emptyList(),
    val jdRawFormattedText: String = "",
    val questions: List<Map<String, Any>> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
