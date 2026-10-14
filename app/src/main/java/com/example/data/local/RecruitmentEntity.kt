package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recruitment_packs")
data class RecruitmentPackEntity(
    @PrimaryKey
    val id: String,
    val roleTitle: String,
    val department: String,
    val experienceLevel: String,
    val rawNotes: String,
    val jdTitle: String,
    val jdHeadline: String,
    val jdAbout: String,
    val jdResponsibilitiesJson: String,
    val jdHardSkillsJson: String,
    val jdSoftSkillsJson: String,
    val jdPerksJson: String,
    val jdHashtagsJson: String,
    val jdRawFormattedText: String,
    val questionsJson: String,
    val createdAt: Long
)
