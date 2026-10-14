package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BehavioralQuestion
import com.example.data.model.FirestoreRecruitmentPack
import com.example.data.model.JobDescription
import com.example.data.model.RecruitmentPack
import com.example.data.model.SkillType
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRecruitmentRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun observePacks(): Flow<List<RecruitmentPack>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/recruitment_packs"
        emitAll(
            db.collection("users").document(uid).collection("recruitment_packs")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreRecruitmentPack::class.java).map { it.toDomainModel() }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun savePack(pack: RecruitmentPack): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/recruitment_packs/${pack.id}"
        val docRef = db.collection("users").document(uid).collection("recruitment_packs").document(pack.id)

        return try {
            val questionsData = pack.questions.map { q ->
                mapOf(
                    "id" to q.id,
                    "competency" to q.competency,
                    "skillType" to q.skillType.name,
                    "question" to q.question,
                    "whatToListenFor" to q.whatToListenFor,
                    "redFlags" to q.redFlags,
                    "followUpProbe" to q.followUpProbe,
                    "candidateScore" to q.candidateScore,
                    "interviewerNotes" to q.interviewerNotes
                )
            }

            val payload = hashMapOf<String, Any>(
                "id" to pack.id,
                "userId" to uid,
                "roleTitle" to pack.roleTitle,
                "department" to pack.department,
                "experienceLevel" to pack.experienceLevel,
                "rawNotes" to pack.rawNotes,
                "jdTitle" to pack.jobDescription.title,
                "jdHeadline" to pack.jobDescription.headline,
                "jdAbout" to pack.jobDescription.aboutRole,
                "jdResponsibilities" to pack.jobDescription.responsibilities,
                "jdHardSkills" to pack.jobDescription.hardSkills,
                "jdSoftSkills" to pack.jobDescription.softSkills,
                "jdPerks" to pack.jobDescription.perksAndBenefits,
                "jdHashtags" to pack.jobDescription.hashtags,
                "jdRawFormattedText" to pack.jobDescription.rawFormattedText,
                "questions" to questionsData,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePack(packId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/recruitment_packs/$packId"
        val docRef = db.collection("users").document(uid).collection("recruitment_packs").document(packId)

        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun FirestoreRecruitmentPack.toDomainModel(): RecruitmentPack {
        val mappedQuestions = questions.map { map ->
            val id = (map["id"] as? Number)?.toInt() ?: 1
            val competency = map["competency"] as? String ?: ""
            val typeStr = map["skillType"] as? String ?: "HARD"
            val skillType = if (typeStr == "HARD") SkillType.HARD else SkillType.SOFT
            val question = map["question"] as? String ?: ""
            val whatToListenFor = map["whatToListenFor"] as? String ?: ""
            val redFlags = map["redFlags"] as? String ?: ""
            val followUpProbe = map["followUpProbe"] as? String ?: ""
            val candidateScore = (map["candidateScore"] as? Number)?.toInt() ?: 0
            val interviewerNotes = map["interviewerNotes"] as? String ?: ""

            BehavioralQuestion(
                id = id,
                competency = competency,
                skillType = skillType,
                question = question,
                whatToListenFor = whatToListenFor,
                redFlags = redFlags,
                followUpProbe = followUpProbe,
                candidateScore = candidateScore,
                interviewerNotes = interviewerNotes
            )
        }

        val jd = JobDescription(
            title = jdTitle,
            department = department,
            experienceLevel = experienceLevel,
            locationType = "Hybrid / Remote",
            headline = jdHeadline,
            aboutRole = jdAbout,
            responsibilities = jdResponsibilities,
            hardSkills = jdHardSkills,
            softSkills = jdSoftSkills,
            perksAndBenefits = jdPerks,
            hashtags = jdHashtags,
            rawFormattedText = jdRawFormattedText
        )

        return RecruitmentPack(
            id = id,
            roleTitle = roleTitle,
            department = department,
            experienceLevel = experienceLevel,
            rawNotes = rawNotes,
            jobDescription = jd,
            questions = mappedQuestions,
            createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
