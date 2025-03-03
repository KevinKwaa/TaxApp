package com.example.taxapp.chatbot.database

import android.content.Context
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taxapp.chatbot.MessageType
import kotlinx.coroutines.flow.Flow
import java.util.Date

private const val TAG = "ChatDBEntities"
/**
 * Entity representing a chat message in the database
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val messageType: String, // "USER" or "BOT"
    val timestamp: Long,
    val userId: String      // User ID to associate messages with specific users
)

/**
 * Type converters for Room database
 */
class Converters {
    @TypeConverter
    fun fromMessageType(messageType: MessageType): String {
        return messageType.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return try {
            MessageType.valueOf(value)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting MessageType: $value", e)
            MessageType.BOT // Default fallback
        }
    }
}

/**
 * Data Access Object for chat messages
 */
// In ChatDatabase.kt, update the ChatMessageDao interface:

@Dao
interface ChatMessageDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity)

    // Get all messages for a specific user
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllMessages(userId: String): Flow<List<ChatMessageEntity>>

    // Get recent messages for a specific user
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(userId: String, limit: Int): Flow<List<ChatMessageEntity>>

    // Delete all messages for a specific user
    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    suspend fun deleteAllMessages(userId: String)

    // Delete a specific message for a specific user
    @Query("DELETE FROM chat_messages WHERE id = :messageId AND userId = :userId")
    suspend fun deleteMessage(messageId: String, userId: String)

    // Count messages for a specific user
    @Query("SELECT COUNT(*) FROM chat_messages WHERE userId = :userId")
    suspend fun getMessageCount(userId: String): Int
}

/**
 * Room database for chat messages
 */
@Database(entities = [ChatMessageEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        private const val TAG = "ChatDatabase"

        @Volatile
        private var INSTANCE: ChatDatabase? = null

        // Define migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add userId column to existing table
                    database.execSQL("ALTER TABLE chat_messages ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
                    Log.d(TAG, "Migration 1->2 successful: Added userId column with default value")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 1->2", e)
                }
            }
        }

        fun getInstance(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ChatDatabase::class.java,
                        "chat_database"
                    )
                        //.addMigration(MIGRATION_1_2)  // Add the migration
                        .fallbackToDestructiveMigration()  // As a last resort
                        .build()

                    INSTANCE = instance
                    Log.d(TAG, "ChatDatabase instance created successfully")
                    return instance
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating ChatDatabase instance", e)
                    throw e
                }
            }
        }
    }
}