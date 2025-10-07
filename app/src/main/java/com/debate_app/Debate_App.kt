package com.debate_app

import android.app.Application
import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.flow.Flow

class MyApplication : Application() {
    lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        val firestore = Firebase.firestore
        firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
        val auth = FirebaseAuth.getInstance()
        val roomDb = AppDatabase.getDatabase(this) // Use the companion object's method
        repository = AppRepository(auth, firestore, roomDb, this)
    }
}


@Dao
interface DebateDao {
    @Query("SELECT * FROM debates WHERE id = :id")
    fun getDebate(id: String): Flow<LocalDebate?>

    @Query("SELECT * FROM debates WHERE id = :id")
    suspend fun getDebateSuspend(id: String): LocalDebate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debate: LocalDebate)

    @Query("SELECT * FROM debates WHERE date = :date ORDER BY id DESC")
    fun getDebatesForDate(date: String): Flow<List<LocalDebate>>

    @Query("SELECT * FROM debates ORDER BY date DESC, id DESC")
    fun getAllDebates(): Flow<List<LocalDebate>>

    @Query("DELETE FROM debates")
    suspend fun deleteAll()
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE debate_id = :debateId ORDER BY created_at DESC")
    fun getCommentsForDebate(debateId: String): Flow<List<LocalComment>>

    @Query("SELECT * FROM comments")
    fun getComments(): Flow<List<LocalComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: LocalComment)

    @Query("SELECT * FROM comments WHERE user_id = :userId")
    suspend fun getCommentsByUser(userId: String): List<LocalComment>

    @Query("UPDATE comments SET comment_id = :newId WHERE comment_id = :oldId")
    suspend fun updateCommentId(oldId: String, newId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAll()
}

@Dao
interface PendingVoteDao {
    @Query("SELECT * FROM pending_votes WHERE synced = 0")
    suspend fun getPendingVotes(): List<PendingVote>

    @Insert
    suspend fun insert(vote: PendingVote)

    @Update
    suspend fun update(vote: PendingVote)

    @Query("DELETE FROM pending_votes")
    suspend fun deleteAll()

    @Query("DELETE FROM pending_votes WHERE id = :voteId")
    suspend fun deleteById(voteId: String)
}

@Dao
interface PendingCommentDao {
    @Query("SELECT * FROM pending_comments WHERE synced = 0")
    suspend fun getPendingComments(): List<PendingComment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingComment: PendingComment)

    @Update
    suspend fun update(pendingComment: PendingComment)

    @Delete
    suspend fun delete(pendingComment: PendingComment)

    @Query("DELETE FROM pending_comments")  // Changed from @Delete to @Query
    suspend fun deleteAll()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUser(id: String): Flow<LocalUser?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: LocalUser)

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE user_id = :userId")
    fun getAchievements(userId: String): Flow<List<LocalAchievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: LocalAchievement)

    @Query("SELECT * FROM achievements WHERE name = :name AND user_id = :userId")
    suspend fun getAchievement(name: String, userId: String): LocalAchievement?

    @Query("DELETE FROM achievements")
    suspend fun deleteAll()
}

@Dao
interface UserVoteDao {
    @Query("SELECT * FROM user_votes WHERE user_id = :userId AND debate_id = :debateId")
    suspend fun getVoteForDebate(userId: String, debateId: String): UserVote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vote: UserVote)

    @Query("DELETE FROM user_votes WHERE user_id = :userId AND debate_id = :debateId")
    suspend fun deleteVote(userId: String, debateId: String)

    @Query("DELETE FROM user_votes")
    suspend fun deleteAll()
}


@Database(
    entities = [PendingVote::class, LocalUser::class, LocalDebate::class, LocalComment::class, LocalAchievement::class, PendingComment::class , UserVote::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingVoteDao(): PendingVoteDao
    abstract fun userDao(): UserDao
    abstract fun debateDao(): DebateDao
    abstract fun commentDao(): CommentDao
    abstract fun achievementDao(): AchievementDao
    abstract fun pendingCommentDao(): PendingCommentDao

    abstract fun userVoteDao(): UserVoteDao // Add new DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }
        }
    }
}