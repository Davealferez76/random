package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BehavioralQuestion
import com.example.data.model.JobDescription
import com.example.data.model.RecruitmentPack
import com.example.data.model.SkillType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class RecruitmentRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun savePack_authenticatedOwner_createsDocumentSuccessfully() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = FirestoreRecruitmentRepository(firestore)

    val pack = createSamplePack("Senior Backend Engineer")
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.savePack(pack)
    }

    assertTrue(result.isSuccess)

    val packs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePacks().first()
    }
    assertTrue(packs.any { it.id == pack.id })
  }

  @Test
  fun observePacks_unauthenticatedUser_fails() = runBlocking {
    auth.signOut()
    val repo = FirestoreRecruitmentRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observePacks().first()
      }
      fail("Expected IllegalStateException or PERMISSION_DENIED for unauthenticated caller")
    } catch (e: IllegalStateException) {
      // Expected: User must be signed in before accessing Firestore
      assertTrue(e.message?.contains("User must be signed in") == true)
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun deletePack_authenticatedOwner_deletesSuccessfully() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = FirestoreRecruitmentRepository(firestore)

    val pack = createSamplePack("Staff Android Engineer")
    val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.savePack(pack)
    }
    assertTrue(saveResult.isSuccess)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.deletePack(pack.id)
    }
    assertTrue(deleteResult.isSuccess)
  }

  private fun createSamplePack(roleTitle: String): RecruitmentPack {
    val id = "pack_" + UUID.randomUUID().toString().replace("-", "")
    return RecruitmentPack(
      id = id,
      roleTitle = roleTitle,
      department = "Engineering",
      experienceLevel = "Senior (5+ yrs)",
      rawNotes = "Notes for $roleTitle",
      jobDescription = JobDescription(
        title = roleTitle,
        department = "Engineering",
        experienceLevel = "Senior (5+ yrs)",
        locationType = "Remote",
        headline = "Hiring $roleTitle",
        aboutRole = "About $roleTitle",
        responsibilities = listOf("Write clean code", "Review PRs"),
        hardSkills = listOf("Kotlin", "Coroutines"),
        softSkills = listOf("Communication"),
        perksAndBenefits = listOf("Health insurance"),
        hashtags = listOf("#Hiring"),
        rawFormattedText = "Formatted JD for $roleTitle"
      ),
      questions = listOf(
        BehavioralQuestion(
          id = 1,
          competency = "System Design",
          skillType = SkillType.HARD,
          question = "Describe a system you designed...",
          whatToListenFor = "Metrics and trade-offs",
          redFlags = "Vagueness",
          followUpProbe = "How would you scale it?"
        )
      )
    )
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
