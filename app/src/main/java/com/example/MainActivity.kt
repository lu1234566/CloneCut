package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.VideoProject
import com.example.data.repository.ChatRepository
import com.example.data.repository.VideoTimelineClip
import com.example.data.repository.AudioTimelineClip
import com.example.data.repository.TextTimelineClip
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory

// CapCut Branding Color Scheme (Midnight Slate, Pulse Pink & Electro Blue)
private val CapCutDarkBg = Color(0xFF090B10)
private val CapCutSurfaceBg = Color(0xFF10131E)
private val CapCutCardBg = Color(0xFF181C2E)
private val CapCutActiveColor = Color(0xFF00F2FE) // Vapor Cyber Blue
private val CapCutPinkAccent = Color(0xFFFE0979) // Electric Pink
private val AccentMint = Color(0xFF10B981) // Friendly Mint
private val AccentGold = Color(0xFFFBBF24) // Gold Yellow
private val AccentCherry = Color(0xFFEF4444) // Intense Red
private val TextHighContrast = Color(0xFFF1F5F9)
private val TextSubdued = Color(0xFF94A3B8)

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: ChatRepository
    private lateinit var viewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Persistent Room database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "capcut_clone_db_v5"
        )
        .fallbackToDestructiveMigration()
        .build()

        repository = ChatRepository(database.projectDao())

        viewModel = ViewModelProvider(
            this,
            ChatViewModelFactory(application, repository)
        )[ChatViewModel::class.java]

        setContent {
            CapCutDarkTheme {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CapCutDarkTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = CapCutActiveColor,
        secondary = CapCutPinkAccent,
        tertiary = AccentMint,
        background = CapCutDarkBg,
        surface = CapCutSurfaceBg,
        surfaceVariant = CapCutCardBg,
        onPrimary = Color.Black,
        onBackground = TextHighContrast,
        onSurface = TextHighContrast
    )
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: ChatViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(CapCutActiveColor, CapCutPinkAccent)
                                    )
                                )
                        ) {
                            Text(
                                text = "CC",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CapCut Clone Studio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextHighContrast
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CapCutSurfaceBg
                ),
                actions = {
                    if (activeProject != null) {
                        Text(
                            text = "${activeProject?.resolution} | ${activeProject?.fps}FPS",
                            color = CapCutActiveColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .border(1.dp, CapCutActiveColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CapCutSurfaceBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == "editor",
                    onClick = { viewModel.selectTab("editor") },
                    label = { Text("Editor", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Área de Edição") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CapCutActiveColor,
                        indicatorColor = CapCutActiveColor,
                        unselectedIconColor = TextSubdued,
                        unselectedTextColor = TextSubdued
                    ),
                    modifier = Modifier.testTag("nav_editor")
                )
                NavigationBarItem(
                    selected = currentTab == "projects",
                    onClick = { viewModel.selectTab("projects") },
                    label = { Text("Projetos", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Galeria de Projetos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CapCutActiveColor,
                        indicatorColor = CapCutActiveColor,
                        unselectedIconColor = TextSubdued,
                        unselectedTextColor = TextSubdued
                    ),
                    modifier = Modifier.testTag("nav_projects")
                )
                NavigationBarItem(
                    selected = currentTab == "gallery",
                    onClick = { viewModel.selectTab("gallery") },
                    label = { Text("Galeria", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Vídeos Exportados") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CapCutActiveColor,
                        indicatorColor = CapCutActiveColor,
                        unselectedIconColor = TextSubdued,
                        unselectedTextColor = TextSubdued
                    ),
                    modifier = Modifier.testTag("nav_gallery")
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { viewModel.selectTab("settings") },
                    label = { Text("Ajustes", fontSize = 11.sp) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Configurações da Chave") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CapCutActiveColor,
                        indicatorColor = CapCutActiveColor,
                        unselectedIconColor = TextSubdued,
                        unselectedTextColor = TextSubdued
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        containerColor = CapCutDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "editor" -> EditorWorkspaceScreen(viewModel = viewModel)
                "projects" -> ProjectManagerScreen(viewModel = viewModel)
                "gallery" -> ExportedGalleryScreen(viewModel = viewModel)
                "settings" -> SettingsScreen(viewModel = viewModel)
            }

            // Exporting progress overlay card popup
            if (exportProgress != -1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .width(280.dp)
                            .padding(16.dp)
                            .border(1.dp, CapCutActiveColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = { exportProgress / 100f },
                                color = CapCutPinkAccent,
                                trackColor = CapCutActiveColor.copy(alpha = 0.2f),
                                strokeWidth = 6.dp,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Renderizando CapCut...",
                                fontWeight = FontWeight.Bold,
                                color = TextHighContrast,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mesclando trilhas de vídeo, áudio, filtros e legendas de I.A. ($exportProgress%)",
                                color = TextSubdued,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorWorkspaceScreen(viewModel: ChatViewModel) {
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val activeClips by viewModel.activeClips.collectAsStateWithLifecycle()
    val activeAudio by viewModel.activeAudio.collectAsStateWithLifecycle()
    val activeTexts by viewModel.activeTexts.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val selectedClipId by viewModel.selectedClipId.collectAsStateWithLifecycle()
    val selectedAudioId by viewModel.selectedAudioId.collectAsStateWithLifecycle()
    val selectedTextId by viewModel.selectedTextId.collectAsStateWithLifecycle()

    val activeFilter by viewModel.activeFilter.collectAsStateWithLifecycle()
    val speedRampProfile by viewModel.speedRampProfile.collectAsStateWithLifecycle()
    val useChromaKey by viewModel.useChromaKey.collectAsStateWithLifecycle()
    val chromaColorHex by viewModel.chromaColorHex.collectAsStateWithLifecycle()

    val useAiBackgroundRemover by viewModel.useAiBackgroundRemover.collectAsStateWithLifecycle()
    val useAiNoiseReduction by viewModel.useAiNoiseReduction.collectAsStateWithLifecycle()
    val portraitStyle by viewModel.portraitStyle.collectAsStateWithLifecycle()
    val selectedAiVoice by viewModel.selectedAiVoice.collectAsStateWithLifecycle()

    val isGeneratingAi by viewModel.isGeneratingAi.collectAsStateWithLifecycle()

    if (activeProject == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSubdued,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nenhum Projeto Ativo",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextHighContrast
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Vá para a aba 'Projetos' para inaugurar ou selecionar uma nova linha editorial e começar a fatiar!",
                color = TextSubdued,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { viewModel.createStarterProject("Meu Novo Edite Master") },
                colors = ButtonDefaults.buttonColors(containerColor = CapCutActiveColor, contentColor = Color.Black)
            ) {
                Text("Criar Projeto de Exemplo", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    val maxDuration = viewModel.getMaxTimelineDuration()

    // Find active elements at current time for live rendering!
    val activeClipAtTime = activeClips.find { currentTime >= it.startSec && currentTime < (it.startSec + it.durationSec) }
    val activeTextAtTime = activeTexts.find { currentTime >= it.startSec && currentTime < (it.startSec + it.durationSec) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        // 1. HIGH FIDELITY VIDEO PLAYER (16:9)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .border(2.dp, CapCutSurfaceBg)
                .drawBehind {
                    // Draw Chroma Key background if active
                    if (useChromaKey) {
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(chromaColorHex))
                        } catch (e: Exception) {
                            Color.Green
                        }
                        drawRect(color = parsedColor)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Simulated Video Frame Material Render
            if (activeClipAtTime != null) {
                val themeColorHex = viewModel.sampleVideoThemes.find { it.first == activeClipAtTime.sourceName }?.second
                    ?: "#1E293B"
                val themeColor = try {
                    Color(android.graphics.Color.parseColor(themeColorHex))
                } catch (e: Exception) {
                    Color.DarkGray
                }

                // Render video color box representing scene content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(themeColor.copy(alpha = 0.8f), themeColor)
                            )
                        )
                )

                // Render color filter overlay live
                when (activeFilter) {
                    "Cinematográfico" -> {
                        // Moody high contrast cinematic bar filters
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFEF4444).copy(alpha = 0.08f)) // slight red grade
                        )
                        // Cinemascope letterboxes
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Box(modifier = Modifier.fillMaxWidth().height(18.dp).background(Color.Black))
                            Box(modifier = Modifier.fillMaxWidth().height(18.dp).background(Color.Black))
                        }
                    }
                    "Cyberpunk" -> {
                        // Electro neon purples and overlay lines
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFA855F7).copy(alpha = 0.25f)) // vibrant purple overlay
                        )
                    }
                    "Vintage 90s" -> {
                        // Warm sepia grain tint
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFD97706).copy(alpha = 0.2f)) // warm amber tint
                        )
                    }
                    "Preto & Branco" -> {
                        // Monochrome grayscale overlay desaturator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFFFFF).copy(alpha = 0.15f))
                        )
                    }
                }

                // Render Portrait Style overlays live
                when (portraitStyle) {
                    "Cyberpunk I.A." -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, CapCutPinkAccent, RoundedCornerShape(2.dp))
                                .background(Brush.radialGradient(colors = listOf(Color.Transparent, Color(0xFF1E112A).copy(alpha = 0.5f))))
                        )
                    }
                    "Animação 3D" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF60A5FA).copy(alpha = 0.15f))
                        )
                    }
                    "Filtro Retro I.A." -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f))
                                .background(Color(0xFF78350F).copy(alpha = 0.15f))
                        )
                    }
                    "Desenho Mangá" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        )
                    }
                }

                // Render vector icon graphics animation relative to speed (with AI Recorte/Cutout tracking)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = if (useAiBackgroundRemover) {
                                Modifier
                                    .padding(8.dp)
                                    .border(2.dp, CapCutActiveColor, CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(16.dp)
                            } else {
                                Modifier
                            }
                        ) {
                            Icon(
                                imageVector = when (activeClipAtTime.sourceName) {
                                    "Natureza Intro" -> Icons.Default.Add
                                    "Ação Cortada" -> Icons.Default.PlayArrow
                                    "Cidade Broll" -> Icons.Default.Share
                                    else -> Icons.Default.Star
                                },
                                contentDescription = null,
                                tint = if (useAiBackgroundRemover) CapCutActiveColor else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(if (useAiBackgroundRemover) 44.dp else 54.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (useAiBackgroundRemover) {
                                "✂️ [Recorte I.A. Inteligente]"
                            } else {
                                "[Cena Ativa: ${activeClipAtTime.sourceName} - ${String.format("%.1f", activeClipAtTime.speedMultiplier)}x]"
                            },
                            color = if (useAiBackgroundRemover) CapCutActiveColor else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (portraitStyle != "Nenhum") {
                            Text(
                                text = "✨ Fator Filtro: $portraitStyle",
                                color = CapCutPinkAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // AI Noise reduction indicator wave overlay
                if (useAiNoiseReduction) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                .border(1.dp, AccentMint.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentMint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "🔇 Redução de Ruído I.A. Ativa",
                                color = AccentMint,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Fim do Vídeo (Tela Preta)",
                    color = TextSubdued,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Subtitle overlay track rendering (Dynamic styles!)
            if (activeTextAtTime != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val sColor = try {
                        Color(android.graphics.Color.parseColor(activeTextAtTime.colorHex))
                    } catch (e: Exception) {
                        Color.White
                    }

                    val style = when (activeTextAtTime.styleAnim) {
                        "Neon Sparkle", "Neon" -> TextStyle(
                            color = sColor,
                            fontSize = activeTextAtTime.fontSizeSp.sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = CapCutActiveColor,
                                offset = Offset(0f, 0f),
                                blurRadius = 14f
                            )
                        )
                        "Glitch" -> TextStyle(
                            color = sColor,
                            fontSize = activeTextAtTime.fontSizeSp.sp,
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(
                                color = CapCutPinkAccent,
                                offset = Offset(3f, 2f),
                                blurRadius = 2f
                            )
                        )
                        else -> TextStyle(
                            color = sColor,
                            fontSize = activeTextAtTime.fontSizeSp.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = activeTextAtTime.text,
                        style = style,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("subtitle_canvas_text")
                    )
                }
            }
        }

        // 2. PLAYBACK TIMESET AND NAVIGATION SLIDER
        Surface(
            color = CapCutSurfaceBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("00:%04.1f", currentTime),
                        color = CapCutActiveColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.seekTo(0.0f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", tint = TextHighContrast)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { viewModel.togglePlay() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(CapCutActiveColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Play",
                                tint = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = String.format("00:%04.1f", maxDuration),
                        color = TextSubdued,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                // Interactive scrubber bar
                Slider(
                    value = currentTime,
                    onValueChange = { viewModel.seekTo(it) },
                    valueRange = 0f..maxDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = CapCutActiveColor,
                        activeTrackColor = CapCutActiveColor,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timeline_timeline_scrubber")
                )
            }
        }

        // 3. MULTI-TRACK TIMELINE CHANNELS
        Text(
            "LINHA DE TEMPO MULTI-FAIXA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSubdued,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // TRACK 1: VIDEO CLIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(60.dp)) {
                        Text("Vídeo 🎞️", color = TextSubdued, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        activeClips.forEach { clip ->
                            val isSelected = clip.id == selectedClipId
                            val isClipPlayingLocal = currentTime >= clip.startSec && currentTime < (clip.startSec + clip.durationSec)
                            val bgSeed = viewModel.sampleVideoThemes.find { it.first == clip.sourceName }?.second ?: "#1E293B"
                            val parsedBgColor = try {
                                Color(android.graphics.Color.parseColor(bgSeed))
                            } catch (e: Exception) {
                                Color.Gray
                            }

                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .width((clip.durationSec * 25).dp) // Scaled width
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(parsedBgColor)
                                    .border(
                                        width = if (isSelected) 2.dp else if (isClipPlayingLocal) 1.dp else 0.dp,
                                        color = if (isSelected) CapCutPinkAccent else CapCutActiveColor.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        viewModel.seekTo(clip.startSec)
                                        // Highlight clip edit selection
                                        viewModel.saveCurrentStateToDb() // Flush
                                        viewModel.selectClip(clip.id)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = clip.sourceName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${String.format("%.1f", clip.durationSec)}s (${String.format("%.1f", clip.speedMultiplier)}x)",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // TRACK 2: AUDIO CLIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(60.dp)) {
                        Text("Áudio 🎵", color = TextSubdued, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (activeAudio.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(" Sem áudio ativo. Adicione música ou voz abaixo.", color = TextSubdued.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            activeAudio.forEach { aud ->
                                val isSelected = aud.id == selectedAudioId
                                val isAudPlayingLocal = currentTime >= aud.startSec && currentTime < (aud.startSec + aud.durationSec)

                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .width((aud.durationSec * 25).dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (aud.isNarratorVoice) Color(0xFF0F766E) else Color(0xFF15803D))
                                        .border(
                                            width = if (isSelected) 2.dp else if (isAudPlayingLocal) 1.dp else 0.dp,
                                            color = if (isSelected) AccentGold else CapCutActiveColor.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            viewModel.seekTo(aud.startSec)
                                            viewModel.selectAudio(aud.id)
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (aud.isNarratorVoice) Icons.Default.Add else Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = aud.audioName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // TRACK 3: SUBTITLE / TEXT TIMELINE CAPSULES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(60.dp)) {
                        Text("Texto 💬", color = TextSubdued, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    if (activeTexts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(" Sem legenda. Use a IA para gerar automáticas!", color = TextSubdued.copy(alpha = 0.5f), fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) {
                            activeTexts.forEach { txt ->
                                val isSelected = txt.id == selectedTextId
                                val isTxtPlayingLocal = currentTime >= txt.startSec && currentTime < (txt.startSec + txt.durationSec)

                                Box(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .width((txt.durationSec * 25).dp)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF7C2D12)) // Dark orange
                                        .border(
                                            width = if (isSelected) 1.5.dp else if (isTxtPlayingLocal) 1.dp else 0.dp,
                                            color = if (isSelected) CapCutActiveColor else CapCutPinkAccent.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            viewModel.seekTo(txt.startSec)
                                            viewModel.selectText(txt.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = txt.text,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. POWERFUL TOOLS ACCORDION BAR
        Text(
            "ESTAÇÃO DE TRABALHO & EFEITOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSubdued,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
        )

        var expandedSection by remember { mutableStateOf("split_tools") }

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            // A. DIVIRIR & APARAR (CLIP EDIT DECK)
            SectionHeader(
                title = "Cortar & Splicing de Clipes",
                icon = Icons.Default.Star,
                isSelected = expandedSection == "split_tools",
                onClick = { expandedSection = "split_tools" }
            )
            if (expandedSection == "split_tools") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { viewModel.splitSelectedOrActiveClip() },
                                colors = ButtonDefaults.buttonColors(containerColor = CapCutPinkAccent),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("split_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Corte (Split)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.duplicateSelectedClip() },
                                colors = ButtonDefaults.buttonColors(containerColor = CapCutCardBg, contentColor = TextHighContrast),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Duplicar", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.deleteSelectedClip() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Excluir", fontSize = 11.sp)
                                }
                            }
                        }

                        // Trim Microadjusters
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Micro-Ajuste de Bordas (Trim)", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { viewModel.trimSelectedClip(isLeft = true, delta = -0.5f) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CapCutActiveColor)
                            ) {
                                Text("Aparar Esq (-0.5s)", fontSize = 10.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.trimSelectedClip(isLeft = false, delta = 0.5f) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CapCutActiveColor)
                            ) {
                                Text("Esticar Dir (+0.5s)", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // B. VELOCIDADE & CURVAS (SPEED RAMP CHIPS)
            SectionHeader(
                title = "Rampa de Velocidade & Curvas",
                icon = Icons.Default.Share,
                isSelected = expandedSection == "speed_tools",
                onClick = { expandedSection = "speed_tools" }
            )
            if (expandedSection == "speed_tools") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Perfil de Rampa Curva:", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Normal", "Montagem Jump", "Hero Flash", "Bullet Time").forEach { r ->
                                val selected = speedRampProfile == r
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) CapCutActiveColor else CapCutCardBg)
                                        .border(1.dp, if (selected) CapCutActiveColor else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setSpeedRamp(r) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = r,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.Black else TextHighContrast,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = when (r) {
                                                "Montagem Jump" -> "2.0x"
                                                "Hero Flash" -> "3.0x"
                                                "Bullet Time" -> "0.2x"
                                                else -> "1.0x"
                                            },
                                            fontSize = 8.sp,
                                            color = if (selected) Color.Black.copy(alpha = 0.7f) else TextSubdued
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. FILTROS DE COR, RECORTE I.A. & CHROMA KEY (VISUAL EFFECTS DECK)
            SectionHeader(
                title = "I.A. Recorte, Retratos & Chroma Key",
                icon = Icons.Default.Menu,
                isSelected = expandedSection == "filter_tools",
                onClick = { expandedSection = "filter_tools" }
            )
            if (expandedSection == "filter_tools") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. RECORTE INTELIGENTE I.A. (AI BACKGROUND REMOVER)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Recorte Inteligente I.A.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CapCutActiveColor)
                                Text("Isola o sujeito principal instantaneamente sem fundo verde", fontSize = 9.sp, color = TextSubdued)
                            }
                            Switch(
                                checked = useAiBackgroundRemover,
                                onCheckedChange = { viewModel.setAiBackgroundRemover(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CapCutActiveColor, checkedTrackColor = CapCutActiveColor.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. ESTILO DE RETRATO I.A.
                        Text("Estilos de Retrato I.A. (Portrait Face Tracking):", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Nenhum", "Cyberpunk I.A.", "Animação 3D", "Filtro Retro I.A.", "Desenho Mangá").forEach { style ->
                                val active = portraitStyle == style
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) CapCutPinkAccent else CapCutCardBg)
                                        .clickable { viewModel.setPortraitStyle(style) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = style.replace(" I.A.", ""),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. COLOR FILTERS
                        Text("Filtros de Cor Clássicos:", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Nenhum", "Cinematográfico", "Cyberpunk", "Vintage 90s", "Preto & Branco").forEach { f ->
                                val active = activeFilter == f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) CapCutPinkAccent else CapCutCardBg)
                                        .clickable { viewModel.setFilter(f) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = f,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. CHROMA KEY GREEN SCREEN
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Chroma Key Customizado", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextHighContrast)
                                Text("Substitui uma cor específica nas cenas", fontSize = 9.sp, color = TextSubdued)
                            }
                            Switch(
                                checked = useChromaKey,
                                onCheckedChange = { viewModel.setChromaEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentMint, checkedTrackColor = AccentMint.copy(alpha = 0.4f))
                            )
                        }

                        if (useChromaKey) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf("#00FF00" to "Verde", "#0000FF" to "Azul", "#FF00FF" to "Rosa").forEach { pair ->
                                    val isCol = chromaColorHex == pair.first
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isCol) CapCutActiveColor else CapCutCardBg)
                                            .clickable { viewModel.setChromaColor(pair.first) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(pair.second, fontSize = 10.sp, color = if (isCol) Color.Black else TextHighContrast, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // D. VOZ, LEGENDA & PROCESSAMENTO DE ÁUDIO I.A. (AI SHORTS CHANNELS)
            SectionHeader(
                title = "I.A. Legendas, Vozes (TTS) & Redução de Ruído",
                icon = Icons.Default.Refresh,
                isSelected = expandedSection == "ai_tools",
                onClick = { expandedSection = "ai_tools" }
            )
            if (expandedSection == "ai_tools") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. CHANNELS VOLUME & AUDIO BOOST DECK
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CapCutPinkAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Controle de Volume & Amplificador I.A. (Até 400%)", fontSize = 11.sp, color = TextHighContrast, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (activeAudio.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CapCutCardBg)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("Nenhuma trilha sonora ativa neste projeto.", fontSize = 10.sp, color = TextSubdued)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { viewModel.addAudioTrack("🎵 Trilha Sonora de Exemplo", false) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CapCutActiveColor, contentColor = Color.Black),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Adicionar Áudio para Ajustar/Amplificar", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                activeAudio.forEach { aud ->
                                    val isSelected = aud.id == selectedAudioId
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) CapCutCardBg.copy(alpha = 0.9f) else CapCutCardBg),
                                        border = if (isSelected) BorderStroke(1.dp, CapCutActiveColor) else null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(aud.audioName, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "Tempo: ${String.format("%.1f", aud.startSec)}s - duração: ${String.format("%.1f", aud.durationSec)}s",
                                                            fontSize = 8.sp,
                                                            color = TextSubdued
                                                        )
                                                        if (aud.volume > 1.0f) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(CapCutPinkAccent, RoundedCornerShape(3.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            ) {
                                                                Text("🚀 I.A. AMPLIFICADO", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${(aud.volume * 100).toInt()}% Vol",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (aud.volume > 1.0f) CapCutPinkAccent else CapCutActiveColor
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Slider(
                                                value = aud.volume,
                                                onValueChange = { viewModel.updateAudioVolume(aud.id, it) },
                                                valueRange = 0f..4f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = if (aud.volume > 1.0f) CapCutPinkAccent else CapCutActiveColor,
                                                    activeTrackColor = if (aud.volume > 1.0f) CapCutPinkAccent else CapCutActiveColor,
                                                    inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                                                ),
                                                modifier = Modifier.height(18.dp)
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                listOf(
                                                    "Mute" to 0.0f,
                                                    "Normal (100%)" to 1.0f,
                                                    "Boost (200%)" to 2.0f,
                                                    "Max (400%)" to 4.0f
                                                ).forEach { (label, value) ->
                                                    val isPresetActive = Math.abs(aud.volume - value) < 0.15f
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isPresetActive) (if (value > 1.0f) CapCutPinkAccent else CapCutActiveColor) else Color.Black.copy(alpha = 0.4f))
                                                            .clickable { viewModel.updateAudioVolume(aud.id, value) }
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isPresetActive) (if (value > 1.0f) Color.White else Color.Black) else Color.White
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. REDUÇÃO DE RUÍDO COM I.A.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Redução de Ruído Inteligente I.A.", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentMint)
                                Text("Suprime zumbidos, ecos e ruídos de fundo automaticamente", fontSize = 9.sp, color = TextSubdued)
                            }
                            Switch(
                                checked = useAiNoiseReduction,
                                onCheckedChange = { viewModel.setAiNoiseReduction(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AccentMint, checkedTrackColor = AccentMint.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. TEXT-TO-SPEECH (TEXTO PARA VOZ I.A. - TTS)
                        Text("Texto para Voz I.A. (Narradores Personalizados):", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Narrador de Cinema", "Google Latina", "Voz de Desenho", "Fofa Kawaii").forEach { voice ->
                                val active = selectedAiVoice == voice
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) CapCutPinkAccent else CapCutCardBg)
                                        .clickable { viewModel.setSelectedAiVoice(voice) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = voice,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val hasSelectedText = selectedTextId != null
                        Button(
                            onClick = { viewModel.convertTextToAiVoiceover() },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutPinkAccent),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasSelectedText
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasSelectedText) "🗣️ Gerar Locução de Voz para Bloco Ativo" else "⚠️ Selecione um Bloco de Texto na Linha",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. GENERATE SCRIPT TO TIMELINE
                        Text("Script Inteligente para Vídeo (Gerador de Roteiro - Gemini I.A.):", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        val aiTopicInput by viewModel.aiTopicInput.collectAsStateWithLifecycle()
                        OutlinedTextField(
                            value = aiTopicInput,
                            onValueChange = { viewModel.setAiTopic(it) },
                            placeholder = { Text("Tema do Roteiro (ex: Curiosidades do Mar, Oferta Relâmpago)", color = TextSubdued, fontSize = 11.sp) },
                            textStyle = TextStyle(color = TextHighContrast, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CapCutActiveColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.generateAiContentToTimeline() },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutActiveColor, contentColor = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_ai_button"),
                            enabled = !isGeneratingAi && aiTopicInput.isNotBlank()
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gerando Subtítulos da I.A...", fontSize = 11.sp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Injetar Roteiro e Fazer Vozes no Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = CapCutCardBg)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. AUTO LEGENDAS
                        Text("Extrair Legendas Automáticas do Áudio:", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { viewModel.triggerAutoCaps() },
                            colors = ButtonDefaults.buttonColors(containerColor = CapCutCardBg, contentColor = CapCutActiveColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CapCutActiveColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .testTag("auto_caps_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar Legendas com Trilha Sonora", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // E. EXPORTAR VÍDEO (RENDER PANEL)
            SectionHeader(
                title = "Exportador de Vídeo 4K",
                icon = Icons.Default.Check,
                isSelected = expandedSection == "export_tools",
                onClick = { expandedSection = "export_tools" }
            )
            if (expandedSection == "export_tools") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    var selectedQuality by remember { mutableStateOf("1080p") }
                    var selectedFps by remember { mutableStateOf(30) }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Resolução:", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("720p", "1080p", "4K").forEach { q ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedQuality == q) CaptionColor(q) else CapCutCardBg)
                                        .clickable { selectedQuality = q }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(q, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedQuality == q) Color.Black else TextHighContrast)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Taxa de Quadros (FPS):", fontSize = 11.sp, color = TextSubdued, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(30, 60).forEach { f ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedFps == f) CapCutActiveColor else CapCutCardBg)
                                        .clickable { selectedFps = f }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${f} FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedFps == f) Color.Black else TextHighContrast)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.triggerExportSimulation(selectedQuality, selectedFps) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentMint, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_trigger_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Renderizar e Exportar como .MP4", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) CapCutActiveColor else TextSubdued,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CapCutActiveColor else TextHighContrast,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isSelected) Icons.Default.Menu else Icons.Default.Add,
            contentDescription = null,
            tint = TextSubdued,
            modifier = Modifier.size(16.dp)
        )
    }
    Divider(color = CapCutSurfaceBg.copy(alpha = 0.5f))
}

@Composable
fun CaptionColor(text: String): Color {
    return when (text) {
        "4K" -> AccentGold
        else -> CapCutActiveColor
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectManagerScreen(viewModel: ChatViewModel) {
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    var newProjectTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "CRIAR NOVO PROJETO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CapCutActiveColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newProjectTitle,
                    onValueChange = { newProjectTitle = it },
                    placeholder = { Text("Nome do Projeto (ex: Comercial Pizzaria)", color = TextSubdued, fontSize = 13.sp) },
                    textStyle = TextStyle(color = TextHighContrast, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CapCutActiveColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (newProjectTitle.isNotBlank()) {
                            viewModel.createStarterProject(newProjectTitle)
                            newProjectTitle = ""
                            viewModel.selectTab("editor")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CapCutActiveColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().testTag("add_project_btn"),
                    enabled = newProjectTitle.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inaugurar Painel no Linha de Tempo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Text(
            "MEU ARQUIVO LOCAL DE PROJETOS (${savedProjects.size})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSubdued,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (savedProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhum rascunho de vídeo salvo no banco SQLite.", color = TextSubdued, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedProjects) { proj ->
                    val isActive = proj.id == activeProject?.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) CapCutCardBg else CapCutSurfaceBg
                        ),
                        border = if (isActive) BorderStroke(1.5.dp, CapCutActiveColor) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadProject(proj)
                                viewModel.selectTab("editor")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    proj.title,
                                    fontWeight = FontWeight.Bold,
                                    color = TextHighContrast,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Resolução: ${proj.resolution}  |  Quadros: ${proj.fps} FPS",
                                    fontSize = 11.sp,
                                    color = TextSubdued
                                )
                            }
                            IconButton(onClick = { viewModel.deleteProjectItem(proj) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar Rascunho", tint = AccentCherry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportedGalleryScreen(viewModel: ChatViewModel) {
    val exportedVideos by viewModel.exportedVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "GALERIA DE RENDERIZADOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CapCutActiveColor
            )

            if (exportedVideos.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearMockGallery() }
                ) {
                    Text("Limpar Tudo", color = AccentCherry, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (exportedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(64.dp), tint = TextSubdued.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Nenhum arquivo .MP4 finalizado ainda.",
                        color = TextSubdued,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Finalize edições e exporte na aba Editor!",
                        color = TextSubdued.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exportedVideos) { video ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentMint.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentMint)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = video,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextHighContrast,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(180.dp)
                                    )
                                    Text(
                                        text = "Estado: Salvo e Compilado (Pronto para Postar)",
                                        fontSize = 10.sp,
                                        color = TextSubdued
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Simulação: Compartilhando no TikTok!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartilhar", tint = CapCutActiveColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: ChatViewModel) {
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    var inputKey by remember(customApiKey) { mutableStateOf(customApiKey) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "CHAVE GEMINI API (OPCIONAL)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CapCutActiveColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Insira sua API Key pessoal para permitir que a inteligência artificial faça chamadas reais do Gemini para expandir os roteiros com títulos dinâmicos e cronograma.",
                    fontSize = 12.sp,
                    color = TextSubdued,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("Chave AI Studio...", color = TextSubdued, fontSize = 13.sp) },
                    textStyle = TextStyle(color = TextHighContrast, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CapCutActiveColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.saveApiKey(inputKey)
                        Toast.makeText(context, "Chave de API salva localmente!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CapCutActiveColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Chave", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "AJUSTES DO PREVIEW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSubdued,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CapCutSurfaceBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Proporção 9:16 (TikTok)", fontSize = 13.sp, color = TextHighContrast)
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(checkedThumbColor = CapCutActiveColor)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aceleração por Hardware (GPU)", fontSize = 13.sp, color = TextHighContrast)
                    Switch(
                        checked = true,
                        onCheckedChange = null,
                        enabled = false,
                        colors = SwitchDefaults.colors(checkedThumbColor = CapCutActiveColor)
                    )
                }
            }
        }
    }
}
