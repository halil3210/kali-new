package alie.info.newmultichoice.data

import com.google.gson.annotations.SerializedName

/**
 * JSON model for parsing questions from assets
 */
data class QuestionJsonModel(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("question_en")
    val questionEn: String,
    
    @SerializedName("question_de")
    val questionDe: String,
    
    @SerializedName("options_en")
    val optionsEn: List<String>,
    
    @SerializedName("options_de")
    val optionsDe: List<String>,
    
    @SerializedName("correct")
    val correct: String
) {
    /**
     * Convert JSON model to Room entity
     */
    fun toQuestion(): Question {
        return Question(
            id = id,
            questionEn = questionEn,
            optionAEn = optionsEn.getOrNull(0)?.removePrefix("A) ") ?: "",
            optionBEn = optionsEn.getOrNull(1)?.removePrefix("B) ") ?: "",
            optionCEn = optionsEn.getOrNull(2)?.removePrefix("C) ") ?: "",
            optionDEn = optionsEn.getOrNull(3)?.removePrefix("D) ") ?: "",
            questionDe = questionDe,
            optionADe = optionsDe.getOrNull(0)?.removePrefix("A) ") ?: "",
            optionBDe = optionsDe.getOrNull(1)?.removePrefix("B) ") ?: "",
            optionCDe = optionsDe.getOrNull(2)?.removePrefix("C) ") ?: "",
            optionDDe = optionsDe.getOrNull(3)?.removePrefix("D) ") ?: "",
            correct = correct
        )
    }
}

