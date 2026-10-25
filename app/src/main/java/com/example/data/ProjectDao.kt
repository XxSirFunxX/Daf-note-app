package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    // --- PROJECTS ---
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    // --- FOLDERS ---
    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    // --- NOTE DEFINITIONS ---
    @Query("SELECT * FROM note_definitions WHERE isHidden = 0 ORDER BY createdAt ASC")
    fun getAllActiveNoteDefinitions(): Flow<List<NoteDefinitionEntity>>

    @Query("SELECT * FROM note_definitions ORDER BY createdAt ASC")
    fun getAllNoteDefinitions(): Flow<List<NoteDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteDefinition(noteDef: NoteDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteDefinitions(noteDefs: List<NoteDefinitionEntity>)

    @Update
    suspend fun updateNoteDefinition(noteDef: NoteDefinitionEntity)

    @Query("DELETE FROM note_definitions WHERE id = :id")
    suspend fun deleteNoteDefinition(id: String)

    @Query("UPDATE note_definitions SET isHidden = :hidden WHERE id = :id")
    suspend fun setNoteDefinitionHidden(id: String, hidden: Boolean)

    @Query("UPDATE note_definitions SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategoryNotes(oldCategoryId: String, newCategoryId: String)

    // --- CUSTOM SYMBOLS ---
    @Query("SELECT * FROM custom_symbols WHERE isHidden = 0 ORDER BY createdAt DESC")
    fun getAllActiveCustomSymbols(): Flow<List<CustomSymbolEntity>>

    @Query("SELECT * FROM custom_symbols ORDER BY createdAt DESC")
    fun getAllCustomSymbols(): Flow<List<CustomSymbolEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomSymbol(symbol: CustomSymbolEntity)

    @Update
    suspend fun updateCustomSymbol(symbol: CustomSymbolEntity)

    @Query("DELETE FROM custom_symbols WHERE id = :id")
    suspend fun deleteCustomSymbol(id: String)

    @Query("UPDATE custom_symbols SET isHidden = :hidden WHERE id = :id")
    suspend fun setCustomSymbolHidden(id: String, hidden: Boolean)

    @Query("UPDATE custom_symbols SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategoryCustomSymbols(oldCategoryId: String, newCategoryId: String)

    // --- HELP TOPICS ---
    @Query("SELECT * FROM help_topics ORDER BY orderIndex ASC")
    fun getAllHelpTopics(): Flow<List<HelpTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpTopic(topic: HelpTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpTopics(topics: List<HelpTopicEntity>)

    @Update
    suspend fun updateHelpTopic(topic: HelpTopicEntity)

    @Query("DELETE FROM help_topics WHERE id = :id")
    suspend fun deleteHelpTopic(id: String)

    @Query("DELETE FROM help_topics")
    suspend fun deleteAllHelpTopics()
}
