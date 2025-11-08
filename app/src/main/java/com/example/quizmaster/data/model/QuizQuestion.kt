package com.example.quizmaster.data.model

/**
 * The core model for a single quiz question.
 */
data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int
)