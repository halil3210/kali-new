const express = require('express');
const { runQuery, getQuery, allQuery } = require('./database');

const router = express.Router();

// Middleware für JWT-Verifikation
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

// User-Statistiken abrufen
router.get('/user/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.params;

    // User Stats laden
    let stats = await getQuery(`
      SELECT total_quizzes as totalQuizzes, total_correct as totalCorrect,
             total_wrong as totalWrong, current_streak as currentStreak,
             best_streak as bestStreak, total_time_minutes as totalTimeMinutes,
             average_percentage as averagePercentage, last_quiz_date as lastQuizDate
      FROM user_stats
      WHERE user_id = ?
    `, [userId]);

    // Falls keine Stats vorhanden, aus Sessions berechnen
    if (!stats) {
      const sessionsResult = await allQuery(`
        SELECT COUNT(*) as totalQuizzes,
               SUM(correct_answers) as totalCorrect,
               SUM(wrong_answers) as totalWrong,
               SUM(duration_minutes) as totalTimeMinutes,
               AVG(percentage) as averagePercentage,
               MAX(timestamp) as lastQuizTimestamp
        FROM quiz_sessions
        WHERE user_id = ?
      `, [userId]);

      if (sessionsResult && sessionsResult[0]) {
        const sessionStats = sessionsResult[0];
        stats = {
          totalQuizzes: sessionStats.totalQuizzes || 0,
          totalCorrect: sessionStats.totalCorrect || 0,
          totalWrong: sessionStats.totalWrong || 0,
          currentStreak: 0, // Wird später berechnet
          bestStreak: 0,    // Wird später berechnet
          totalTimeMinutes: sessionStats.totalTimeMinutes || 0,
          averagePercentage: sessionStats.averagePercentage || 0,
          lastQuizDate: sessionStats.lastQuizTimestamp ?
            new Date(sessionStats.lastQuizTimestamp * 1000).toISOString() : null
        };

        // Stats in Datenbank speichern
        await runQuery(`
          INSERT OR REPLACE INTO user_stats
          (user_id, device_id, total_quizzes, total_correct, total_wrong, total_time_minutes, average_percentage, last_quiz_date)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `, [
          userId, deviceId,
          stats.totalQuizzes, stats.totalCorrect, stats.totalWrong,
          stats.totalTimeMinutes, stats.averagePercentage, stats.lastQuizDate
        ]);
      } else {
        stats = {
          totalQuizzes: 0,
          totalCorrect: 0,
          totalWrong: 0,
          currentStreak: 0,
          bestStreak: 0,
          totalTimeMinutes: 0,
          averagePercentage: 0,
          lastQuizDate: null
        };
      }
    }

    res.json({
      message: 'User stats retrieved successfully',
      stats: stats
    });

  } catch (error) {
    console.error('User stats error:', error);
    res.status(500).json({ message: 'Failed to retrieve user stats' });
  }
});

// User-Stats aktualisieren
router.post('/update', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId, stats } = req.body;

    if (!deviceId || !stats) {
      return res.status(400).json({ message: 'Device ID and stats are required' });
    }

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

    res.json({ message: 'User stats updated successfully' });

  } catch (error) {
    console.error('Stats update error:', error);
    res.status(500).json({ message: 'Failed to update user stats' });
  }
});

// Unlock-Status abrufen
router.get('/unlock-status/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;

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
      message: 'Unlock status retrieved successfully',
      achievements: achievements || [],
      examUnlocks: examUnlocks || []
    });

  } catch (error) {
    console.error('Unlock status error:', error);
    res.status(500).json({ message: 'Failed to retrieve unlock status' });
  }
});

// Exam Unlock Status für spezifischen Exam prüfen
router.get('/exam-unlock/:deviceId/:examNumber', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { examNumber } = req.params;

    const examNum = parseInt(examNumber);
    if (isNaN(examNum)) {
      return res.status(400).json({ message: 'Invalid exam number' });
    }

    // Prüfen ob Exam freigeschaltet ist
    const unlock = await getQuery(`
      SELECT unlocked_at as unlockedAt
      FROM exam_unlocks
      WHERE user_id = ? AND exam_number = ?
    `, [userId, examNum]);

    res.json({
      message: 'Exam unlock status retrieved successfully',
      examNumber: examNum,
      isUnlocked: !!unlock,
      unlockedAt: unlock ? unlock.unlockedAt : null
    });

  } catch (error) {
    console.error('Exam unlock check error:', error);
    res.status(500).json({ message: 'Failed to check exam unlock status' });
  }
});

// Exam freischalten
router.post('/unlock-exam', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId, examNumber } = req.body;

    if (!deviceId || typeof examNumber !== 'number') {
      return res.status(400).json({ message: 'Device ID and exam number are required' });
    }

    // Prüfen ob bereits freigeschaltet
    const existingUnlock = await getQuery(`
      SELECT id FROM exam_unlocks WHERE user_id = ? AND exam_number = ?
    `, [userId, examNumber]);

    if (existingUnlock) {
      return res.status(409).json({ message: 'Exam already unlocked' });
    }

    // Exam freischalten
    await runQuery(`
      INSERT INTO exam_unlocks (user_id, exam_number) VALUES (?, ?)
    `, [userId, examNumber]);

    res.json({
      message: 'Exam unlocked successfully',
      examNumber: examNumber,
      unlockedAt: new Date().toISOString()
    });

  } catch (error) {
    console.error('Exam unlock error:', error);
    res.status(500).json({ message: 'Failed to unlock exam' });
  }
});

module.exports = router;
