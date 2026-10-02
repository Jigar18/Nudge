package app.nudge.reminders.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class NudgeDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao

    companion object {
        fun create(context: Context): NudgeDatabase =
            Room.databaseBuilder(context, NudgeDatabase::class.java, "nudge.db").build()
    }
}
