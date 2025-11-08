// File: app/src/main/java/com/example/quizmaster/MainActivity.kt
package com.example.quizmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.quizmaster.ui.quiz.QuizUiState
import com.example.quizmaster.ui.quiz.QuizViewModel
import com.example.quizmaster.ui.theme.CorrectGreen
import com.example.quizmaster.ui.theme.IncorrectRed
import com.example.quizmaster.ui.theme.NeutralGray
import com.example.quizmaster.ui.theme.QuizMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizMasterTheme {
                // This is a simple ViewModelFactory to pass the Repository
                val viewModelFactory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return QuizViewModel(QuizRepository()) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }

                val viewModel: QuizViewModel = viewModel(factory = viewModelFactory)
                QuizScreen(viewModel = viewModel)
            }
        }
    }
}

// --- The Main Screen Composable (CORRECTED) ---
@Composable
fun QuizScreen(viewModel: QuizViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Use Surface to set the background color for the entire app
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Our new AppBackground color
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // --- THIS BLOCK IS NOW CORRECT ---
            uiState.isQuizFinished -> {
                QuizFinishedScreen(
                    score = uiState.score,
                    totalQuestions = uiState.questions.size,
                    isScoreSubmitted = uiState.isScoreSubmitted, // <-- Pass the state
                    onSubmit = { username ->
                        viewModel.submitFinalScore(username)
                    },
                    onPlayAgain = { // <-- Pass the function
                        viewModel.playAgain()
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// --- Composable for the Active Quiz (Unchanged) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizContent(uiState: QuizUiState, onAnswerSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header: Timer & Score ---
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
                color = if (uiState.timeLeftSeconds <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        // --- Question Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Makes the card take up available space
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        // --- Answer Options ---
        uiState.currentQuestion?.options?.forEachIndexed { index, optionText ->
            val isSelected = uiState.selectedAnswerIndex == index
            val hasAnswered = uiState.selectedAnswerIndex != null
            val correctIndex = uiState.currentQuestion.correctOptionIndex

            val buttonColor = when {
                !hasAnswered -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                isSelected && index == correctIndex -> ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                isSelected && index != correctIndex -> ButtonDefaults.buttonColors(containerColor = IncorrectRed)
                index == correctIndex -> ButtonDefaults.buttonColors(containerColor = CorrectGreen)
                else -> ButtonDefaults.buttonColors(containerColor = NeutralGray)
            }

            Button(
                onClick = { onAnswerSelected(index) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(56.dp),
                colors = buttonColor,
                enabled = !hasAnswered,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(text = optionText, fontSize = 16.sp)
            }
        }
    }
}


// --- Composable for the Finished Screen (CORRECTED) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizFinishedScreen(
    score: Int,
    totalQuestions: Int,
    isScoreSubmitted: Boolean, // <-- Correct parameter
    onSubmit: (String) -> Unit,
    onPlayAgain: () -> Unit    // <-- Correct parameter
) {
    var username by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Score Card (Always shows) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

        // --- This is the logic you were missing ---
        if (!isScoreSubmitted) {
            // --- STATE 1: Show the Submit Form ---
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
            // --- STATE 2: Show "Done!" and "Play Again" ---
            Text(
                "Score Submitted!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPlayAgain, // Call the new function
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