package com.example.data.repository

import com.example.data.api.GeminiApiService
import com.example.data.api.RetrofitClient
import com.example.data.local.VideoProject
import com.example.data.local.VideoProjectDao
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
        fallbackTimeline(niche, targetDurationSec)
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
