package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val durationSeconds: Int = 12,
    val resolution: String = "1085p", // e.g., 1080p, 4K
    val fps: Int = 30,
    val activeFilter: String = "Nenhum", // e.g., Vintage, Cyberpunk, Cinematic, B&W
    val useChromaKey: Boolean = false,
    val chromaBgColor: String = "#00FF00", // green screen
    val speedRampProfile: String = "Normal", // e.g. Montage, Hero, Bullet
    
    // JSON arrays of track models
    val clipsJson: String,  
    val audioJson: String,
    val textsJson: String,
    
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :projectId LIMIT 1")
    suspend fun getProjectById(projectId: Long): VideoProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects")
    suspend fun deleteAllProjects()
}

@Database(entities = [VideoProject::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): VideoProjectDao
}
