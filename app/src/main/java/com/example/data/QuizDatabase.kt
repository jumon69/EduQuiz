package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String, // Cyberpunk colors, e.g., "#00F0FF" (Neon Cyan), "#FF007F" (Neon Pink)
    val iconName: String, // e.g., "Terminal", "Science", "School", "Calculate"
    val pdfHash: String? = null
)

@Entity(
    tableName = "mcqs",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId")]
)
data class McqEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val explanation: String
)

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val scorePercentage: Int, // 0 - 100
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wrong_answers",
    foreignKeys = [
        ForeignKey(
            entity = McqEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class WrongAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_guard_status")
data class FocusGuardStatusEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val dailyTarget: Int = 20,
    val correctCount: Int = 0,
    val isLocked: Boolean = true
)

@Dao
interface QuizDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: Int)

    @Query("SELECT * FROM mcqs WHERE subjectId = :subjectId")
    suspend fun getMcqsForSubject(subjectId: Int): List<McqEntity>

    @Query("SELECT * FROM mcqs WHERE subjectId = :subjectId")
    fun getMcqsForSubjectFlow(subjectId: Int): Flow<List<McqEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcq(mcq: McqEntity): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMcqs(mcqs: List<McqEntity>)

    @Query("DELETE FROM mcqs WHERE subjectId = :subjectId")
    suspend fun clearMcqsForSubject(subjectId: Int)

    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    fun getAllQuizHistory(): Flow<List<QuizHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizHistory(history: QuizHistoryEntity)

    // Focus Guard Lock Status
    @Query("SELECT * FROM focus_guard_status WHERE date = :date")
    fun getFocusGuardStatusFlow(date: String): Flow<FocusGuardStatusEntity?>

    @Query("SELECT * FROM focus_guard_status WHERE date = :date")
    suspend fun getFocusGuardStatus(date: String): FocusGuardStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFocusGuardStatus(status: FocusGuardStatusEntity)

    // Wrong Answers / Review Bank Status
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWrongAnswer(wrongAnswer: WrongAnswerEntity)

    @Query("SELECT mcqs.* FROM mcqs INNER JOIN wrong_answers ON mcqs.id = wrong_answers.questionId ORDER BY wrong_answers.timestamp DESC")
    fun getWrongAnswersFlow(): Flow<List<McqEntity>>

    @Query("DELETE FROM wrong_answers WHERE questionId = :questionId")
    suspend fun deleteWrongAnswer(questionId: Int)

    @Query("SELECT * FROM subjects WHERE pdfHash = :hash LIMIT 1")
    suspend fun getSubjectByPdfHash(hash: String): SubjectEntity?
}

@Database(entities = [SubjectEntity::class, McqEntity::class, QuizHistoryEntity::class, WrongAnswerEntity::class, FocusGuardStatusEntity::class], version = 3, exportSchema = false)
abstract class QuizDatabase : RoomDatabase() {
    abstract val quizDao: QuizDao

    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getDatabase(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "eduquiz_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class QuizRepository(private val quizDao: QuizDao) {
    val allSubjects: Flow<List<SubjectEntity>> = quizDao.getAllSubjects()
    val allHistory: Flow<List<QuizHistoryEntity>> = quizDao.getAllQuizHistory()
    val allWrongAnswers: Flow<List<McqEntity>> = quizDao.getWrongAnswersFlow()

    fun getFocusGuardStatusFlow(date: String): Flow<FocusGuardStatusEntity?> {
        return quizDao.getFocusGuardStatusFlow(date)
    }

    suspend fun getFocusGuardStatus(date: String): FocusGuardStatusEntity? {
        return quizDao.getFocusGuardStatus(date)
    }

    suspend fun updateFocusGuardStatus(status: FocusGuardStatusEntity) {
        quizDao.insertOrUpdateFocusGuardStatus(status)
    }

    suspend fun insertWrongAnswer(questionId: Int) {
        quizDao.insertWrongAnswer(WrongAnswerEntity(questionId = questionId))
    }

    suspend fun deleteWrongAnswer(questionId: Int) {
        quizDao.deleteWrongAnswer(questionId)
    }

    suspend fun insertSubject(name: String, colorHex: String, iconName: String, pdfHash: String? = null): Int {
        return quizDao.insertSubject(SubjectEntity(name = name, colorHex = colorHex, iconName = iconName, pdfHash = pdfHash)).toInt()
    }

    suspend fun getSubjectByPdfHash(hash: String): SubjectEntity? {
        return quizDao.getSubjectByPdfHash(hash)
    }

    suspend fun deleteSubject(subjectId: Int) {
        quizDao.deleteSubject(subjectId)
    }

    suspend fun getMcqsForSubject(subjectId: Int): List<McqEntity> {
        return quizDao.getMcqsForSubject(subjectId)
    }

    fun getMcqsForSubjectFlow(subjectId: Int): Flow<List<McqEntity>> {
        return quizDao.getMcqsForSubjectFlow(subjectId)
    }

    suspend fun insertHistory(subjectName: String, totalQuestions: Int, correctAnswers: Int, scorePercentage: Int) {
        quizDao.insertQuizHistory(
            QuizHistoryEntity(
                subjectName = subjectName,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                scorePercentage = scorePercentage
            )
        )
    }

    // Batch insert 100 MCQs at a time to prevent SQLite locking during heavy processing
    suspend fun batchInsertMcqs(subjectId: Int, mcqsList: List<McqEntity>) {
        val chunks = mcqsList.chunked(100)
        for (chunk in chunks) {
            quizDao.insertMcqs(chunk.map { it.copy(subjectId = subjectId) })
        }
    }
}
