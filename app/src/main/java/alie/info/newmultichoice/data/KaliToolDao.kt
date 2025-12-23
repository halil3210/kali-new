package alie.info.newmultichoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KaliToolDao {
    @Query("SELECT * FROM kali_tools ORDER BY name ASC")
    fun getAllTools(): Flow<List<KaliTool>>
    
    @Query("SELECT * FROM kali_tools WHERE id = :toolId")
    suspend fun getToolById(toolId: Int): KaliTool?
    
    @Query("SELECT * FROM kali_tools WHERE category = :category ORDER BY name ASC")
    fun getToolsByCategory(category: String): Flow<List<KaliTool>>
    
    @Query("SELECT DISTINCT category FROM kali_tools ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: KaliTool)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<KaliTool>)
    
    @Query("DELETE FROM kali_tools")
    suspend fun deleteAllTools()
}

