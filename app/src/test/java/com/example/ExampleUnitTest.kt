package com.example

import com.example.data.model.GeminiModelOption
import com.example.data.model.SkillType
import com.example.data.remote.GeminiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun recruitmentPack_generatesExactly10Questions() {
        val notes = "Looking for a Senior Backend Engineer in Go and AWS"
        val pack = GeminiClient.createSmartFallbackPack(notes, "Senior (5+ yrs)", "Tech")

        assertEquals(10, pack.questions.size)
    }

    @Test
    fun recruitmentPack_containsBothHardAndSoftSkills() {
        val notes = "Staff Full-Stack Engineer"
        val pack = GeminiClient.createSmartFallbackPack(notes, "Staff", "High-Growth")

        val hasHardSkills = pack.questions.any { it.skillType == SkillType.HARD }
        val hasSoftSkills = pack.questions.any { it.skillType == SkillType.SOFT }

        assertTrue("Should contain hard skill behavioral questions", hasHardSkills)
        assertTrue("Should contain soft skill behavioral questions", hasSoftSkills)
    }

    @Test
    fun recruitmentPack_questionsHaveComprehensiveRubric() {
        val notes = "Product Manager"
        val pack = GeminiClient.createSmartFallbackPack(notes, "Mid-Level", "SaaS")

        for (q in pack.questions) {
            assertTrue("Question #${q.id} prompt must not be blank", q.question.isNotBlank())
            assertTrue("Question #${q.id} competency must not be blank", q.competency.isNotBlank())
            assertTrue("Question #${q.id} whatToListenFor must not be blank", q.whatToListenFor.isNotBlank())
            assertTrue("Question #${q.id} redFlags must not be blank", q.redFlags.isNotBlank())
            assertTrue("Question #${q.id} followUpProbe must not be blank", q.followUpProbe.isNotBlank())
        }
    }

    @Test
    fun jobDescription_containsLinkedInFormattingAndHashtags() {
        val notes = "Senior Android Engineer"
        val pack = GeminiClient.createSmartFallbackPack(notes, "Senior", "Mobile")
        val jd = pack.jobDescription

        assertTrue(jd.headline.isNotBlank())
        assertTrue(jd.responsibilities.isNotEmpty())
        assertTrue(jd.hardSkills.isNotEmpty())
        assertTrue(jd.softSkills.isNotEmpty())
        assertTrue(jd.hashtags.isNotEmpty())
        assertTrue(jd.rawFormattedText.contains("WHAT YOU'LL DO"))
        assertTrue(jd.rawFormattedText.contains("#"))
    }

    @Test
    fun geminiModels_argonSupportsHighThinking() {
        assertTrue(GeminiModelOption.ARGON_THINKING.supportsHighThinking)
        assertEquals("gemini-4-argon", GeminiModelOption.ARGON_THINKING.modelId)
        assertFalse(GeminiModelOption.FLASH_38.supportsHighThinking)
    }
}
