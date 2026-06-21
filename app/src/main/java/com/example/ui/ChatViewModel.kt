package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.VideoProject
import com.example.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application, private val repository: ChatRepository) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("capcut_clone_prefs", 0)
    private val _currentTab = MutableStateFlow("editor"); val currentTab = _currentTab.asStateFlow()
    private val _activeProject = MutableStateFlow<VideoProject?>(null); val activeProject = _activeProject.asStateFlow()
    val savedProjects = repository.allProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _activeClips = MutableStateFlow<List<VideoTimelineClip>>(emptyList()); val activeClips = _activeClips.asStateFlow()
    private val _activeAudio = MutableStateFlow<List<AudioTimelineClip>>(emptyList()); val activeAudio = _activeAudio.asStateFlow()
    private val _activeTexts = MutableStateFlow<List<TextTimelineClip>>(emptyList()); val activeTexts = _activeTexts.asStateFlow()
    private val _currentTime = MutableStateFlow(0f); val currentTime = _currentTime.asStateFlow()
    private val _isPlaying = MutableStateFlow(false); val isPlaying = _isPlaying.asStateFlow()
    private val _selectedClipId = MutableStateFlow<String?>(null); val selectedClipId = _selectedClipId.asStateFlow()
    private val _selectedAudioId = MutableStateFlow<String?>(null); val selectedAudioId = _selectedAudioId.asStateFlow()
    private val _selectedTextId = MutableStateFlow<String?>(null); val selectedTextId = _selectedTextId.asStateFlow()
    private val _activeFilter = MutableStateFlow("Nenhum"); val activeFilter = _activeFilter.asStateFlow()
    private val _speedRampProfile = MutableStateFlow("Normal"); val speedRampProfile = _speedRampProfile.asStateFlow()
    private val _useChromaKey = MutableStateFlow(false); val useChromaKey = _useChromaKey.asStateFlow()
    private val _chromaColorHex = MutableStateFlow("#00FF00"); val chromaColorHex = _chromaColorHex.asStateFlow()
    private val _useAiBackgroundRemover = MutableStateFlow(false); val useAiBackgroundRemover = _useAiBackgroundRemover.asStateFlow()
    private val _useAiNoiseReduction = MutableStateFlow(false); val useAiNoiseReduction = _useAiNoiseReduction.asStateFlow()
    private val _portraitStyle = MutableStateFlow("Nenhum"); val portraitStyle = _portraitStyle.asStateFlow()
    private val _selectedAiVoice = MutableStateFlow("Narrador de Cinema"); val selectedAiVoice = _selectedAiVoice.asStateFlow()
    private val _isGeneratingAi = MutableStateFlow(false); val isGeneratingAi = _isGeneratingAi.asStateFlow()
    private val _aiTopicInput = MutableStateFlow(""); val aiTopicInput = _aiTopicInput.asStateFlow()
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: ""); val customApiKey = _customApiKey.asStateFlow()
    private val _exportProgress = MutableStateFlow(-1); val exportProgress = _exportProgress.asStateFlow()
    private val _exportedVideos = MutableStateFlow<List<String>>(emptyList()); val exportedVideos = _exportedVideos.asStateFlow()
    private var bootstrapped = prefs.getBoolean("starter_project_bootstrapped", false)
    val sampleVideoThemes = listOf("Abertura Neo" to "#1A1F32", "Cena Natureza" to "#10B981", "Fracao Urbana" to "#FE0979", "Encerramento" to "#4F46E5")

    init { viewModelScope.launch { repository.allProjects.collect { list -> if (list.isEmpty() && !bootstrapped) { bootstrapped = true; prefs.edit().putBoolean("starter_project_bootstrapped", true).apply(); createStarterProject("Meu Video Autoral 1") } else if (list.isNotEmpty() && _activeProject.value == null) { bootstrapped = true; prefs.edit().putBoolean("starter_project_bootstrapped", true).apply(); loadProject(list.first()) } } } }

    fun selectTab(v:String){_currentTab.value=v}; fun setAiTopic(v:String){_aiTopicInput.value=v}; fun togglePlay(){_isPlaying.value=!_isPlaying.value}
    fun seekTo(v:Float){_currentTime.value=v.coerceIn(0f,getMaxTimelineDuration())}; fun getMaxTimelineDuration():Float=maxOf(_activeClips.value.maxOfOrNull{it.startSec+it.durationSec}?:12f,_activeAudio.value.maxOfOrNull{it.startSec+it.durationSec}?:0f,_activeTexts.value.maxOfOrNull{it.startSec+it.durationSec}?:0f).coerceAtLeast(6f)
    fun selectClip(v:String?){_selectedClipId.value=v}; fun selectAudio(v:String?){_selectedAudioId.value=v}; fun selectText(v:String?){_selectedTextId.value=v}
    fun setAiBackgroundRemover(v:Boolean){_useAiBackgroundRemover.value=v}; fun setAiNoiseReduction(v:Boolean){_useAiNoiseReduction.value=v}; fun setPortraitStyle(v:String){_portraitStyle.value=v}; fun setSelectedAiVoice(v:String){_selectedAiVoice.value=v}
    fun setFilter(v:String){_activeFilter.value=v;saveCurrentStateToDb()}; fun setChromaEnabled(v:Boolean){_useChromaKey.value=v;saveCurrentStateToDb()}; fun setChromaColor(v:String){_chromaColorHex.value=v;saveCurrentStateToDb()}
    fun splitSelectedOrActiveClip(){}; fun trimSelectedClip(isLeft:Boolean,delta:Float){}; fun deleteSelectedClip(){_selectedClipId.value?.let{_activeClips.value=_activeClips.value.filterNot{x->x.id==it};saveCurrentStateToDb()}}
    fun duplicateSelectedClip(){_activeClips.value.find{it.id==_selectedClipId.value}?.let{val n=it.copy(id=UUID.randomUUID().toString(),startSec=getMaxTimelineDuration());_activeClips.value=_activeClips.value+n;_selectedClipId.value=n.id;saveCurrentStateToDb()}}
    fun setSpeedRamp(v:String){_speedRampProfile.value=v;saveCurrentStateToDb()}
    fun addAudioTrack(n:String,isNarrator:Boolean){val a=AudioTimelineClip(UUID.randomUUID().toString(),n,_currentTime.value,5f,0.8f,isNarrator);_activeAudio.value=_activeAudio.value+a;_selectedAudioId.value=a.id;saveCurrentStateToDb()}
    fun removeSelectedAudio(){_selectedAudioId.value?.let{_activeAudio.value=_activeAudio.value.filterNot{x->x.id==it};_selectedAudioId.value=null;saveCurrentStateToDb()}}
    fun addTextCard(c:String){val t=TextTimelineClip(UUID.randomUUID().toString(),c,_currentTime.value,3f);_activeTexts.value=_activeTexts.value+t;_selectedTextId.value=t.id;saveCurrentStateToDb()}
    fun updateSelectedTextContent(v:String){_selectedTextId.value?.let{_activeTexts.value=_activeTexts.value.map{x->if(x.id==it)x.copy(text=v)else x};saveCurrentStateToDb()}}
    fun removeSelectedText(){_selectedTextId.value?.let{_activeTexts.value=_activeTexts.value.filterNot{x->x.id==it};_selectedTextId.value=null;saveCurrentStateToDb()}}
    fun updateAudioVolume(id:String,v:Float){_activeAudio.value=_activeAudio.value.map{if(it.id==id)it.copy(volume=v.coerceIn(0f,1f))else it};saveCurrentStateToDb()}; fun updateSelectedAudioVolume(v:Float){_selectedAudioId.value?.let{updateAudioVolume(it,v)}}
    fun convertTextToAiVoiceover(){_activeTexts.value.find{it.id==_selectedTextId.value}?.let{addAudioTrack("${_selectedAiVoice.value}: ${it.text.take(20)}",true)}}
    fun triggerAutoCaps(){_activeTexts.value=_activeTexts.value+_activeClips.value.map{TextTimelineClip(UUID.randomUUID().toString(),it.sourceName,it.startSec,it.durationSec)};saveCurrentStateToDb()}
    fun generateAiContentToTimeline(){val topic=_aiTopicInput.value.trim();if(topic.isBlank()||_isGeneratingAi.value)return;_isGeneratingAi.value=true;viewModelScope.launch{try{_activeTexts.value=_activeTexts.value+repository.draftAiScriptToTimeline(topic,getMaxTimelineDuration().toInt().coerceIn(10,60),"short video",_customApiKey.value);saveCurrentStateToDb();_aiTopicInput.value=""}finally{_isGeneratingAi.value=false}}}
    fun loadProject(p:VideoProject){_activeProject.value=p;_activeFilter.value=p.activeFilter;_speedRampProfile.value=p.speedRampProfile;_useChromaKey.value=p.useChromaKey;_chromaColorHex.value=p.chromaBgColor;_activeClips.value=repository.deserializeClips(p.clipsJson);_activeAudio.value=repository.deserializeAudio(p.audioJson);_activeTexts.value=repository.deserializeTexts(p.textsJson);_selectedClipId.value=_activeClips.value.firstOrNull()?.id;_currentTime.value=0f}
    fun createStarterProject(title:String){viewModelScope.launch{val c=listOf(VideoTimelineClip("sc1","Intro",0f,4f),VideoTimelineClip("sc2","Cena",4f,4f));val a=listOf(AudioTimelineClip("aud1","Audio",0f,8f));val t=listOf(TextTimelineClip("txt1","Bem-vindo",0f,3f));val id=repository.saveProject(VideoProject(title=title,clipsJson=repository.serializeClips(c),audioJson=repository.serializeAudio(a),textsJson=repository.serializeTexts(t)));repository.getProjectById(id)?.let{loadProject(it)}}}
    fun saveCurrentStateToDb(){val p=_activeProject.value?:return;viewModelScope.launch(Dispatchers.IO){repository.saveProject(p.copy(activeFilter=_activeFilter.value,speedRampProfile=_speedRampProfile.value,useChromaKey=_useChromaKey.value,chromaBgColor=_chromaColorHex.value,clipsJson=repository.serializeClips(_activeClips.value),audioJson=repository.serializeAudio(_activeAudio.value),textsJson=repository.serializeTexts(_activeTexts.value)))} }
    fun deleteProjectItem(p:VideoProject){viewModelScope.launch{repository.deleteProject(p);if(_activeProject.value?.id==p.id){_activeProject.value=null;_activeClips.value=emptyList();_activeAudio.value=emptyList();_activeTexts.value=emptyList()}}}
    fun triggerExportSimulation(r:String,fps:Int){if(_exportProgress.value!=-1)return;viewModelScope.launch{_exportProgress.value=100;_exportedVideos.value=listOf("${_activeProject.value?.title?:"Projeto"} ($r @ $fps FPS).mp4")+_exportedVideos.value;_exportProgress.value=-1}}
    fun clearMockGallery(){_exportedVideos.value=emptyList()}; fun saveApiKey(k:String){_customApiKey.value=k;prefs.edit().putString("custom_api_key",k).apply()}
}
class ChatViewModelFactory(private val application:Application,private val repository:ChatRepository):ViewModelProvider.Factory{override fun<T:ViewModel>create(modelClass:Class<T>):T{if(modelClass.isAssignableFrom(ChatViewModel::class.java)){@Suppress("UNCHECKED_CAST") return ChatViewModel(application,repository) as T};throw IllegalArgumentException("Unknown ViewModel class")}}
