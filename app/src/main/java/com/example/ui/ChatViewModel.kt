package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.VideoProject
import com.example.data.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    application: Application,
    private val repository: ChatRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("capcut_clone_prefs", Context.MODE_PRIVATE)

    // Current screen Tab: "editor", "projects", "gallery", "settings"
    private val _currentTab = MutableStateFlow("editor")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Loaded project
    private val _activeProject = MutableStateFlow<VideoProject?>(null)
    val activeProject: StateFlow<VideoProject?> = _activeProject.asStateFlow()

    // All projects from local Room db
    val savedProjects: StateFlow<List<VideoProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks editor states
    private val _activeClips = MutableStateFlow<List<VideoTimelineClip>>(emptyList())
    val activeClips: StateFlow<List<VideoTimelineClip>> = _activeClips.asStateFlow()

    private val _activeAudio = MutableStateFlow<List<AudioTimelineClip>>(emptyList())
    val activeAudio: StateFlow<List<AudioTimelineClip>> = _activeAudio.asStateFlow()

    private val _activeTexts = MutableStateFlow<List<TextTimelineClip>>(emptyList())
    val activeTexts: StateFlow<List<TextTimelineClip>> = _activeTexts.asStateFlow()

    // Playback state
    private val _currentTime = MutableStateFlow(0.0f)
    val currentTime: StateFlow<Float> = _currentTime.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Element selections
    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedAudioId = MutableStateFlow<String?>(null)
    val selectedAudioId: StateFlow<String?> = _selectedAudioId.asStateFlow()

    private val _selectedTextId = MutableStateFlow<String?>(null)
    val selectedTextId: StateFlow<String?> = _selectedTextId.asStateFlow()

    // Project attributes
    private val _activeFilter = MutableStateFlow("Nenhum")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    private val _speedRampProfile = MutableStateFlow("Normal")
    val speedRampProfile: StateFlow<String> = _speedRampProfile.asStateFlow()

    private val _useChromaKey = MutableStateFlow(false)
    val useChromaKey: StateFlow<Boolean> = _useChromaKey.asStateFlow()

    private val _chromaColorHex = MutableStateFlow("#00FF00") // Green Screen
    val chromaColorHex: StateFlow<String> = _chromaColorHex.asStateFlow()

    // --- CAPCUT PREMIUM AI STATES ---
    private val _useAiBackgroundRemover = MutableStateFlow(false)
    val useAiBackgroundRemover: StateFlow<Boolean> = _useAiBackgroundRemover.asStateFlow()

    private val _useAiNoiseReduction = MutableStateFlow(false)
    val useAiNoiseReduction: StateFlow<Boolean> = _useAiNoiseReduction.asStateFlow()

    private val _portraitStyle = MutableStateFlow("Nenhum")
    val portraitStyle: StateFlow<String> = _portraitStyle.asStateFlow()

    private val _selectedAiVoice = MutableStateFlow("Narrador de Cinema")
    val selectedAiVoice: StateFlow<String> = _selectedAiVoice.asStateFlow()

    // UI Utilities
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    private val _aiTopicInput = MutableStateFlow("")
    val aiTopicInput: StateFlow<String> = _aiTopicInput.asStateFlow()

    // Settings
    private val _customApiKey = MutableStateFlow(sharedPrefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Image/Video Render Sample references (determines which color block visual renders in preview)
    val sampleVideoThemes = listOf(
        "Abertura Neo" to "#1A1F32",
        "Cena Natureza" to "#10B981",
        "Fração Urbana" to "#FE0979",
        "Encerramento" to "#4F46E5"
    )

    // Export progress bouncer simulation
    private val _exportProgress = MutableStateFlow(-1) // -1 = not exporting, 0-100 = active progress
    val exportProgress: StateFlow<Int> = _exportProgress.asStateFlow()

    private val _exportedVideos = MutableStateFlow<List<String>>(emptyList())
    val exportedVideos: StateFlow<List<String>> = _exportedVideos.asStateFlow()

    init {
        // Automatic ticker coroutine for playhead simulation
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (_isPlaying.value) {
                    val maxS = getMaxTimelineDuration()
                    if (maxS > 0f) {
                        var nextVal = _currentTime.value + 0.1f
                        if (nextVal >= maxS) {
                            nextVal = 0.0f // Loop back
                        }
                        _currentTime.value = nextVal
                    } else {
                        _isPlaying.value = false
                    }
                }
            }
        }

        // Prepopulate standard samples when first launching so empty screen is not shown
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                if (list.isEmpty()) {
                    createStarterProject("Meu Vídeo Autoral 1")
                } else if (_activeProject.value == null) {
                    loadProject(list.first())
                }
            }
        }

        // Restore custom gallery lists
        _exportedVideos.value = sharedPrefs.getStringSet("exported_clips", emptySet())?.toList() ?: emptyList()
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun setAiTopic(topic: String) {
        _aiTopicInput.value = topic
    }

    // Playback modifiers
    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
    }

    fun seekTo(timeSec: Float) {
        val maxDuration = getMaxTimelineDuration()
        _currentTime.value = timeSec.coerceIn(0f, maxDuration)
    }

    fun getMaxTimelineDuration(): Float {
        val maxClip = _activeClips.value.map { it.startSec + it.durationSec }.maxOrNull() ?: 12f
        val maxAud = _activeAudio.value.map { it.startSec + it.durationSec }.maxOrNull() ?: 0f
        val maxTxt = _activeTexts.value.map { it.startSec + it.durationSec }.maxOrNull() ?: 0f
        return maxOf(maxClip, maxAud, maxTxt).coerceAtLeast(6f)
    }

    // --- TIMELINE ACTIONS DECK ---

    // 1. SPLIT (Corte) at playhead position
    fun splitSelectedOrActiveClip() {
        val curTime = _currentTime.value
        val clips = _activeClips.value.toMutableList()
        val target = clips.find { curTime > it.startSec && curTime < (it.startSec + it.durationSec) }
            ?: _activeClips.value.find { it.id == _selectedClipId.value }
            ?: return

        val offset = curTime - target.startSec
        if (offset > 0.5f && (target.durationSec - offset) > 0.5f) {
            val index = clips.indexOf(target)
            clips.removeAt(index)

            val part1 = target.copy(id = UUID.randomUUID().toString(), durationSec = offset)
            val part2 = target.copy(id = UUID.randomUUID().toString(), startSec = curTime, durationSec = target.durationSec - offset)

            clips.add(index, part1)
            clips.add(index + 1, part2)

            _activeClips.value = clips
            _selectedClipId.value = part2.id
            saveCurrentStateToDb()
        }
    }

    // 2. TRIM Left / Right
    fun trimSelectedClip(isLeft: Boolean, delta: Float) {
        val cid = _selectedClipId.value ?: return
        val clips = _activeClips.value.toMutableList()
        val index = clips.indexOfFirst { it.id == cid }
        if (index != -1) {
            val clip = clips[index]
            if (isLeft) {
                val nextStart = (clip.startSec + delta).coerceIn(0f, clip.startSec + clip.durationSec - 0.5f)
                val diff = nextStart - clip.startSec
                clips[index] = clip.copy(startSec = nextStart, durationSec = clip.durationSec - diff)
            } else {
                val nextDur = (clip.durationSec + delta).coerceAtLeast(0.5f)
                clips[index] = clip.copy(durationSec = nextDur)
            }
            _activeClips.value = clips
            saveCurrentStateToDb()
        }
    }

    // 3. Delete Highlighted clip
    fun deleteSelectedClip() {
        val cid = _selectedClipId.value
        if (cid != null && _activeClips.value.size > 1) {
            _activeClips.value = _activeClips.value.filterNot { it.id == cid }
            _selectedClipId.value = _activeClips.value.firstOrNull()?.id
            saveCurrentStateToDb()
        }
    }

    // 4. Duplicate Clip
    fun duplicateSelectedClip() {
        val cid = _selectedClipId.value ?: return
        val original = _activeClips.value.find { it.id == cid } ?: return
        val clips = _activeClips.value.toMutableList()
        val index = clips.indexOf(original)

        val duration = original.durationSec
        val lastEnd = clips.map { it.startSec + it.durationSec }.maxOrNull() ?: 12f

        val duplicated = original.copy(
            id = UUID.randomUUID().toString(),
            startSec = lastEnd
        )
        clips.add(index + 1, duplicated)
        _activeClips.value = clips
        _selectedClipId.value = duplicated.id
        saveCurrentStateToDb()
    }

    // 5. Speed Ramp curves
    fun setSpeedRamp(ramp: String) {
        _speedRampProfile.value = ramp
        val cid = _selectedClipId.value ?: return
        val clips = _activeClips.value.toMutableList()
        val idx = clips.indexOfFirst { it.id == cid }
        if (idx != -1) {
            val mult = when (ramp) {
                "Montagem Jump" -> 2.0f
                "Hero Flash" -> 3.0f
                "Bullet Time" -> 0.2f
                else -> 1.0f
            }
            clips[idx] = clips[idx].copy(speedMultiplier = mult)
            _activeClips.value = clips
            saveCurrentStateToDb()
        }
    }

    // Adjusting Active Filter
    fun setFilter(filter: String) {
        _activeFilter.value = filter
        saveCurrentStateToDb()
    }

    // Chroma key settings
    fun setChromaEnabled(enabled: Boolean) {
        _useChromaKey.value = enabled
        saveCurrentStateToDb()
    }

    fun setChromaColor(color: String) {
        _chromaColorHex.value = color
        saveCurrentStateToDb()
    }

    // Add audio element
    fun addAudioTrack(name: String, isNarrator: Boolean) {
        val activeProj = _activeProject.value ?: return
        val sounds = _activeAudio.value.toMutableList()
        val start = _currentTime.value
        val newSound = AudioTimelineClip(
            id = UUID.randomUUID().toString(),
            audioName = name,
            startSec = start,
            durationSec = 5f,
            volume = 0.8f,
            isNarratorVoice = isNarrator
        )
        sounds.add(newSound)
        _activeAudio.value = sounds
        _selectedAudioId.value = newSound.id
        saveCurrentStateToDb()
    }

    fun removeSelectedAudio() {
        val aid = _selectedAudioId.value ?: return
        _activeAudio.value = _activeAudio.value.filterNot { it.id == aid }
        _selectedAudioId.value = null
        saveCurrentStateToDb()
    }

    // Add caption cards
    fun addTextCard(content: String) {
        val textsList = _activeTexts.value.toMutableList()
        val start = _currentTime.value
        val newText = TextTimelineClip(
            id = UUID.randomUUID().toString(),
            text = content,
            startSec = start,
            durationSec = 3f,
            colorHex = "#FFFFFF",
            fontSizeSp = 16f,
            styleAnim = "Surgir"
        )
        textsList.add(newText)
        _activeTexts.value = textsList
        _selectedTextId.value = newText.id
        saveCurrentStateToDb()
    }

    fun updateSelectedTextContent(newTxt: String) {
        val tid = _selectedTextId.value ?: return
        _activeTexts.value = _activeTexts.value.map {
            if (it.id == tid) it.copy(text = newTxt) else it
        }
        saveCurrentStateToDb()
    }

    fun removeSelectedText() {
        val tid = _selectedTextId.value ?: return
        _activeTexts.value = _activeTexts.value.filterNot { it.id == tid }
        _selectedTextId.value = null
        saveCurrentStateToDb()
    }

    fun selectClip(id: String?) {
        _selectedClipId.value = id
    }

    fun selectAudio(id: String?) {
        _selectedAudioId.value = id
    }

    fun selectText(id: String?) {
        _selectedTextId.value = id
    }

    // --- CAPCUT AI ACTIONS & SETTERS ---
    fun setAiBackgroundRemover(enabled: Boolean) {
        _useAiBackgroundRemover.value = enabled
    }

    fun setAiNoiseReduction(enabled: Boolean) {
        _useAiNoiseReduction.value = enabled
    }

    fun setPortraitStyle(style: String) {
        _portraitStyle.value = style
    }

    fun setSelectedAiVoice(voice: String) {
        _selectedAiVoice.value = voice
    }

    fun updateAudioVolume(id: String, newVol: Float) {
        val sounds = _activeAudio.value.map { aud ->
            if (aud.id == id) {
                aud.copy(volume = newVol)
            } else {
                aud
            }
        }
        _activeAudio.value = sounds
        saveCurrentStateToDb()
    }

    fun updateSelectedAudioVolume(newVol: Float) {
        val selId = _selectedAudioId.value ?: return
        updateAudioVolume(selId, newVol)
    }

    fun convertTextToAiVoiceover() {
        val selectedTxtId = _selectedTextId.value ?: return
        val textClip = _activeTexts.value.find { it.id == selectedTxtId } ?: return
        val voiceName = _selectedAiVoice.value

        val voiceAudio = AudioTimelineClip(
            id = "tts_${UUID.randomUUID().hashCode()}",
            audioName = "🗣️ [$voiceName]: \"${if (textClip.text.length > 15) textClip.text.take(15) + "..." else textClip.text}\"",
            startSec = textClip.startSec,
            durationSec = textClip.durationSec,
            volume = 1.0f,
            isNarratorVoice = true
        )

        val updatedAudios = _activeAudio.value.toMutableList()
        // Remove existing TTS audio starting at same place to allow redo
        val filtered = updatedAudios.filterNot { it.startSec == textClip.startSec && it.isNarratorVoice }
        val finalAudios = filtered + voiceAudio
        _activeAudio.value = finalAudios
        _selectedAudioId.value = voiceAudio.id
        saveCurrentStateToDb()
    }

    // --- AI POWERED SHORTCUTS ---

    // 1. Legendas Automáticas: transcribe current project audio track into synchronous subtitles
    fun triggerAutoCaps() {
        val generatedSubtexts = mutableListOf<TextTimelineClip>()
        val currentAudios = _activeAudio.value
        val currentClips = _activeClips.value

        if (currentAudios.isNotEmpty()) {
            // Transcribe from separate audio tracks
            currentAudios.forEachIndexed { idx, aud ->
                val cleanName = aud.audioName.replace(Regex("🗣️|🎵|🎵\\s|🗣️\\s|🎧|\\[.*?\\]:?\\s?|\\\""), "")
                generatedSubtexts.add(
                    TextTimelineClip(
                        id = "auto_cap_aud_$idx",
                        text = "🎤 $cleanName",
                        startSec = aud.startSec,
                        durationSec = aud.durationSec,
                        colorHex = "#00F2FE", // Cyan
                        fontSizeSp = 15f,
                        styleAnim = "Neon Sparkle"
                    )
                )
            }
        } else if (currentClips.isNotEmpty()) {
            // No audio tracks, transcribe based on thematic video clips
            currentClips.forEachIndexed { idx, clip ->
                val dialogue = when (clip.sourceName) {
                    "Natureza Intro" -> "🍀 O amanhecer de um novo dia na floresta..."
                    "Ação Cortada" -> "⚡ Sinta a adrenalina pura em movimento rápido!"
                    "Cidade Broll" -> "🌆 O coração frenético da metrópole à noite..."
                    else -> "✨ Desvendando novos ângulos com tecnologia I.A."
                }
                generatedSubtexts.add(
                    TextTimelineClip(
                        id = "auto_cap_clip_$idx",
                        text = dialogue,
                        startSec = clip.startSec,
                        durationSec = clip.durationSec,
                        colorHex = "#FFFF00", // Yellow for video track speech transcription!
                        fontSizeSp = 15f,
                        styleAnim = "Letra por Letra"
                    )
                )
            }
        }
        _activeTexts.value = generatedSubtexts
        saveCurrentStateToDb()
    }

    // 2. IA Script-to-Timeline: calling Gemini and appending directly to live tracks
    fun generateAiContentToTimeline() {
        val topic = _aiTopicInput.value
        if (topic.isBlank() || _isGeneratingAi.value) return
        _isGeneratingAi.value = true

        viewModelScope.launch {
            val duration = getMaxTimelineDuration().toInt().coerceIn(10, 60)
            
            // Runs Gemini builder in background threads
            val responseCaptions = repository.draftAiScriptToTimeline(
                niche = topic,
                targetDurationSec = duration,
                userRequests = "Incluir ganchos rápidos para redes sociais",
                customApiKey = _customApiKey.value
            )

            // Construct new tracks
            _activeTexts.value = responseCaptions

            // Add corresponding simulated synthesised narrative speech voiceovers!
            val updatedAudios = mutableListOf<AudioTimelineClip>()
            responseCaptions.forEachIndexed { i, sub ->
                val shortCaps = if (sub.text.length > 20) sub.text.take(20) + "..." else sub.text
                updatedAudios.add(
                    AudioTimelineClip(
                        id = "narrator_ai_$i",
                        audioName = "IA Voz: \"$shortCaps\"",
                        startSec = sub.startSec,
                        durationSec = sub.durationSec,
                        volume = 1.0f,
                        isNarratorVoice = true
                    )
                )
            }
            _activeAudio.value = updatedAudios

            // Clear settings
            _aiTopicInput.value = ""
            _isGeneratingAi.value = false
            saveCurrentStateToDb()
        }
    }

    // --- SAVING & CREATING PROJECTS IN DATABASE ---

    fun loadProject(project: VideoProject) {
        _activeProject.value = project
        _activeFilter.value = project.activeFilter
        _speedRampProfile.value = project.speedRampProfile
        _useChromaKey.value = project.useChromaKey
        _chromaColorHex.value = project.chromaBgColor

        _activeClips.value = repository.deserializeClips(project.clipsJson)
        _activeAudio.value = repository.deserializeAudio(project.audioJson)
        _activeTexts.value = repository.deserializeTexts(project.textsJson)

        _selectedClipId.value = _activeClips.value.firstOrNull()?.id
        _selectedAudioId.value = null
        _selectedTextId.value = null
        _currentTime.value = 0.0f
    }

    fun createStarterProject(title: String) {
        viewModelScope.launch {
            val defaultClips = listOf(
                VideoTimelineClip("sc_1", "Natureza Intro", 0f, 4f, 1f, "Fade"),
                VideoTimelineClip("sc_2", "Ação Cortada", 4f, 4f, 1f, "Glitch"),
                VideoTimelineClip("sc_3", "Cidade Broll", 8f, 4f, 1f, "Zoom")
            )
            val defaultAudio = listOf(
                AudioTimelineClip("aud_1", "Batida Lo-fi Estilo", 0f, 12f, 0.4f, false)
            )
            val defaultTexts = listOf(
                TextTimelineClip("st_1", "⚡ Seja Bem-Vindo!", 0.5f, 3f, "#FFFFFF", 16f, "Surgir")
            )

            val newProj = VideoProject(
                title = title,
                durationSeconds = 12,
                clipsJson = repository.serializeClips(defaultClips),
                audioJson = repository.serializeAudio(defaultAudio),
                textsJson = repository.serializeTexts(defaultTexts)
            )

            val newId = repository.saveProject(newProj)
            val inserted = repository.getProjectById(newId)
            if (inserted != null) {
                loadProject(inserted)
            }
        }
    }

    fun saveCurrentStateToDb() {
        val currentProj = _activeProject.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updated = currentProj.copy(
                activeFilter = _activeFilter.value,
                speedRampProfile = _speedRampProfile.value,
                useChromaKey = _useChromaKey.value,
                chromaBgColor = _chromaColorHex.value,
                clipsJson = repository.serializeClips(_activeClips.value),
                audioJson = repository.serializeAudio(_activeAudio.value),
                textsJson = repository.serializeTexts(_activeTexts.value)
            )
            repository.saveProject(updated)
            _activeProject.value = updated
        }
    }

    fun deleteProjectItem(project: VideoProject) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                // Clear active
                _activeClips.value = emptyList()
                _activeAudio.value = emptyList()
                _activeTexts.value = emptyList()
            }
        }
    }

    // --- EXP PORT SIMULATION ---

    fun triggerExportSimulation(resolution: String, fps: Int) {
        if (_exportProgress.value != -1) return
        _exportProgress.value = 0

        viewModelScope.launch {
            for (p in 0..100 step 10) {
                _exportProgress.value = p
                delay(200)
            }
            // Done
            val activeTitle = _activeProject.value?.title ?: "Meu Projeto"
            val idStr = UUID.randomUUID().toString().take(6)
            val fileDetail = "$activeTitle ($resolution @ $fps FPS)_$idStr.mp4"

            val updatedGallery = _exportedVideos.value.toMutableList()
            updatedGallery.add(0, fileDetail)
            _exportedVideos.value = updatedGallery

            // Persist locally
            sharedPrefs.edit().putStringSet("exported_clips", updatedGallery.toSet()).apply()

            _exportProgress.value = -1 // reset progress bouncer
        }
    }

    fun clearMockGallery() {
        _exportedVideos.value = emptyList()
        sharedPrefs.edit().remove("exported_clips").apply()
    }

    // Save Custom API Key
    fun saveApiKey(key: String) {
        _customApiKey.value = key
        sharedPrefs.edit().putString("custom_api_key", key).apply()
    }
}

class ChatViewModelFactory(
    private val application: Application,
    private val repository: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
