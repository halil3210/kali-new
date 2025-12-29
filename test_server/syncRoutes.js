const express = require('express');
const { runQuery, getQuery, allQuery } = require('./database');

const router = express.Router();

// Middleware für JWT-Verifikation (importiert aus authRoutes)
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ message: 'Access token required' });
  }

  const jwt = require('jsonwebtoken');
  const JWT_SECRET = process.env.JWT_SECRET || 'klcp-super-secret-key-2025';

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ message: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
}

// Sync Upload - Daten vom Client hochladen
router.post('/upload', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId, sessions, stats, achievements, examUnlocks } = req.body;

    if (!deviceId) {
      return res.status(400).json({ message: 'Device ID is required' });
    }

    let uploadedCount = 0;

    // Quiz Sessions hochladen
    if (sessions && Array.isArray(sessions)) {
      for (const session of sessions) {
        try {
          await runQuery(`
            INSERT OR REPLACE INTO quiz_sessions
            (user_id, device_id, timestamp, total_questions, correct_answers, wrong_answers, percentage, duration_minutes, language)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          `, [
            userId,
            deviceId,
            session.timestamp,
            session.totalQuestions,
            session.correctAnswers,
            session.wrongAnswers,
            session.percentage,
            session.durationMinutes || 0,
            session.language || 'en'
          ]);
          uploadedCount++;
        } catch (error) {
          console.error('Error uploading session:', error);
        }
      }
    }

    // User Stats aktualisieren
    if (stats) {
      await runQuery(`
        INSERT OR REPLACE INTO user_stats
        (user_id, device_id, total_quizzes, total_correct, total_wrong, current_streak, best_streak, total_time_minutes, average_percentage, last_quiz_date, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
      `, [
        userId,
        deviceId,
        stats.totalQuizzes || 0,
        stats.totalCorrect || 0,
        stats.totalWrong || 0,
        stats.currentStreak || 0,
        stats.bestStreak || 0,
        stats.totalTimeMinutes || 0,
        stats.averagePercentage || 0,
        stats.lastQuizDate ? new Date(stats.lastQuizDate).toISOString() : null
      ]);
    }

    // Achievements hochladen
    if (achievements && Array.isArray(achievements)) {
      for (const achievement of achievements) {
        try {
          await runQuery(`
            INSERT OR IGNORE INTO achievements (user_id, achievement_type, unlocked_at)
            VALUES (?, ?, ?)
          `, [
            userId,
            achievement.type,
            achievement.unlockedAt ? new Date(achievement.unlockedAt).toISOString() : new Date().toISOString()
          ]);
        } catch (error) {
          console.error('Error uploading achievement:', error);
        }
      }
    }

    // Exam Unlocks hochladen
    if (examUnlocks && Array.isArray(examUnlocks)) {
      for (const unlock of examUnlocks) {
        try {
          await runQuery(`
            INSERT OR IGNORE INTO exam_unlocks (user_id, exam_number, unlocked_at)
            VALUES (?, ?, ?)
          `, [
            userId,
            unlock.examNumber,
            unlock.unlockedAt ? new Date(unlock.unlockedAt).toISOString() : new Date().toISOString()
          ]);
        } catch (error) {
          console.error('Error uploading exam unlock:', error);
        }
      }
    }

    res.json({
      message: 'Sync upload successful',
      uploadedItems: uploadedCount,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error('Sync upload error:', error);
    res.status(500).json({ message: 'Sync upload failed' });
  }
});

// Sync Download - Daten zum Client herunterladen
router.get('/download/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.params;

    if (!deviceId) {
      return res.status(400).json({ message: 'Device ID is required' });
    }

    // Quiz Sessions laden
    const sessions = await allQuery(`
      SELECT id, timestamp, total_questions as totalQuestions,
             correct_answers as correctAnswers, wrong_answers as wrongAnswers,
             percentage, duration_minutes as durationMinutes, language
      FROM quiz_sessions
      WHERE user_id = ? AND device_id != ?
      ORDER BY timestamp DESC
      LIMIT 100
    `, [userId, deviceId]);

    // User Stats laden
    const stats = await getQuery(`
      SELECT total_quizzes as totalQuizzes, total_correct as totalCorrect,
             total_wrong as totalWrong, current_streak as currentStreak,
             best_streak as bestStreak, total_time_minutes as totalTimeMinutes,
             average_percentage as averagePercentage, last_quiz_date as lastQuizDate
      FROM user_stats
      WHERE user_id = ?
    `, [userId]);

    // Achievements laden
    const achievements = await allQuery(`
      SELECT achievement_type as type, unlocked_at as unlockedAt
      FROM achievements
      WHERE user_id = ?
      ORDER BY unlocked_at DESC
    `, [userId]);

    // Exam Unlocks laden
    const examUnlocks = await allQuery(`
      SELECT exam_number as examNumber, unlocked_at as unlockedAt
      FROM exam_unlocks
      WHERE user_id = ?
      ORDER BY unlocked_at DESC
    `, [userId]);

    res.json({
      message: 'Sync download successful',
      data: {
        sessions: sessions || [],
        stats: stats || null,
        achievements: achievements || [],
        examUnlocks: examUnlocks || []
      },
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error('Sync download error:', error);
    res.status(500).json({ message: 'Sync download failed' });
  }
});

module.exports = router;
