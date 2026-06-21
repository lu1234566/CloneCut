package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerationConfig
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.local.VideoProject
import com.example.data.local.VideoProjectDao
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.max

@JsonClass(generateAdapter = true)
data class VideoTimelineClip(
    val id: String,
    val sourceName: String,
    val startSec: Float,
    val durationSec: Float,
    val speedMultiplier: Float = 1.0f,
    val transition: String = "Nenhum",
    val filterIntensity: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class AudioTimelineClip(
    val id: String,
    val audioName: String,
    val startSec: Float,
    val durationSec: Float,
    val volume: Float = 0.8f,
    val isNarratorVoice: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TextTimelineClip(
    val id: String,
    val text: String,
    val startSec: Float,
    val durationSec: Float,
    val colorHex: String = "#FFFFFF",
    val fontSizeSp: Float = 16f,
    val styleAnim: String = "Surgir"
)

class ChatRepository(
    private val projectDao: VideoProjectDao,
    private val apiService: GeminiApiService = RetrofitClient.service
) {
    val allProjects: Flow<List<VideoProject>> = projectDao.getAllProjects()
    private val moshi = Moshi.Builder().build()
    private val clipsAdapter = moshi.adapter<List<VideoTimelineClip>>(Types.newParameterizedType(List::class.java, VideoTimelineClip::class.java))
    private val audioAdapter = moshi.adapter<List<AudioTimelineClip>>(Types.newParameterizedType(List::class.java, AudioTimelineClip::class.java))
    private val textsAdapter = moshi.adapter<List<TextTimelineClip>>(Types.newParameterizedType(List::class.java, TextTimelineClip::class.java))

    suspend fun saveProject(project: VideoProject): Long = withContext(Dispatchers.IO) { projectDao.insertProject(project) }
    suspend fun deleteProject(project: VideoProject) = withContext(Dispatchers.IO) { projectDao.deleteProject(project) }
    suspend fun getProjectById(id: Long): VideoProject? = withContext(Dispatchers.IO) { projectDao.getProjectById(id) }

    fun serializeClips(clips: List<VideoTimelineClip>): String = clipsAdapter.toJson(clips)
    fun serializeAudio(audios: List<AudioTimelineClip>): String = audioAdapter.toJson(audios)
    fun serializeTexts(texts: List<TextTimelineClip>): String = textsAdapter.toJson(texts)

    fun deserializeClips(data: String): List<VideoTimelineClip> = parse(data) { clipsAdapter.fromJson(it) }
    fun deserializeAudio(data: String): List<AudioTimelineClip> = parse(data) { audioAdapter.fromJson(it) }
    fun deserializeTexts(data: String): List<TextTimelineClip> = parse(data) { textsAdapter.fromJson(it) }

    private fun <T> parse(data: String, reader: (String) -> List<T>?): List<T> {
        if (data.isBlank()) return emptyList()
        return try { reader(data) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    suspend fun draftAiScriptToTimeline(
        niche: String,
        targetDurationSec: Int,
        userRequests: String,
        customApiKey: String? = null
    ): List<TextTimelineClip> = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext fallbackTimeline(niche, targetDurationSec)
        val prompt = "Gere exatamente quatro legendas em portugues para video curto. Tema: $niche. Duracao: $targetDurationSec segundos. Pedido: $userRequests. Responda somente linhas no formato inicio|fim|texto."
        val request = GeminiRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(temperature = 0.7, maxOutputTokens = 512)
        )
        return@withContext try {
            val text = apiService.generateContent(apiKey, request).candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
            parseGeminiTimeline(text, targetDurationSec).ifEmpty { fallbackTimeline(niche, targetDurationSec) }
        } catch (e: Exception) {
            fallbackTimeline(niche, targetDurationSec)
        }
    }

    private fun parseGeminiTimeline(text: String, targetDurationSec: Int): List<TextTimelineClip> {
        val maxDuration = targetDurationSec.coerceAtLeast(4).toFloat()
        val latestSafeStart = (maxDuration - 0.5f).coerceAtLeast(0f)
        val out = mutableListOf<TextTimelineClip>()
        text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.take(8).forEachIndexed { index, line ->
            val parts = line.split("|", limit = 3)
            if (parts.size >= 3) {
                val rawStart = parts[0].trim().toFloatOrNull() ?: (index * maxDuration / 4f)
                val start = rawStart.coerceIn(0f, latestSafeStart)
                val rawEnd = parts[1].trim().toFloatOrNull() ?: ((index + 1) * maxDuration / 4f)
                val end = rawEnd.coerceIn(start + 0.5f, maxDuration)
                val caption = parts[2].trim().ifBlank { "Cena ${index + 1}" }
                out.add(TextTimelineClip("ai_${index}_${System.nanoTime()}", caption, start, max(0.5f, end - start), if (index == 0) "#00F2FE" else "#FFFFFF", if (index == 0) 18f else 16f, if (index % 2 == 0) "Glitch" else "Neon Sparkle"))
            }
        }
        return out.take(4)
    }

    private fun fallbackTimeline(niche: String, targetDurationSec: Int): List<TextTimelineClip> {
        val total = targetDurationSec.coerceAtLeast(12).toFloat()
        val block = total / 4f
        val topic = niche.ifBlank { "CloneCut" }
        val stamp = System.nanoTime()
        return listOf(
            TextTimelineClip("ai_0_$stamp", "Gancho: $topic", 0f, block, "#00F2FE", 18f, "Neon"),
            TextTimelineClip("ai_1_$stamp", "Dica de nivel Pro sobre este nicho.", block, block, "#FFFFFF", 16f, "Surgir"),
            TextTimelineClip("ai_2_$stamp", "Mantenha os cortes alinhados com a batida.", block * 2, block, "#FE0979", 16f, "Glitch"),
            TextTimelineClip("ai_3_$stamp", "Salve o projeto e veja o resultado final.", block * 3, block, "#10B981", 17f, "Slide")
        )
    }
}
