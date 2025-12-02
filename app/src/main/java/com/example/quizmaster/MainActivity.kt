// File: app/src/main/java/com/example/quizmaster/MainActivity.kt
package com.example.quizmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quizmaster.data.QuizRepository
import com.example.quizmaster.data.model.LeaderboardEntry
import com.example.quizmaster.ui.quiz.QuizUiState
import com.example.quizmaster.ui.quiz.QuizViewModel
import com.example.quizmaster.ui.theme.CorrectGreen
import com.example.quizmaster.ui.theme.IncorrectRed
import com.example.quizmaster.ui.theme.NeutralGray
import com.example.quizmaster.ui.theme.QuizMasterTheme

enum class AppScreen {
    Welcome,
    Quiz,
    Leaderboard,
    Answers
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizMasterTheme {
                // ViewModel factory so we can pass the repository
                val viewModelFactory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return QuizViewModel(QuizRepository()) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }

                val quizViewModel: QuizViewModel = viewModel(factory = viewModelFactory)

                // Simple navigation state
                var currentScreen by remember { mutableStateOf(AppScreen.Welcome) }

                when (currentScreen) {
                    AppScreen.Welcome -> {
                        WelcomeScreen(
                            onStartQuiz = { currentScreen = AppScreen.Quiz },
                            onViewLeaderboard = { currentScreen = AppScreen.Leaderboard }
                        )
                    }
                    AppScreen.Quiz -> {
                        QuizScreen(
                            viewModel = quizViewModel,
                            onViewLeaderboard = { currentScreen = AppScreen.Leaderboard },
                            onShowAnswers = { currentScreen = AppScreen.Answers }
                        )
                    }
                    AppScreen.Leaderboard -> {
                        LeaderboardScreen(
                            onBack = { currentScreen = AppScreen.Welcome }
                        )
                    }
                    AppScreen.Answers -> {
                        AnswerKeyScreen(
                            viewModel = quizViewModel,
                            onBack = { currentScreen = AppScreen.Quiz } // goes back to result screen
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- WELCOME SCREEN ----------------------

@Composable
fun WelcomeScreen(
    onStartQuiz: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "QuizMaster",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Welcome! Test your knowledge and see how you rank on the leaderboard.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStartQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Quiz", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onViewLeaderboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Leaderboard"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Leaderboard", fontSize = 16.sp)
            }
        }
    }
}

// ---------------------- QUIZ SCREEN ----------------------

@Composable
fun QuizScreen(
    viewModel: QuizViewModel,
    onViewLeaderboard: () -> Unit,
    onShowAnswers: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isQuizFinished -> {
                QuizFinishedScreen(
                    score = uiState.score,
                    totalQuestions = uiState.questions.size,
                    isScoreSubmitted = uiState.isScoreSubmitted,
                    onSubmit = { username ->
                        viewModel.submitFinalScore(username)
                    },
                    onPlayAgain = {
                        viewModel.playAgain()
                    },
                    onViewLeaderboard = {
                        onViewLeaderboard()
                    },
                    onShowAnswers = {
                        onShowAnswers()
                    }
                )
            }

            uiState.currentQuestion != null -> {
                QuizContent(
                    uiState = uiState,
                    onAnswerSelected = { index ->
                        viewModel.onAnswerSelected(index)
                    }
                )
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// --- Composable for the Active Quiz ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(
    uiState: QuizUiState,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: score + timer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Score: ${uiState.score}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Time: ${uiState.timeLeftSeconds}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (uiState.timeLeftSeconds <= 5)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }

        // Question Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.currentQuestion?.text ?: "Loading question...",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val hasAnswered = uiState.selectedAnswerIndex != null

        // Answer options
        uiState.currentQuestion?.options?.forEachIndexed { index, optionText ->
            val isSelected = uiState.selectedAnswerIndex == index
            val correctIndex = uiState.currentQuestion.correctOptionIndex

            val buttonColors = when {
                !hasAnswered -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
                isSelected && index == correctIndex -> ButtonDefaults.buttonColors(
                    containerColor = CorrectGreen
                )
                isSelected && index != correctIndex -> ButtonDefaults.buttonColors(
                    containerColor = IncorrectRed
                )
                index == correctIndex -> ButtonDefaults.buttonColors(
                    containerColor = CorrectGreen
                )
                else -> ButtonDefaults.buttonColors(
                    containerColor = NeutralGray
                )
            }

            Button(
                onClick = { onAnswerSelected(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(56.dp),
                colors = buttonColors,
                enabled = !hasAnswered,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(text = optionText, fontSize = 16.sp)
            }
        }
    }
}

// ---------------------- QUIZ FINISHED / THANK YOU ----------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizFinishedScreen(
    score: Int,
    totalQuestions: Int,
    isScoreSubmitted: Boolean,
    onSubmit: (String) -> Unit,
    onPlayAgain: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onShowAnswers: () -> Unit
) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Score card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Quiz Finished!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Your Score: $score / $totalQuestions",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        if (!isScoreSubmitted) {
            // STATE 1: Show submit form
            Text(
                "Enter your name for the leaderboard:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Your Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { if (username.isNotBlank()) onSubmit(username) },
                enabled = username.isNotBlank(),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit to Leaderboard", fontSize = 16.sp)
            }
        } else {
            // STATE 2: Thank you + actions
            Text(
                "Thank you! Your score has been submitted.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "You can view the leaderboard to see how you rank.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onShowAnswers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show Answers", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onViewLeaderboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Leaderboard"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Leaderboard", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Play Again", fontSize = 16.sp)
            }
        }
    }
}

// ---------------------- ANSWER KEY SCREEN ----------------------

@Composable
fun AnswerKeyScreen(
    viewModel: QuizViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Answer Key",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.questions.isEmpty()) {
                Text("No questions to show.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(uiState.questions) { index, question ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Q${index + 1}. ${question.text}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val correctOption =
                                    question.options.getOrNull(question.correctOptionIndex)

                                Text(
                                    text = "Correct answer:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = correctOption ?: "N/A",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CorrectGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- LEADERBOARD SCREEN ----------------------

@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val repository = remember { QuizRepository() }

    var entries by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null
            entries = repository.getGlobalLeaderboard()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load leaderboard"
        } finally {
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Leaderboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                entries.isEmpty() -> {
                    Text("No scores yet. Be the first to play!")
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = entry.username.ifBlank { "Anonymous" },
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Score: ${entry.score}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
