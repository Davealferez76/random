package com.example.data.repository

import com.example.data.local.RecruitmentDao
import com.example.data.local.RecruitmentPackEntity
import com.example.data.model.BehavioralQuestion
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiModelOption
import com.example.data.model.JobDescription
import com.example.data.model.RecruitmentPack
import com.example.data.model.SkillType
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class RecruitmentRepository(private val dao: RecruitmentDao) {

    val savedPacks: Flow<List<RecruitmentPack>> = dao.getAllPacks().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun generatePack(
        rawNotes: String,
        roleLevel: String,
        industryOrTone: String,
        modelOption: GeminiModelOption,
        enableThinking: Boolean
    ): Result<RecruitmentPack> {
        val result = GeminiClient.generateRecruitmentPack(
            rawNotes = rawNotes,
            roleLevel = roleLevel,
            industryOrTone = industryOrTone,
            modelOption = modelOption,
            enableThinking = enableThinking
        )
        if (result.isSuccess) {
            val pack = result.getOrThrow()
            dao.insertPack(pack.toEntity())
        }
        return result
    }

    suspend fun savePack(pack: RecruitmentPack) {
        dao.insertPack(pack.toEntity())
    }

    suspend fun deletePack(packId: String) {
        dao.deletePackById(packId)
    }

    suspend fun sendChatMessage(
        history: List<ChatMessage>,
        newMessage: String,
        currentPack: RecruitmentPack?,
        modelOption: GeminiModelOption,
        enableThinking: Boolean
    ): Result<String> {
        return GeminiClient.sendChatMessage(
            history = history,
            newMessage = newMessage,
            currentPack = currentPack,
            modelOption = modelOption,
            enableThinking = enableThinking
        )
    }

    private fun RecruitmentPack.toEntity(): RecruitmentPackEntity {
        val qArray = JSONArray()
        for (q in questions) {
            val qObj = JSONObject().apply {
                put("id", q.id)
                put("competency", q.competency)
                put("skillType", q.skillType.name)
                put("question", q.question)
                put("whatToListenFor", q.whatToListenFor)
                put("redFlags", q.redFlags)
                put("followUpProbe", q.followUpProbe)
                put("candidateScore", q.candidateScore)
                put("interviewerNotes", q.interviewerNotes)
            }
            qArray.put(qObj)
        }

        return RecruitmentPackEntity(
            id = id,
            roleTitle = roleTitle,
            department = department,
            experienceLevel = experienceLevel,
            rawNotes = rawNotes,
            jdTitle = jobDescription.title,
            jdHeadline = jobDescription.headline,
            jdAbout = jobDescription.aboutRole,
            jdResponsibilitiesJson = JSONArray(jobDescription.responsibilities).toString(),
            jdHardSkillsJson = JSONArray(jobDescription.hardSkills).toString(),
            jdSoftSkillsJson = JSONArray(jobDescription.softSkills).toString(),
            jdPerksJson = JSONArray(jobDescription.perksAndBenefits).toString(),
            jdHashtagsJson = JSONArray(jobDescription.hashtags).toString(),
            jdRawFormattedText = jobDescription.rawFormattedText,
            questionsJson = qArray.toString(),
            createdAt = createdAt
        )
    }

    private fun RecruitmentPackEntity.toDomainModel(): RecruitmentPack {
        fun jsonArrayToList(json: String): List<String> {
            val list = mutableListOf<String>()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) list.add(arr.getString(i))
            } catch (_: Exception) {}
            return list
        }

        val questionsList = mutableListOf<BehavioralQuestion>()
        try {
            val arr = JSONArray(questionsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                questionsList.add(
                    BehavioralQuestion(
                        id = obj.optInt("id", i + 1),
                        competency = obj.optString("competency", ""),
                        skillType = if (obj.optString("skillType") == "HARD") SkillType.HARD else SkillType.SOFT,
                        question = obj.optString("question", ""),
                        whatToListenFor = obj.optString("whatToListenFor", ""),
                        redFlags = obj.optString("redFlags", ""),
                        followUpProbe = obj.optString("followUpProbe", ""),
                        candidateScore = obj.optInt("candidateScore", 0),
                        interviewerNotes = obj.optString("interviewerNotes", "")
                    )
                )
            }
        } catch (_: Exception) {}

        val jd = JobDescription(
            title = jdTitle,
            department = department,
            experienceLevel = experienceLevel,
            locationType = "Hybrid / Remote",
            headline = jdHeadline,
            aboutRole = jdAbout,
            responsibilities = jsonArrayToList(jdResponsibilitiesJson),
            hardSkills = jsonArrayToList(jdHardSkillsJson),
            softSkills = jsonArrayToList(jdSoftSkillsJson),
            perksAndBenefits = jsonArrayToList(jdPerksJson),
            hashtags = jsonArrayToList(jdHashtagsJson),
            rawFormattedText = jdRawFormattedText
        )

        return RecruitmentPack(
            id = id,
            roleTitle = roleTitle,
            department = department,
            experienceLevel = experienceLevel,
            rawNotes = rawNotes,
            jobDescription = jd,
            questions = questionsList,
            createdAt = createdAt
        )
    }
}
