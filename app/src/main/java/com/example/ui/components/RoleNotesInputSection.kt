package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeminiModelOption
import com.example.ui.RecruitmentUiState
import com.example.ui.theme.GoldStar
import com.example.ui.theme.TalentBlue
import com.example.ui.theme.TalentBlueLight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleNotesInputSection(
    uiState: RecruitmentUiState,
    onNotesChange: (String) -> Unit,
    onRoleLevelChange: (String) -> Unit,
    onToneChange: (String) -> Unit,
    onModelChange: (GeminiModelOption) -> Unit,
    onToggleThinking: (Boolean) -> Unit,
    onSelectTemplate: (String, String, String, String) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "Recruitment Sandbox",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Recruitment Sandbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transform raw role notes into a tailored LinkedIn Job Description and 10 behavioral interview questions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Quick Role Templates
        Text(
            text = "Quick Inspiration Templates",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val templates = listOf(
                RoleTemplate(
                    label = "Payments Backend Lead",
                    title = "Senior Lead Backend Engineer",
                    level = "Senior (5+ yrs)",
                    tone = "High-Growth Fintech",
                    notes = "Lead Backend Engineer for core payments ledger. Go / Java, high-throughput transactions, idempotent APIs, Redis, PostgreSQL, AWS/K8s. Strong system design, incident response, mentoring 3 devs. Remote/NYC."
                ),
                RoleTemplate(
                    label = "Staff Mobile Engineer",
                    title = "Staff Android Engineer",
                    level = "Staff / Principal",
                    tone = "Modern Product Tech",
                    notes = "Staff Android Engineer for flagship app with 5M+ DAU. Kotlin, Jetpack Compose, modular architecture, offline-first sync, performance profiling, CI/CD pipelines. Advocate for accessibility & design system parity."
                ),
                RoleTemplate(
                    label = "AI Product Manager",
                    title = "Senior AI Product Manager",
                    level = "Senior (5+ yrs)",
                    tone = "Fast-Paced Innovation",
                    notes = "Senior PM to drive LLM & multimodal AI features. Experience launching enterprise AI tools, user retention metrics, prompt engineering evaluation, cross-functional collaboration with ML researchers and sales leadership."
                ),
                RoleTemplate(
                    label = "Enterprise Account Exec",
                    title = "Enterprise Account Executive",
                    level = "Mid to Senior",
                    tone = "B2B SaaS / Revenue",
                    notes = "Enterprise AE for $100k+ ACV software contracts. MEDDPICC methodology, outbound prospecting, negotiating with C-suite executives, partnering with solution engineers. High EQ, competitive, quota crusher."
                )
            )

            templates.forEach { tmpl ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .clickable {
                            onSelectTemplate(tmpl.title, tmpl.notes, tmpl.level, tmpl.tone)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tmpl.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Raw Notes Input
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Hiring Notes & Requirements",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (uiState.rawNotes.isNotEmpty()) {
                    IconButton(
                        onClick = { onNotesChange("") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear notes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = uiState.rawNotes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("raw_notes_input"),
                placeholder = {
                    Text("Jot down any unorganized requirements, tech stack, team goals, compensation range, culture requirements, soft skills, or quirks...")
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        // Seniority Level Chips
        Column {
            Text(
                text = "Target Experience Level",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val levels = listOf(
                    "Junior (1-2 yrs)",
                    "Mid-Level (3-4 yrs)",
                    "Senior (5+ yrs)",
                    "Staff / Principal (8+ yrs)",
                    "Director / Executive"
                )
                levels.forEach { level ->
                    FilterChip(
                        selected = uiState.roleLevel == level,
                        onClick = { onRoleLevelChange(level) },
                        label = { Text(level) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Industry / Tone
        Column {
            Text(
                text = "Target Culture / Tone",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tones = listOf(
                    "High-Growth Tech / Modern",
                    "Elite Enterprise / Corporate",
                    "Early-Stage Startup / scrappy",
                    "Mission-Driven / Culture First"
                )
                tones.forEach { tone ->
                    FilterChip(
                        selected = uiState.industryOrTone == tone,
                        onClick = { onToneChange(tone) },
                        label = { Text(tone) }
                    )
                }
            }
        }

        // Model & Thinking Mode Configuration Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Thinking mode",
                            tint = if (uiState.enableThinking) GoldStar else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "High Thinking Mode (gemini-4-argon)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sets thinkingLevel = HIGH for deep competency extraction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.enableThinking,
                        onCheckedChange = onToggleThinking,
                        modifier = Modifier.testTag("thinking_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldStar,
                            checkedTrackColor = GoldStar.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Model Selection
                Text(
                    text = "Select Gemini Model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GeminiModelOption.values().forEach { model ->
                        val isSelected = uiState.selectedModel == model
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onModelChange(model) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = when (model) {
                                        GeminiModelOption.FLASH_LITE -> "3.1 Lite"
                                        GeminiModelOption.FLASH_38 -> "3.8 Flash"
                                        GeminiModelOption.ARGON_THINKING -> "4 Argon"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (model) {
                                        GeminiModelOption.FLASH_LITE -> "Fast"
                                        GeminiModelOption.FLASH_38 -> "General"
                                        GeminiModelOption.ARGON_THINKING -> "High Think"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Progress or Generate Button
        if (uiState.isGenerating) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = uiState.generationProgressText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Button(
                onClick = onGenerate,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("generate_pack_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Generate Recruitment Pack",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class RoleTemplate(
    val label: String,
    val title: String,
    val level: String,
    val tone: String,
    val notes: String
)
