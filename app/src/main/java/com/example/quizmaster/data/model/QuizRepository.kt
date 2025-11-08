// File: app/src/main/java/com/example/quizmaster/data/QuizRepository.kt
package com.example.quizmaster.data

import com.example.quizmaster.data.model.LeaderboardEntry
import com.example.quizmaster.data.model.QuizQuestion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class QuizRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    // Keep the local list for simplicity.
    private val allQuestions = listOf(
        QuizQuestion("q1", "What is the official language for Android development?", listOf("Java", "Kotlin", "Dart", "Swift"), 1),
        QuizQuestion("q2", "Which architectural component survives configuration changes?", listOf("Activity", "Fragment", "ViewModel", "Intent"), 2),
        QuizQuestion("q3", "What is a 'coroutine'?", listOf("A thread", "A light-weight thread", "A function", "An Activity"), 1),
        QuizQuestion("q4", "What does 'MVVM' stand for?", listOf("Model-View-View-Model", "Model-View-ViewModel", "Main-View-ViewModel", "Model-Value-View-Model"), 1),
        QuizQuestion("q5", "Which file defines app permissions?", listOf("build.gradle", "MainActivity.kt", "styles.xml", "AndroidManifest.xml"), 3)
    )

    suspend fun getQuestionsForQuiz(): List<QuizQuestion> {
        // We'll return the local list, shuffled.
        return allQuestions.shuffled()
    }

    suspend fun submitScore(entry: LeaderboardEntry) {
        try {
            db.collection("leaderboard")
                .add(entry)
                .await()
            println("Score submitted successfully!")
        } catch (e: Exception) {
            println("Error submitting score: ${e.message}")
        }
    }

    suspend fun getGlobalLeaderboard(): List<LeaderboardEntry> {
        return try {
            val snapshot = db.collection("leaderboard")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(20)
                .get()
                .await()
            snapshot.toObjects(LeaderboardEntry::class.java)
        } catch (e: Exception) {
            println("Error fetching leaderboard: ${e.message}")
            emptyList()
        }
    }
}