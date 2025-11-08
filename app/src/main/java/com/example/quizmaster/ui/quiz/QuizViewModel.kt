// File: app/src/main/java/com/example/quizmaster/ui/quiz/QuizViewModel.kt
package com.example.quizmaster.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizmaster.data.model.QuizQuestion
import com.example.quizmaster.data.QuizRepository
import com.example.quizmaster.data.model.LeaderboardEntry
import kotlinx.coroutines.Dispatchers // <-- IMPORT ADDED
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // <-- IMPORT ADDED

// Define the UI State data class
data class QuizUiState(
    val currentQuestion: QuizQuestion? = null,
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val timeLeftSeconds: Int = 0,
    val isQuizFinished: Boolean = false,
    val isScoreSubmitted: Boolean = false, // <-- Corrected: No duplicate
    val selectedAnswerIndex: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class QuizViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState(isLoading = true))
    val uiState: StateFlow<QuizUiState> = _uiState

    private var timerJob: Job? = null
    private val questionTimeLimit = 10

    init {
        loadQuiz()
    }

    // --- THIS IS THE CORRECT, MODIFIED loadQuiz FUNCTION ---
    private fun loadQuiz() {
        viewModelScope.launch {
            try {
                val questions = repository.getQuestionsForQuiz()
                _uiState.update { currentState ->
                    currentState.copy(
                        questions = questions,
                        currentQuestion = questions.firstOrNull(),
                        currentQuestionIndex = 0,
                        timeLeftSeconds = questionTimeLimit,
                        isLoading = false,
                        isQuizFinished = false,
                        score = 0,
                        isScoreSubmitted = false // <-- Reset flag is included
                    )
                }
                startTimer()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load quiz: ${e.message}") }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = questionTimeLimit
            while (timeLeft > 0) {
                _uiState.update { it.copy(timeLeftSeconds = timeLeft) }
                delay(1000)
                timeLeft--
            }
            _uiState.update { it.copy(timeLeftSeconds = 0) }
            checkAnswer(null) // Time's up
        }
    }

    fun onAnswerSelected(selectedIndex: Int) {
        if (_uiState.value.selectedAnswerIndex != null) return // Already answered
        timerJob?.cancel()
        _uiState.update { it.copy(selectedAnswerIndex = selectedIndex) }

        viewModelScope.launch {
            delay(500)
            checkAnswer(selectedIndex)
        }
    }

    private fun checkAnswer(selectedIndex: Int?) {
        val currentState = _uiState.value
        val correctIndex = currentState.currentQuestion?.correctOptionIndex
        var newScore = currentState.score
        if (selectedIndex == correctIndex) {
            newScore++
        }

        _uiState.update {
            it.copy(
                score = newScore,
                selectedAnswerIndex = selectedIndex ?: -1 // -1 if time ran out
            )
        }

        viewModelScope.launch {
            delay(1500) // Show correct/wrong answer
            nextQuestion()
        }
    }

    private fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuestionIndex + 1

        if (nextIndex < currentState.questions.size) {
            // --- More questions remain ---
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    currentQuestion = it.questions[nextIndex],
                    timeLeftSeconds = questionTimeLimit,
                    selectedAnswerIndex = null // Reset selected answer
                )
            }
            startTimer()
        } else {
            // --- Quiz is Finished ---
            _uiState.update {
                it.copy(
                    isQuizFinished = true,
                    timeLeftSeconds = 0
                )
            }
        }
    }

    // --- 1. MODIFY THIS FUNCTION ---
    fun submitFinalScore(username: String) {
        viewModelScope.launch {
            val entry = LeaderboardEntry(
                userId = "temp_user_id",
                username = username,
                score = _uiState.value.score,
                timestamp = System.currentTimeMillis()
            )

            // Run the network call on the IO thread
            withContext(Dispatchers.IO) {
                repository.submitScore(entry)
            }

            // Tell the UI that submission is complete!
            _uiState.update { it.copy(isScoreSubmitted = true) }
        }
    }

    // --- 2. ADD THIS NEW FUNCTION ---
    fun playAgain() {
        // Just call loadQuiz() again, it will reset everything
        loadQuiz()
    }

    // --- NO SECOND loadQuiz() FUNCTION ---
    // The changes were merged into the one at the top.
}