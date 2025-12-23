package alie.info.newmultichoice

import alie.info.newmultichoice.data.Question
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Quiz logic
 */
class QuizViewModelTest {
    
    @Test
    fun testQuestionDataModel() {
        val question = Question(
            id = 1,
            questionEn = "What is Linux?",
            optionAEn = "An operating system",
            optionBEn = "A programming language",
            optionCEn = "A database",
            optionDEn = "A web browser",
            questionDe = "Was ist Linux?",
            optionADe = "Ein Betriebssystem",
            optionBDe = "Eine Programmiersprache",
            optionCDe = "Eine Datenbank",
            optionDDe = "Ein Webbrowser",
            correct = "A"
        )
        
        assertEquals(1, question.id)
        assertEquals("What is Linux?", question.questionEn)
        assertEquals("A", question.correct)
    }
    
    @Test
    fun testCorrectAnswer() {
        val question = Question(
            id = 1,
            questionEn = "Test question",
            optionAEn = "Option A",
            optionBEn = "Option B",
            optionCEn = "Option C",
            optionDEn = "Option D",
            questionDe = "Test Frage",
            optionADe = "Option A",
            optionBDe = "Option B",
            optionCDe = "Option C",
            optionDDe = "Option D",
            correct = "B"
        )
        
        val userAnswer = "B"
        val isCorrect = userAnswer == question.correct
        
        assertTrue(isCorrect)
    }
    
    @Test
    fun testWrongAnswer() {
        val question = Question(
            id = 1,
            questionEn = "Test question",
            optionAEn = "Option A",
            optionBEn = "Option B",
            optionCEn = "Option C",
            optionDEn = "Option D",
            questionDe = "Test Frage",
            optionADe = "Option A",
            optionBDe = "Option B",
            optionCDe = "Option C",
            optionDDe = "Option D",
            correct = "C"
        )
        
        val userAnswer = "A"
        val isCorrect = userAnswer == question.correct
        
        assertFalse(isCorrect)
    }
    
    @Test
    fun testScoreCalculation() {
        val totalQuestions = 50
        val correctAnswers = 42
        
        val percentage = (correctAnswers.toFloat() / totalQuestions.toFloat()) * 100
        
        assertEquals(84.0f, percentage, 0.01f)
    }
}

