package alie.info.newmultichoice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a quiz question with bilingual support (English and German)
 */
@Entity(tableName = "questions")
data class Question(
    @PrimaryKey
    val id: Int,
    
    // English version
    val questionEn: String,
    val optionAEn: String,
    val optionBEn: String,
    val optionCEn: String,
    val optionDEn: String,
    
    // German version
    val questionDe: String,
    val optionADe: String,
    val optionBDe: String,
    val optionCDe: String,
    val optionDDe: String,
    
    // Correct answer (A, B, C, or D)
    val correct: String
)

