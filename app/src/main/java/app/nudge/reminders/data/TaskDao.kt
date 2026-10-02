package app.nudge.reminders.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY dueAt ASC")
    fun observeActive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status != 'ACTIVE' ORDER BY completedAt DESC")
    fun observeFinished(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun activeTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun find(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Upsert
    suspend fun upsert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE status != 'ACTIVE'")
    suspend fun clearFinished()
}
