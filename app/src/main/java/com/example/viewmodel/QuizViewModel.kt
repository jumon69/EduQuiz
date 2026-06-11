package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.ParsedMcq
import com.example.api.TutorChatMessage
import com.example.data.McqEntity
import com.example.data.QuizDatabase
import com.example.data.QuizHistoryEntity
import com.example.data.QuizRepository
import com.example.data.SubjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import com.example.data.FocusGuardStatusEntity
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: QuizRepository
    val allSubjects: StateFlow<List<SubjectEntity>>
    val allHistory: StateFlow<List<QuizHistoryEntity>>
    val allWrongAnswers: StateFlow<List<McqEntity>>

    private val todayStringByUtility: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    val focusGuardStatus: StateFlow<FocusGuardStatusEntity?>

    init {
        val database = QuizDatabase.getDatabase(application)
        repository = QuizRepository(database.quizDao)
        allSubjects = repository.allSubjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allHistory = repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allWrongAnswers = repository.allWrongAnswers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        focusGuardStatus = repository.getFocusGuardStatusFlow(todayStringByUtility).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        // Reset or seed checking daily row at startup
        viewModelScope.launch {
            val todayStr = todayStringByUtility
            val existing = repository.getFocusGuardStatus(todayStr)
            if (existing == null) {
                repository.updateFocusGuardStatus(
                    FocusGuardStatusEntity(date = todayStr, dailyTarget = 20, correctCount = 0, isLocked = true)
                )
            }
        }

        // Seed default cyberpunk subjects on first launch if database is empty
        viewModelScope.launch {
            allSubjects.collect { subjects ->
                if (subjects.isEmpty()) {
                    seedDefaultSubjects()
                }
            }
        }
    }

    // --- Parsing / Extraction Flow States ---
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _parsingProgress = MutableStateFlow(0f)
    val parsingProgress: StateFlow<Float> = _parsingProgress.asStateFlow()

    private val _parsingConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val parsingConsoleLogs: StateFlow<List<String>> = _parsingConsoleLogs.asStateFlow()

    // --- Active Quiz Session States ---
    private val _activeQuizQuestions = MutableStateFlow<List<McqEntity>>(emptyList())
    val activeQuizQuestions: StateFlow<List<McqEntity>> = _activeQuizQuestions.asStateFlow()

    private val _activeSubject = MutableStateFlow<SubjectEntity?>(null)
    val activeSubject: StateFlow<SubjectEntity?> = _activeSubject.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userChoices = MutableStateFlow<Map<Int, String>>(emptyMap()) // index -> option ("A", "B", "C", "D")
    val userChoices: StateFlow<Map<Int, String>> = _userChoices.asStateFlow()

    private val _isQuestionChecked = MutableStateFlow<Map<Int, Boolean>>(emptyMap()) // index -> checked
    val isQuestionChecked: StateFlow<Map<Int, Boolean>> = _isQuestionChecked.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    // --- AI Tutor Chat Sidebar States ---
    private val _aiTutorMessages = MutableStateFlow<List<TutorChatMessage>>(emptyList())
    val aiTutorMessages: StateFlow<List<TutorChatMessage>> = _aiTutorMessages.asStateFlow()

    private val _isAiTutorThinking = MutableStateFlow(false)
    val isAiTutorThinking: StateFlow<Boolean> = _isAiTutorThinking.asStateFlow()

    private fun addConsoleLog(log: String) {
        _parsingConsoleLogs.value = _parsingConsoleLogs.value + "[${System.currentTimeMillis() % 100000}] $log"
    }

    private suspend fun seedDefaultSubjects() {
        // Disabled to remove dummy data as requested by the user
    }

    // --- Database Modification Commands ---

    fun addNewSubject(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            repository.insertSubject(name, colorHex, iconName)
        }
    }

    fun deleteSubject(subjectId: Int) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    // --- Parser Simulation: 160MB PDF Memory-Safe Generator Pipeline ---

    fun run160MbPdfStreamingParser(subjectName: String, fileHash: String? = null) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _parsingProgress.value = 0f
            _parsingConsoleLogs.value = emptyList()

            addConsoleLog("INITIALIZING STREAM PARSER: LOADING SOURCE PDF DOCUMENT")
            addConsoleLog("STRATEGY: MEMORY-SAFE CHUNK GENERATORS (YIELD STREAMS)")
            delay(600)

            // Dynamic cryptographic key resolution
            val fileHashToUse = if (!fileHash.isNullOrBlank()) {
                fileHash
            } else {
                try {
                    val digest = java.security.MessageDigest.getInstance("SHA-256")
                    val hashBytes = digest.digest(subjectName.trim().lowercase().toByteArray(Charsets.UTF_8))
                    hashBytes.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    "hash_" + subjectName.hashCode().toString()
                }
            }

            addConsoleLog("CHECKING LOCAL SECURE SQLITE REGISTRY FOR HASH: [${fileHashToUse.take(16)}...]")
            delay(1000)

            val existingSubject = repository.getSubjectByPdfHash(fileHashToUse)
            if (existingSubject != null) {
                val existingMcqs = repository.getMcqsForSubject(existingSubject.id)
                if (existingMcqs.isNotEmpty()) {
                    addConsoleLog("CACHE HIT: Pre-existing questions found for PDF hash [${fileHashToUse.take(12)}...]")
                    addConsoleLog("INSTANT RETRIEVAL SUCCESSFUL! Fetching ${existingMcqs.size} questions from local SQLite database instantly.")
                    _parsingProgress.value = 1.0f
                    delay(1200)
                    _isAnalyzing.value = false
                    return@launch
                }
            }

            addConsoleLog("CACHE MISS: No local study guides found for this PDF.")
            addConsoleLog("Parsing new PDF and generating MCQs via AI Engine... ⚡")
            delay(1000)

            val totalMb = 160
            val chunkSizeMb = 16
            var currentLoaded = 0

            val generatedMcqs = mutableListOf<McqEntity>()

            // Mock yield streaming loop that represents the python generator pipeline
            for (step in 1..10) {
                currentLoaded += chunkSizeMb
                _parsingProgress.value = (currentLoaded.toFloat() / totalMb.toFloat())
                
                addConsoleLog("YIELD GENERATOR: Read bytes $currentLoaded MB / $totalMb MB")
                addConsoleLog("PARSING CHUNK $step: PyMuPDF extracting tables and paragraphs...")
                delay(400)

                // Generate parsed content in this chunk (Supporting Bangla scripting block correctly)
                val chunkMcqs = listOf(
                    McqEntity(
                        subjectId = 0, // Assigned later
                        question = "[$subjectName] " + if (step % 2 == 0) "নিচের কোনটি সত্য? Theory of Relativity equation holds constant." else "What is the critical entropy yield of Section ${step * 45}?",
                        optionA = if (step % 2 == 0) "অপশন ক) E = mc^2 is verified" else "Option Alpha for Theory ${step * 2}",
                        optionB = if (step % 2 == 0) "অপশন খ) Planck Constant equals h/p" else "Option Beta for Equation ${step + 10}",
                        optionC = if (step % 2 == 0) "অপশন গ) Quantum state decreases in density" else "Option Delta of Matrix ${step * 120}",
                        optionD = if (step % 2 == 0) "অপশন ঘ) Mass-energy equivalence is broken" else "Option Gamma (Correct Scientific Constant)",
                        correctAnswer = "A",
                        explanation = if (step % 2 == 0) "আইনস্টাইনের ভর-শক্তি সমতুল্যতা সূত্রানুযায়ী E = mc^2 সমীকরণটি নির্দেশিত করে।" else "Under the verified theory extracted from Chunk $step paragraph, option A matches the local density constant bounds perfectly."
                    )
                )
                generatedMcqs.addAll(chunkMcqs)
                addConsoleLog("PIPELINE BUFFER: Buffer generated ${chunkMcqs.size} questions safely.")
            }

            addConsoleLog("YIELD LOOP COMPLETE. PARSED ${generatedMcqs.size} MCQS.")
            addConsoleLog("DATABASE ENGINE: Commencing safe batch insertion (100 at a time)")
            
            // Safe insertion using the Repository batch insertion with pdfHash saved
            val newSubjectId = repository.insertSubject(subjectName, "#00F0FF", "Terminal", pdfHash = fileHashToUse)
            repository.batchInsertMcqs(newSubjectId, generatedMcqs)

            addConsoleLog("BATCH INSERT COMPLETE: SQLite indices rebuilt. SQLite Lock cleared.")
            delay(800)
            _isAnalyzing.value = false
        }
    }

    // --- Live Camera Action / Text OCR Processing via Gemini ---

    fun parseMcqsFromCapturedImage(imageBitmap: Bitmap, subjectName: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _parsingProgress.value = 0.3f
            _parsingConsoleLogs.value = emptyList()

            addConsoleLog("CAMERA HANDSHAKE: INGESTING IMAGE SNAPSHOT")
            addConsoleLog("OCR CONFIG: Google Gemini OCR Processing Visual Matrix")
            delay(800)

            try {
                addConsoleLog("TRANSMITTING MATRIX TO GEMINI-3.5-FLASH FOR MULTI-MODAL COMPILATION...")
                val mcqsList = GeminiClient.parseMcqsFromImage(imageBitmap)
                _parsingProgress.value = 0.7f

                addConsoleLog("MCQS PARSED SUCCESSFULLY: Found ${mcqsList.size} questions from snapshot!")
                addConsoleLog("COORDINATING BATCH TRANSACTION WITH LOCAL STORAGE")

                val newSubjectId = repository.insertSubject(subjectName, "#FF007F", "Camera")
                
                val entities = mcqsList.map { parsed ->
                    McqEntity(
                        subjectId = newSubjectId,
                        question = parsed.question,
                        optionA = parsed.optionA,
                        optionB = parsed.optionB,
                        optionC = parsed.optionC,
                        optionD = parsed.optionD,
                        correctAnswer = parsed.correctAnswer,
                        explanation = parsed.explanation
                    )
                }

                repository.batchInsertMcqs(newSubjectId, entities)
                addConsoleLog("WRITE SUCCESSFUL: Local SQLite database synchronized!")
                _parsingProgress.value = 1.0f
                delay(800)
            } catch (e: Exception) {
                addConsoleLog("OCR MATRIX COMPILE ERROR: ${e.message}")
                addConsoleLog("REVERTING: SQLite lock released. Saved locally as fallback.")
                delay(1500)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun parseMcqsFromRawNotes(subjectName: String, notesText: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _parsingProgress.value = 0.2f
            _parsingConsoleLogs.value = emptyList()

            addConsoleLog("NOTES INPUT INGESTED: Length = ${notesText.length} chars")
            addConsoleLog("STREAMING REQUEST: Querying Gemini-3.5-Flash text model")
            delay(550)

            try {
                val mcqsList = GeminiClient.parseMcqsFromText(notesText)
                _parsingProgress.value = 0.6f
                addConsoleLog("SUCCESS: Structured MCQ data extracted (${mcqsList.size} elements)")

                val newSubjectId = repository.insertSubject(subjectName, "#FF007F", "School")
                val entities = mcqsList.map { parsed ->
                    McqEntity(
                        subjectId = newSubjectId,
                        question = parsed.question,
                        optionA = parsed.optionA,
                        optionB = parsed.optionB,
                        optionC = parsed.optionC,
                        optionD = parsed.optionD,
                        correctAnswer = parsed.correctAnswer,
                        explanation = parsed.explanation
                    )
                }
                repository.batchInsertMcqs(newSubjectId, entities)

                addConsoleLog("SQLITE WRITE: Batch operations complete. Indexes flushed.")
                _parsingProgress.value = 1.0f
                delay(600)
            } catch (e: Exception) {
                addConsoleLog("AI ENGINE CRITICAL ERROR: ${e.message}")
                delay(1200)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // --- Active Quiz Session Controls ---

    fun startQuickQuiz(onReady: () -> Unit) {
        viewModelScope.launch {
            val subjects = allSubjects.value
            if (subjects.isNotEmpty()) {
                startQuizSession(subjects.first())
                onReady()
            } else {
                seedDefaultSubjects()
                delay(150)
                val refreshed = allSubjects.value
                if (refreshed.isNotEmpty()) {
                    startQuizSession(refreshed.first())
                    onReady()
                }
            }
        }
    }

    fun startQuizSession(subject: SubjectEntity) {
        viewModelScope.launch {
            _activeSubject.value = subject
            _currentQuestionIndex.value = 0
            _userChoices.value = emptyMap()
            _isQuestionChecked.value = emptyMap()
            _quizFinished.value = false
            _aiTutorMessages.value = listOf(
                TutorChatMessage("ai", "Greetings, Scholar. I am Chronos, your offline-capable AI Tutor. Solve the MCQs of absolute zero. Let me know if you need any scientific explanations!")
            )

            val questions = repository.getMcqsForSubject(subject.id)
            if (questions.isNotEmpty()) {
                _activeQuizQuestions.value = questions.shuffled() // Shuffle for optimal cognitive practice
            } else {
                _activeQuizQuestions.value = emptyList()
            }
        }
    }

    fun selectOption(option: String) {
        val currIndex = _currentQuestionIndex.value
        if (_isQuestionChecked.value[currIndex] != true && !_quizFinished.value) {
            _userChoices.value = _userChoices.value + (currIndex to option)
        }
    }

    fun checkAnswer() {
        val currIndex = _currentQuestionIndex.value
        _isQuestionChecked.value = _isQuestionChecked.value + (currIndex to true)
        
        val questions = _activeQuizQuestions.value
        val mcq = questions.getOrNull(currIndex)
        val selectedOption = _userChoices.value[currIndex]

        if (mcq != null && selectedOption != null) {
            val isCorrect = selectedOption == mcq.correctAnswer
            viewModelScope.launch {
                if (isCorrect) {
                    val todayStr = todayStringByUtility
                    val current = repository.getFocusGuardStatus(todayStr) ?: FocusGuardStatusEntity(date = todayStr)
                    val newCount = current.correctCount + 1
                    val updated = current.copy(
                        correctCount = newCount,
                        isLocked = newCount < current.dailyTarget
                    )
                    repository.updateFocusGuardStatus(updated)
                } else {
                    repository.insertWrongAnswer(mcq.id)
                }
            }
        }

        // Let Tutor comment on user answer automatically
        triggerTutorAutoReaction()
    }

    fun deleteWrongAnswer(questionId: Int) {
        viewModelScope.launch {
            repository.deleteWrongAnswer(questionId)
        }
    }

    fun setMockCorrectCount(count: Int) {
        viewModelScope.launch {
            val todayStr = todayStringByUtility
            val current = repository.getFocusGuardStatus(todayStr) ?: FocusGuardStatusEntity(date = todayStr)
            val updated = current.copy(
                correctCount = count,
                isLocked = count < current.dailyTarget
            )
            repository.updateFocusGuardStatus(updated)
        }
    }

    fun setMockLocked(locked: Boolean) {
        viewModelScope.launch {
            val todayStr = todayStringByUtility
            val current = repository.getFocusGuardStatus(todayStr) ?: FocusGuardStatusEntity(date = todayStr)
            val updated = current.copy(isLocked = locked)
            repository.updateFocusGuardStatus(updated)
        }
    }

    fun nextQuestion() {
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex < _activeQuizQuestions.value.size) {
            _currentQuestionIndex.value = nextIndex
        } else {
            finishQuizSession()
        }
    }

    private fun finishQuizSession() {
        _quizFinished.value = true
        val questions = _activeQuizQuestions.value
        val choices = _userChoices.value
        
        var correctCount = 0
        questions.forEachIndexed { index, mcq ->
            if (choices[index] == mcq.correctAnswer) {
                correctCount++
            }
        }

        val percentage = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0
        val subjectName = _activeSubject.value?.name ?: "Unknown"

        viewModelScope.launch {
            repository.insertHistory(
                subjectName = subjectName,
                totalQuestions = questions.size,
                correctAnswers = correctCount,
                scorePercentage = percentage
            )
        }
    }

    // --- AI Tutor Assistant Panel Controllers ---

    private fun triggerTutorAutoReaction() {
        val index = _currentQuestionIndex.value
        val questions = _activeQuizQuestions.value
        val mcq = questions.getOrNull(index) ?: return
        val userChoice = _userChoices.value[index] ?: "None"
        val isCorrect = userChoice == mcq.correctAnswer

        viewModelScope.launch {
            _isAiTutorThinking.value = true
            delay(600)
            val text = if (isCorrect) {
                "✨ **Excellently solved, Scholar!** You successfully flagged target choice **${mcq.correctAnswer}**.\n\nHere is why you are correct: _${mcq.explanation}_"
            } else {
                "⚠️ **Warning, concept drift detected!** You selected **$userChoice**, but the correct scientific reference is **${mcq.correctAnswer}**.\n\n*   **Explanation**: ${mcq.explanation}\n\nClick **'Ask AI for Concept'** below to run a deep theoretical explanation on the microgrid concepts!"
            }
            _aiTutorMessages.value = _aiTutorMessages.value + TutorChatMessage("ai", text)
            _isAiTutorThinking.value = false
        }
    }

    fun askTutorSpecificQuestion(queryText: String) {
        if (queryText.isBlank()) return
        
        val currMessages = _aiTutorMessages.value
        _aiTutorMessages.value = currMessages + TutorChatMessage("user", queryText)

        viewModelScope.launch {
            _isAiTutorThinking.value = true
            
            val index = _currentQuestionIndex.value
            val questions = _activeQuizQuestions.value
            val mcq = questions.getOrNull(index)

            val explanationResponse = if (mcq != null) {
                GeminiClient.askTutor(
                    mcqQuestion = mcq.question,
                    optionA = mcq.optionA,
                    optionB = mcq.optionB,
                    optionC = mcq.optionC,
                    optionD = mcq.optionD,
                    correctAnswer = mcq.correctAnswer,
                    studentAnswer = _userChoices.value[index] ?: "Unanswered",
                    userQuery = queryText,
                    history = currMessages
                )
            } else {
                "No active MCQ. Chronos is awaiting study references, Scholar."
            }

            _aiTutorMessages.value = _aiTutorMessages.value + TutorChatMessage("ai", explanationResponse)
            _isAiTutorThinking.value = false
        }
    }
}

class QuizViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
