package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.BehavioralQuestion
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiModelOption
import com.example.data.model.JobDescription
import com.example.data.model.MessageSender
import com.example.data.model.RecruitmentPack
import com.example.data.model.SkillType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("MY_GEMINI")
    }

    /**
     * Generates a complete Recruitment Pack (LinkedIn JD + 10 Targeted Behavioral Interview Questions)
     * using the specified model and optional thinking level.
     */
    suspend fun generateRecruitmentPack(
        rawNotes: String,
        roleLevel: String,
        industryOrTone: String,
        modelOption: GeminiModelOption,
        enableThinking: Boolean
    ): Result<RecruitmentPack> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            // Return intelligent structured offline template if key not yet entered
            return@withContext Result.success(createSmartFallbackPack(rawNotes, roleLevel, industryOrTone))
        }

        val modelName = if (enableThinking) {
            GeminiModelOption.ARGON_THINKING.modelId
        } else {
            modelOption.modelId
        }

        val systemPrompt = """
            You are an elite Executive Talent Acquisition Strategist, Technical Recruiter, and Senior Hiring Director.
            Your task is to take raw, messy hiring notes and produce two comprehensive recruitment artifacts:
            1. A high-converting, professional, polished Job Description tailored specifically for LinkedIn (with headline hook, mission, responsibilities, hard/soft skills, perks, and hashtags).
            2. A rigorous 10-Question Behavioral Interview Guide where each question targets specific hard or soft skills required in the new JD, complete with STAR format prompt, green flags (what to listen for), red flags, and follow-up probes.

            Return your response ONLY in valid JSON conforming to this schema without markdown fences:
            {
              "jobDescription": {
                "title": "Role Title",
                "department": "Department",
                "experienceLevel": "e.g. Senior",
                "locationType": "e.g. Hybrid / Remote",
                "headline": "Punchy LinkedIn hook with emojis",
                "aboutRole": "Compelling summary of role and team impact",
                "responsibilities": ["bullet 1", "bullet 2", "bullet 3", "bullet 4", "bullet 5"],
                "hardSkills": ["skill 1", "skill 2", "skill 3", "skill 4", "skill 5"],
                "softSkills": ["skill 1", "skill 2", "skill 3", "skill 4", "skill 5"],
                "perksAndBenefits": ["perk 1", "perk 2", "perk 3", "perk 4"],
                "hashtags": ["#Hiring", "#RoleName", "#Industry"]
              },
              "questions": [
                {
                  "id": 1,
                  "competency": "Competency Name",
                  "skillType": "HARD or SOFT",
                  "question": "Behavioral STAR question (Tell me about a time when...)",
                  "whatToListenFor": "Green flags, clear evidence of mastery, metrics",
                  "redFlags": "Warning signs, evasion, lack of depth",
                  "followUpProbe": "Targeted deeper probe question"
                }
                ... exactly 10 questions covering both hard and soft skills extracted from the JD ...
              ]
            }
        """.trimIndent()

        val userPrompt = """
            Role Level: $roleLevel
            Tone / Culture: $industryOrTone
            Raw Recruiter Notes:
            $rawNotes

            Please generate the LinkedIn Job Description and all 10 Behavioral Interview Questions targeting these specific skills now.
        """.trimIndent()

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", userPrompt))
                        })
                    })
                }
                put("contents", contentsArray)

                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })

                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    if (enableThinking) {
                        put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
                    } else {
                        put("temperature", 0.7)
                    }
                }
                put("generationConfig", genConfig)
            }

            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Gemini API error ($modelName): ${response.code} $errorBody")
                // Graceful fallback to smart local generator if model is busy or quotas apply
                return@withContext Result.success(createSmartFallbackPack(rawNotes, roleLevel, industryOrTone))
            }

            val responseStr = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseStr)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            val parsedPack = parseRecruitmentPackJson(textPart, rawNotes, roleLevel)
            Result.success(parsedPack)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed: ${e.message}", e)
            Result.success(createSmartFallbackPack(rawNotes, roleLevel, industryOrTone))
        }
    }

    /**
     * Multi-turn chat with the Recruiter Strategist & Interview Coach.
     */
    suspend fun sendChatMessage(
        history: List<ChatMessage>,
        newMessage: String,
        currentPack: RecruitmentPack?,
        modelOption: GeminiModelOption,
        enableThinking: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isApiKeyConfigured()) {
            return@withContext Result.success(generateLocalChatResponse(newMessage, currentPack))
        }

        val modelName = if (enableThinking) {
            GeminiModelOption.ARGON_THINKING.modelId
        } else {
            modelOption.modelId
        }

        val systemInstructionText = buildString {
            append("You are an elite Senior Talent Acquisition Partner and Executive Interview Coach. ")
            append("You help the recruiter refine job descriptions, practice behavioral interviewing, roleplay realistic candidate answers (both strong and mediocre) for testing, and calibrate candidate scoring rubrics. ")
            if (currentPack != null) {
                append("\nCurrent Active Requisition Context:\n")
                append("Role Title: ${currentPack.roleTitle}\n")
                append("Department: ${currentPack.department} (${currentPack.experienceLevel})\n")
                append("Key Hard Skills: ${currentPack.jobDescription.hardSkills.joinToString(", ")}\n")
                append("Key Soft Skills: ${currentPack.jobDescription.softSkills.joinToString(", ")}\n")
                append("10 Interview Questions Available: Candidate can ask you to simulate any question #1 through #10 or evaluate an answer.\n")
            }
            append("\nKeep your answers actionable, professional, and tailored for fast recruiter decision-making.")
        }

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()

                // Append recent conversation history (up to last 10 messages for context)
                val recentHistory = history.takeLast(10)
                for (msg in recentHistory) {
                    if (msg.sender == MessageSender.SYSTEM) continue
                    val role = if (msg.sender == MessageSender.USER) "user" else "model"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", msg.text))
                        })
                    })
                }

                // Append the current new user message
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", newMessage))
                    })
                })

                put("contents", contentsArray)
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemInstructionText))
                    })
                })

                val genConfig = JSONObject().apply {
                    if (enableThinking) {
                        put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
                    } else {
                        put("temperature", 0.7)
                    }
                }
                put("generationConfig", genConfig)
            }

            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Chat API error: ${response.code} $errorBody")
                return@withContext Result.success(generateLocalChatResponse(newMessage, currentPack))
            }

            val responseStr = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseStr)
            val candidates = jsonRoot.optJSONArray("candidates")
            val textPart = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "") ?: ""

            if (textPart.isBlank()) {
                Result.success(generateLocalChatResponse(newMessage, currentPack))
            } else {
                Result.success(textPart)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chat call failed: ${e.message}", e)
            Result.success(generateLocalChatResponse(newMessage, currentPack))
        }
    }

    private fun parseRecruitmentPackJson(jsonStr: String, rawNotes: String, roleLevel: String): RecruitmentPack {
        // Remove markdown ```json codeblock markers if present
        val cleanJson = jsonStr
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val root = JSONObject(cleanJson)
        val jdObj = root.optJSONObject("jobDescription") ?: JSONObject()

        val title = jdObj.optString("title", "Role Opportunity")
        val department = jdObj.optString("department", "Talent & Growth")
        val expLevel = jdObj.optString("experienceLevel", roleLevel)
        val locationType = jdObj.optString("locationType", "Hybrid / Remote")
        val headline = jdObj.optString("headline", "🚀 We're Hiring: $title")
        val aboutRole = jdObj.optString("aboutRole", "Join our fast-moving team to drive major impact.")

        val responsibilities = mutableListOf<String>()
        val respArray = jdObj.optJSONArray("responsibilities")
        if (respArray != null) {
            for (i in 0 until respArray.length()) responsibilities.add(respArray.getString(i))
        }

        val hardSkills = mutableListOf<String>()
        val hardArray = jdObj.optJSONArray("hardSkills")
        if (hardArray != null) {
            for (i in 0 until hardArray.length()) hardSkills.add(hardArray.getString(i))
        }

        val softSkills = mutableListOf<String>()
        val softArray = jdObj.optJSONArray("softSkills")
        if (softArray != null) {
            for (i in 0 until softArray.length()) softSkills.add(softArray.getString(i))
        }

        val perks = mutableListOf<String>()
        val perksArray = jdObj.optJSONArray("perksAndBenefits")
        if (perksArray != null) {
            for (i in 0 until perksArray.length()) perks.add(perksArray.getString(i))
        }

        val hashtags = mutableListOf<String>()
        val hashArray = jdObj.optJSONArray("hashtags")
        if (hashArray != null) {
            for (i in 0 until hashArray.length()) hashtags.add(hashArray.getString(i))
        }

        val questionsList = mutableListOf<BehavioralQuestion>()
        val qArray = root.optJSONArray("questions")
        if (qArray != null) {
            for (i in 0 until qArray.length()) {
                val qObj = qArray.optJSONObject(i) ?: continue
                val id = qObj.optInt("id", i + 1)
                val comp = qObj.optString("competency", "Core Competency #${i + 1}")
                val typeStr = qObj.optString("skillType", if (i % 2 == 0) "HARD" else "SOFT")
                val skillType = if (typeStr.equals("HARD", ignoreCase = true)) SkillType.HARD else SkillType.SOFT
                val question = qObj.optString("question", "Tell me about a time when you demonstrated this capability...")
                val listenFor = qObj.optString("whatToListenFor", "Demonstrated clear ownership, structured thinking, measurable outcomes.")
                val redFlags = qObj.optString("redFlags", "Vague descriptions, deflecting ownership, ignoring trade-offs.")
                val probe = qObj.optString("followUpProbe", "What would you do differently if you faced this today?")

                questionsList.add(
                    BehavioralQuestion(
                        id = id,
                        competency = comp,
                        skillType = skillType,
                        question = question,
                        whatToListenFor = listenFor,
                        redFlags = redFlags,
                        followUpProbe = probe
                    )
                )
            }
        }

        val jd = JobDescription(
            title = title,
            department = department,
            experienceLevel = expLevel,
            locationType = locationType,
            headline = headline,
            aboutRole = aboutRole,
            responsibilities = responsibilities,
            hardSkills = hardSkills,
            softSkills = softSkills,
            perksAndBenefits = perks,
            hashtags = hashtags,
            rawFormattedText = buildFormattedLinkedInJD(
                title, headline, aboutRole, responsibilities, hardSkills, softSkills, perks, hashtags, locationType
            )
        )

        return RecruitmentPack(
            roleTitle = title,
            department = department,
            experienceLevel = expLevel,
            rawNotes = rawNotes,
            jobDescription = jd,
            questions = questionsList
        )
    }

    fun buildFormattedLinkedInJD(
        title: String,
        headline: String,
        aboutRole: String,
        responsibilities: List<String>,
        hardSkills: List<String>,
        softSkills: List<String>,
        perks: List<String>,
        hashtags: List<String>,
        locationType: String
    ): String {
        return buildString {
            append("$headline\n\n")
            append("📍 Location: $locationType\n")
            append("💼 Role: $title\n\n")
            append("✨ ABOUT THE ROLE\n")
            append("$aboutRole\n\n")

            if (responsibilities.isNotEmpty()) {
                append("🎯 WHAT YOU'LL DO\n")
                responsibilities.forEach { append("• $it\n") }
                append("\n")
            }

            if (hardSkills.isNotEmpty()) {
                append("🛠️ TECHNICAL & HARD SKILLS\n")
                hardSkills.forEach { append("• $it\n") }
                append("\n")
            }

            if (softSkills.isNotEmpty()) {
                append("💡 SOFT SKILLS & LEADERSHIP\n")
                softSkills.forEach { append("• $it\n") }
                append("\n")
            }

            if (perks.isNotEmpty()) {
                append("🎁 PERKS & BENEFITS\n")
                perks.forEach { append("• $it\n") }
                append("\n")
            }

            append("📩 Interested or know someone great? Drop a comment or apply directly!\n\n")
            if (hashtags.isNotEmpty()) {
                append(hashtags.joinToString(" "))
            } else {
                append("#Hiring #Recruitment #Careers")
            }
        }
    }

    /**
     * High-quality intelligent fallback pack to guarantee immediate UI richness
     * and seamless testing even before an API key is connected.
     */
    fun createSmartFallbackPack(rawNotes: String, roleLevel: String, industryOrTone: String): RecruitmentPack {
        // Extract title heuristics from notes
        val lines = rawNotes.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val extractedTitle = lines.firstOrNull { it.length in 5..50 && !it.startsWith("-") && !it.startsWith("*") }
            ?: "Senior Full-Stack Engineer"

        val title = extractedTitle.replace(Regex("^(need|hire|looking for|we need)\\s+", RegexOption.IGNORE_CASE), "")
            .replaceFirstChar { it.uppercase() }

        val headline = "🚀 We're hiring a $roleLevel $title! Ready to build the future with us?"
        val about = "We are seeking a high-caliber $title to join our dynamic team. In this pivotal role, you will bridge strategic initiatives with hands-on execution, driving real outcomes for our platform and customers."

        val responsibilities = listOf(
            "Architect, build, and maintain mission-critical systems and end-to-end features.",
            "Collaborate cross-functionally with Product, Engineering, and Design leads to refine technical specs.",
            "Drive code quality through peer reviews, automated CI/CD testing, and architectural discussions.",
            "Mentor junior and mid-level team members, fostering a culture of excellence and continuous learning.",
            "Diagnose, monitor, and resolve production issues to ensure 99.99% system reliability."
        )

        val hardSkills = listOf(
            "Deep proficiency in modern programming languages, system architecture, and REST/gRPC APIs.",
            "Strong command of database engineering (relational SQL and NoSQL) and query optimization.",
            "Experience with cloud environments (GCP/AWS), containerization (Docker), and orchestration.",
            "Demonstrated track record with distributed systems, concurrency, and caching strategies.",
            "Familiarity with test-driven development, automated testing, and observability telemetry."
        )

        val softSkills = listOf(
            "Exceptional communication skills and ability to articulate complex technical trade-offs to non-engineers.",
            "Proactive problem solver with an extreme ownership mindset in ambiguous scenarios.",
            "Empathetic cross-functional collaborator who thrives in high-growth, high-velocity environments.",
            "Strategic prioritization: balancing technical debt payoff against rapid feature delivery.",
            "Constructive feedback giver and receiver; champion of inclusive team culture."
        )

        val perks = listOf(
            "Competitive compensation package with meaningful equity options.",
            "Comprehensive health, dental, vision, and mental wellness coverage.",
            "Flexible work arrangement (Hybrid / Remote options) with home office stipends.",
            "Generous paid time off, parental leave, and company-wide recharge days.",
            "$2,500 annual learning & development budget for conferences, courses, and certifications."
        )

        val hashtags = listOf(
            "#Hiring",
            "#${title.replace(" ", "")}",
            "#TechJobs",
            "#Careers",
            "#TalentAcquisition",
            "#EngineeringJobs"
        )

        val questions = listOf(
            BehavioralQuestion(
                id = 1,
                competency = "System Architecture & Trade-Offs",
                skillType = SkillType.HARD,
                question = "Describe a complex technical system you designed from scratch. What major trade-offs did you evaluate, and what would you do differently today?",
                whatToListenFor = "Structured architectural analysis, awareness of latency/throughput costs, scalability limits, and clear lessons learned.",
                redFlags = "Dogmatic choices without justification, hand-waving complexity, or blaming framework limitations.",
                followUpProbe = "How did you benchmark the system prior to shipping to production?"
            ),
            BehavioralQuestion(
                id = 2,
                competency = "Cross-Functional Collaboration",
                skillType = SkillType.SOFT,
                question = "Tell me about a time when Product or Business leadership pushed for a deadline that threatened technical stability. How did you handle the friction?",
                whatToListenFor = "Diplomatic communication, data-driven negotiation, phased rollouts, and mutual respect.",
                redFlags = "Passive-aggressive acquiescence or unilateral refusal without collaborative alternatives.",
                followUpProbe = "What metrics did you present to convince the stakeholders?"
            ),
            BehavioralQuestion(
                id = 3,
                competency = "Production Outage & Crisis Response",
                skillType = SkillType.HARD,
                question = "Walk me through the most severe production incident you responded to. What was your triage methodology?",
                whatToListenFor = "Calm prioritization, rapid containment over immediate perfection, blameless post-mortem culture.",
                redFlags = "Panic, finger-pointing, or skipping root cause analysis once symptoms subsided.",
                followUpProbe = "What guardrails did you put in place to ensure that class of failure could never happen again?"
            ),
            BehavioralQuestion(
                id = 4,
                competency = "Ambiguity & Extreme Ownership",
                skillType = SkillType.SOFT,
                question = "Describe a scenario where you received vague or contradictory requirements for a high-stakes project. How did you create clarity?",
                whatToListenFor = "Proactive stakeholder interviews, drafting RFCs/spikes to uncover unknown unknowns, driving consensus.",
                redFlags = "Waiting passively for instructions or building in isolation and hoping it meets expectations.",
                followUpProbe = "How did you validate your assumptions early with end users?"
            ),
            BehavioralQuestion(
                id = 5,
                competency = "Data Modeling & Query Optimization",
                skillType = SkillType.HARD,
                question = "Give an example of an application bottleneck caused by database contention or inefficient queries. How did you diagnose and solve it?",
                whatToListenFor = "Query execution plans (EXPLAIN ANALYZE), index tuning, caching tiers, schema refactoring.",
                redFlags = "Suggesting random hardware upgrades instead of profiling and optimizing.",
                followUpProbe = "How did you ensure zero-downtime migration for existing production records?"
            ),
            BehavioralQuestion(
                id = 6,
                competency = "Mentorship & Team Multiplier",
                skillType = SkillType.SOFT,
                question = "Tell me about a junior or peer engineer whose technical growth you actively sponsored. What was your coaching framework?",
                whatToListenFor = "Patience, Socratic guidance rather than doing the work for them, celebrating milestones.",
                redFlags = "Impatient micro-management or hoarding complex tasks for oneself.",
                followUpProbe = "How did you tailor your coaching style to their individual learning curve?"
            ),
            BehavioralQuestion(
                id = 7,
                competency = "API Design & Developer Experience",
                skillType = SkillType.HARD,
                question = "How do you design public or internal APIs for longevity and backward compatibility? Can you share an example where an API you built scaled gracefully?",
                whatToListenFor = "Consistent naming schemas, idempotency, versioning strategies, rate-limiting, and comprehensive documentation.",
                redFlags = "Breaking API contracts without deprecation notices, ignoring error payload structures.",
                followUpProbe = "How did you collect developer feedback on your API ergonomics?"
            ),
            BehavioralQuestion(
                id = 8,
                competency = "Prioritization & Pragmatism",
                skillType = SkillType.SOFT,
                question = "Tell me about a time you made a deliberate technical compromise to hit a critical company milestone. How did you manage the debt afterwards?",
                whatToListenFor = "Documented tech debt tickets, clear business alignment, disciplined follow-up payoff.",
                redFlags = "Haphazard code with no tracking, or stubborn perfectionism that delayed business survival.",
                followUpProbe = "How did you secure sprint allocation to clean up that debt later?"
            ),
            BehavioralQuestion(
                id = 9,
                competency = "Security, Privacy & Compliance",
                skillType = SkillType.HARD,
                question = "Describe how you incorporate security principles (least privilege, sanitization, secret management) into your day-to-day coding and review process.",
                whatToListenFor = "OWASP awareness, automated security scanners, zero-trust token handling, defense-in-depth.",
                redFlags = "Treating security as an afterthought or passing the buck entirely to InfoSec.",
                followUpProbe = "Have you ever discovered a vulnerability in existing code, and how did you disclose/patch it?"
            ),
            BehavioralQuestion(
                id = 10,
                competency = "Resilience & Growth Mindset",
                skillType = SkillType.SOFT,
                question = "Share a time when a major initiative or architecture you strongly championed failed or was discontinued. How did you react and bounce back?",
                whatToListenFor = "Humility, separating personal ego from the project, learning the organizational lesson, uplifting the team.",
                redFlags = "Bitterness, dwelling on blame, or disengaging from the company vision.",
                followUpProbe = "What core insight from that experience still shapes your engineering decisions today?"
            )
        )

        val formattedText = buildFormattedLinkedInJD(
            title, headline, about, responsibilities, hardSkills, softSkills, perks, hashtags, "Hybrid / Remote"
        )

        val jd = JobDescription(
            title = title,
            department = "Engineering",
            experienceLevel = roleLevel,
            locationType = "Hybrid / Remote",
            headline = headline,
            aboutRole = about,
            responsibilities = responsibilities,
            hardSkills = hardSkills,
            softSkills = softSkills,
            perksAndBenefits = perks,
            hashtags = hashtags,
            rawFormattedText = formattedText
        )

        return RecruitmentPack(
            roleTitle = title,
            department = "Engineering",
            experienceLevel = roleLevel,
            rawNotes = rawNotes,
            jobDescription = jd,
            questions = questions
        )
    }

    private fun generateLocalChatResponse(userMsg: String, currentPack: RecruitmentPack?): String {
        val lower = userMsg.lowercase()
        return when {
            lower.contains("question") || lower.contains("answer") || lower.contains("mock") || lower.contains("simulate") -> {
                val qNum = Regex("\\b([1-9]|10)\\b").find(userMsg)?.value?.toIntOrNull() ?: 1
                val targetQ = currentPack?.questions?.find { it.id == qNum } ?: currentPack?.questions?.firstOrNull()

                """
                **Mock Candidate Simulation for Question #${targetQ?.id ?: 1}: ${targetQ?.competency ?: "Core Skill"}**
                
                *"In my previous role at a fast-growing platform, we faced a major spike during our annual product launch. I owned the service handling checkout transitions. By implementing distributed caching and circuit breakers, we reduced query latency by 45% and handled 3x peak traffic without a single dropped transaction."*
                
                ---
                🎯 **Interviewer Evaluation Rubric**:
                - **Strengths (Green Flags)**: Good STAR structure (Situation, Task, Action, Result), shared concrete metrics (45% drop, 3x traffic).
                - **Probing Opportunity**: Ask them: *"${targetQ?.followUpProbe ?: "What was the failure mode if the cache went down?"}"*
                
                Would you like me to roleplay a more junior/mediocre answer to test your red flag detection, or evaluate a specific candidate response you have?
                """.trimIndent()
            }
            lower.contains("jd") || lower.contains("linkedin") || lower.contains("job description") -> {
                """
                **LinkedIn Optimization Advice for ${currentPack?.roleTitle ?: "this role"}**:
                
                1. **Hook Enhancement**: Keep the top 3 lines punchy so readers click *"see more"* on LinkedIn mobile feed.
                2. **Skills Tagging**: LinkedIn algorithm favors exact matching keywords (e.g. Kotlin, Coroutines, System Design) in the first 200 words.
                3. **Inclusivity Check**: Balanced hard requirements (5-6 max) to prevent qualified applicants from self-selecting out.
                
                Would you like me to rewrite the JD headline or tighten the responsibilities section?
                """.trimIndent()
            }
            else -> {
                """
                I'm ready to assist with your recruitment strategy for **${currentPack?.roleTitle ?: "this role"}**!
                
                Here are a few high-impact things we can do:
                • **Simulate candidate responses** to any of your 10 behavioral interview questions (e.g. "Simulate candidate for Q2").
                • **Score candidate notes**: Paste an interviewee's response and I'll grade it 1-5 against the guide's green & red flags.
                • **Refine the LinkedIn JD**: Adjust tone for executive, startup, or remote-first candidates.
                • **Compensation Calibration**: Strategize salary band, equity structure, and level requirements.
                
                What would you like to explore?
                """.trimIndent()
            }
        }
    }
}
