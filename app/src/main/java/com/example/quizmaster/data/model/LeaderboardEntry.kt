package com.example.quizmaster.data.model

/**
 * Model for a Leaderboard entry.
 * Note: It must have a no-argument constructor for Firestore,
 * so we provide default values.
 */
data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val timestamp: Long = 0L
)