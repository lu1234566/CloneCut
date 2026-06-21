package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// Plain Kotlin Timeline Models matching standard CapCut attributes
data class VideoTimelineClip(
    val id: String,
    val sourceName: String,
    val startSec: Float,
    val durationSec: Float,
    val speedMultiplier: Float = 1.0f,
    val transition: String = "Nenhum", // e.g. Fade, Zoom, Glitch, Blur
    val filterIntensity: Float = 1.0f
) {
    fun toSerialized(): String = "$id|$sourceName|$startSec|$durationSec|$speedMultiplier|$transition|$filterIntensity"
    companion object {
        fun fromSerialized(s: String): VideoTimelineClip {
            val parts = s.split("|")
            return VideoTimelineClip(
                id = parts.getOrNull(0) ?: "clip_0",
                sourceName = parts.getOrNull(1) ?: "Natureza",
                startSec = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
                durationSec = parts.getOrNull(3)?.toFloatOrNull() ?: 4f,
                speedMultiplier = parts.getOrNull(4)?.toFloatOrNull() ?: 1.0f,
                transition = parts.getOrNull(5) ?: "Nenhum",
                filterIntensity = parts.getOrNull(6)?.toFloatOrNull() ?: 1.0f
            )
        }
    }
}

data class AudioTimelineClip(
    val id: String,
    val audioName: String,
    val startSec: Float,
    val durationSec: Float,
    val volume: Float = 0.8f,
    val isNarratorVoice: Boolean = false
) {
    fun toSerialized(): String = "$id|$audioName|$startSec|$durationSec|$volume|$isNarratorVoice"
    companion object {
        fun fromSerialized(s: String): AudioTimelineClip {
            val parts = s.split("|")
            return AudioTimelineClip(
                id = parts.getOrNull(0) ?: "aud_0",
                audioName = parts.getOrNull(1) ?: "Música Pop Chill",
                startSec = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
                durationSec = parts.getOrNull(3)?.toFloatOrNull() ?: 4f,
                volume = parts.getOrNull(4)?.toFloatOrNull() ?: 0.8f,
                isNarratorVoice = parts.getOrNull(5)?.toBoolean() ?: false
            )
        }
    }
}

data class TextTimelineClip(
    val id: String,
    val text: String,
    val startSec: Float,
    val durationSec: Float,
    val colorHex: String = "#FFFFFF",
    val fontSizeSp: Float = 16f,
    val styleAnim: String = "Surgir" // e.g. Glitch, Neon Sparkle, Fade, Slide
) {
    fun toSerialized(): String = "$id|$text|$startSec|$durationSec|$colorHex|$fontSizeSp|$styleAnim"
    companion object {
        fun fromSerialized(s: String): TextTimelineClip {
            val parts = s.split("|")
            return TextTimelineClip(
                id = parts.getOrNull(0) ?: "txt_0",
                text = parts.getOrNull(1) ?: "Olá, Criador!",
                startSec = parts.getOrNull(2)?.toFloatOrNull() ?: 0f,
                durationSec = parts.getOrNull(3)?.toFloatOrNull() ?: 3f,
                colorHex = parts.getOrNull(4) ?: "#FFFFFF",
                fontSizeSp = parts.getOrNull(5)?.toFloatOrNull() ?: 16f,
                styleAnim = parts.getOrNull(6) ?: "Surgir"
            )
        }
    }
}

class ChatRepository(
    private val projectDao: VideoProjectDao,
    private val apiService: GeminiApiService = RetrofitClient.service
) {
    val allProjects: Flow<List<VideoProject>> = projectDao.getAllProjects()

    suspend fun saveProject(project: VideoProject): Long = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
    }

    suspend fun deleteProject(project: VideoProject) = withContext(Dispatchers.IO) {
        projectDao.deleteProject(project)
    }

    suspend fun getProjectById(id: Long): VideoProject? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    // Helper functions to convert lists of tracks into Room persistent JSON/Strings
    fun serializeClips(clips: List<VideoTimelineClip>): String = clips.joinToString(";") { it.toSerialized() }
    fun deserializeClips(data: String): List<VideoTimelineClip> {
        if (data.isBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { VideoTimelineClip.fromSerialized(it) }
    }

    fun serializeAudio(audios: List<AudioTimelineClip>): String = audios.joinToString(";") { it.toSerialized() }
    fun deserializeAudio(data: String): List<AudioTimelineClip> {
        if (data.isBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { AudioTimelineClip.fromSerialized(it) }
    }

    fun serializeTexts(texts: List<TextTimelineClip>): String = texts.joinToString(";") { it.toSerialized() }
    fun deserializeTexts(data: String): List<TextTimelineClip> {
        if (data.isBlank()) return emptyList()
        return data.split(";").filter { it.isNotBlank() }.map { TextTimelineClip.fromSerialized(it) }
    }

    /**
     * Gemini AI Generation:
     * Asks Gemini to construct a complete split video script scenario.
     * We will parse the output to construct a high-fidelity preset timeline in Portuguese.
     */
    suspend fun draftAiScriptToTimeline(
        niche: String,
        targetDurationSec: Int,
        userRequests: String,
        customApiKey: String? = null
    ): List<TextTimelineClip> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Static smart fallback generator if API key is not configured yet
            return@withContext listOf(
                TextTimelineClip("ai_0", "⚡ Gancho Viral: Você sabia disso?", 0f, 3f, "#00F2FE", 18f, "Neon"),
                TextTimelineClip("ai_1", "💡 Fato: CapCut usa I.A. para acelerar edições.", 3f, 7f, "#FFFFFF", 16f, "Surgir"),
                TextTimelineClip("ai_2", "🔥 Dica Extra: Use curvas de velocidade!", 7f, 10f, "#FE0979", 16f, "Glitch"),
                TextTimelineClip("ai_3", "👉 Me segue para mais truques!", 10f, targetDurationSec.toFloat(), "#10B981", 17f, "Slide")
            )
        }

        val prompt = """
            You are a viral TikTok & YouTube Short producer on CapCut.
            Create a highly interactive text subtitle track split into EXACTLY 4 logical sections for a video of $targetDurationSec seconds on the topic: "$niche".
            Additional Requests: "$userRequests"
            
            Return ONLY 4 lines, each representing one text clip segment in the format:
            SEC_START|SEC_END|SUBTITLE_TEXT
            
            Guidelines:
            - Split the total $targetDurationSec seconds fairly.
            - Ensure subtitles are in Portuguese, highly catchy and engaging.
            - Your reply MUST ONLY have 4 lines in this exact format, with NO introduction and NO other marks.
            Example:
            0|3|⚡ Gancho: O maior segredo de todos!
            3|7|💡 Quase ninguém te conta esse detalhe...
            7|11|🚀 Basta aplicar o efeito de flash em 3D.
            11|$targetDurationSec|🔥 Siga para receber o próximo tutorial!
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val outputList = mutableListOf<TextTimelineClip>()
            var index = 0

            text.lines().forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    val start = parts[0].trim().toFloatOrNull() ?: (index * 3f)
                    val end = parts[1].trim().toFloatOrNull() ?: ((index + 1) * 3f)
                    val caption = parts[2].trim()
                    
                    val duration = if (end > start) end - start else 3f
                    val effect = if (index % 2 == 0) "Glitch" else "Neon Sparkle"
                    outputList.add(
                        TextTimelineClip(
                            id = "ai_$index",
                            text = caption,
                            startSec = start,
                            durationSec = duration,
                            colorHex = if (index == 0) "#00F2FE" else "#FFFFFF",
                            styleAnim = effect
                        )
                    )
                    index++
                }
            }

            if (outputList.isEmpty()) {
                // Secondary parsing if formatting slightly differs
                listOf(
                    TextTimelineClip("ai_0", "⚡ Gancho: Olha que sensacional!", 0f, 3f, "#00F2FE", 18f, "Neon"),
                    TextTimelineClip("ai_1", "💡 Sabia que pode cortar clipes com IA?", 3f, 7f, "#FFFFFF", 16f, "Surgir"),
                    TextTimelineClip("ai_2", "🔥 Além de fazer transições em um toque.", 7f, 10f, "#FE0979", 16f, "Glitch"),
                    TextTimelineClip("ai_3", "👉 Inscreva-se e edite seu vídeo no CapCut!", 10f, targetDurationSec.toFloat(), "#10B981", 17f, "Slide")
                )
            } else {
                outputList
            }
        } catch (e: Exception) {
            // Dynamic helpful fallback with user parameters
            listOf(
                TextTimelineClip("ai_0", "⚡ Gancho: $niche", 0f, 3f, "#00F2FE", 18f, "Neon"),
                TextTimelineClip("ai_1", "💡 Dica de nível Pro sobre este nicho.", 3f, 7f, "#FFFFFF", 16f, "Surgir"),
                TextTimelineClip("ai_2", "🍀 Lembra de manter as batidas ativas no CapCut.", 7f, 10f, "#FE0979", 16f, "Glitch"),
                TextTimelineClip("ai_3", "👉 Salve o projeto e veja o resultado final!", 10f, targetDurationSec.toFloat(), "#10B981", 17f, "Slide")
            )
        }
    }
}
