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

// Vollständiges Backup erstellen
router.post('/create', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.body;

    if (!deviceId) {
      return res.status(400).json({ message: 'Device ID is required' });
    }

    // Alle User-Daten sammeln
    const sessions = await allQuery(`
      SELECT id, timestamp, total_questions, correct_answers, wrong_answers,
             percentage, duration_minutes, language, created_at
      FROM quiz_sessions
      WHERE user_id = ?
      ORDER BY timestamp DESC
    `, [userId]);

    const stats = await getQuery(`
      SELECT total_quizzes, total_correct, total_wrong, current_streak,
             best_streak, total_time_minutes, average_percentage, last_quiz_date
      FROM user_stats
      WHERE user_id = ?
    `, [userId]);

    const achievements = await allQuery(`
      SELECT achievement_type, unlocked_at
      FROM achievements
      WHERE user_id = ?
      ORDER BY unlocked_at DESC
    `, [userId]);

    const examUnlocks = await allQuery(`
      SELECT exam_number, unlocked_at
      FROM exam_unlocks
      WHERE user_id = ?
      ORDER BY unlocked_at DESC
    `, [userId]);

    // Backup-Objekt erstellen
    const backupData = {
      userId: userId,
      deviceId: deviceId,
      createdAt: new Date().toISOString(),
      version: '2.0',
      data: {
        sessions: sessions || [],
        stats: stats || null,
        achievements: achievements || [],
        examUnlocks: examUnlocks || []
      }
    };

    // Backup in Datenbank speichern (als JSON-String)
    const result = await runQuery(`
      INSERT INTO backup_data (user_id, device_id, backup_data)
      VALUES (?, ?, ?)
    `, [userId, deviceId, JSON.stringify(backupData)]);

    res.json({
      message: 'Backup created successfully',
      backupId: result.lastID,
      backupData: backupData,
      timestamp: backupData.createdAt
    });

  } catch (error) {
    console.error('Backup creation error:', error);
    res.status(500).json({ message: 'Failed to create backup' });
  }
});

// Backup wiederherstellen
router.get('/restore/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.params;

    if (!deviceId) {
      return res.status(400).json({ message: 'Device ID is required' });
    }

    // Neuestes Backup laden
    const backup = await getQuery(`
      SELECT backup_data, created_at
      FROM backup_data
      WHERE user_id = ? AND device_id = ?
      ORDER BY created_at DESC
      LIMIT 1
    `, [userId, deviceId]);

    if (!backup) {
      return res.status(404).json({ message: 'No backup found for this device' });
    }

    // Backup-Daten parsen
    let backupData;
    try {
      backupData = JSON.parse(backup.backup_data);
    } catch (parseError) {
      return res.status(500).json({ message: 'Backup data is corrupted' });
    }

    // Backup-Daten zurückgeben
    res.json({
      message: 'Backup retrieved successfully',
      backup: backupData,
      restoredAt: new Date().toISOString(),
      backupCreatedAt: backup.created_at
    });

  } catch (error) {
    console.error('Backup restore error:', error);
    res.status(500).json({ message: 'Failed to restore backup' });
  }
});

// Backup anwenden (Daten wiederherstellen)
router.post('/restore/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.params;
    const { backupId, confirmRestore } = req.body;

    if (!deviceId || !confirmRestore) {
      return res.status(400).json({
        message: 'Device ID and confirmation required',
        note: 'Set confirmRestore=true to proceed with data restoration'
      });
    }

    // Backup-Daten laden
    let backup;
    if (backupId) {
      // Spezifisches Backup laden
      backup = await getQuery(`
        SELECT backup_data FROM backup_data WHERE id = ? AND user_id = ?
      `, [backupId, userId]);
    } else {
      // Neuestes Backup laden
      backup = await getQuery(`
        SELECT backup_data FROM backup_data
        WHERE user_id = ? AND device_id = ?
        ORDER BY created_at DESC LIMIT 1
      `, [userId, deviceId]);
    }

    if (!backup) {
      return res.status(404).json({ message: 'Backup not found' });
    }

    // Backup-Daten parsen
    let backupData;
    try {
      backupData = JSON.parse(backup.backup_data);
    } catch (parseError) {
      return res.status(500).json({ message: 'Backup data is corrupted' });
    }

    let restoredCount = 0;

    // Sessions wiederherstellen
    if (backupData.data.sessions) {
      for (const session of backupData.data.sessions) {
        try {
          await runQuery(`
            INSERT OR REPLACE INTO quiz_sessions
            (user_id, device_id, timestamp, total_questions, correct_answers, wrong_answers, percentage, duration_minutes, language)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          `, [
            userId,
            deviceId,
            session.timestamp,
            session.total_questions,
            session.correct_answers,
            session.wrong_answers,
            session.percentage,
            session.duration_minutes || 0,
            session.language || 'en'
          ]);
          restoredCount++;
        } catch (error) {
          console.error('Error restoring session:', error);
        }
      }
    }

    // Stats wiederherstellen
    if (backupData.data.stats) {
      await runQuery(`
        INSERT OR REPLACE INTO user_stats
        (user_id, device_id, total_quizzes, total_correct, total_wrong, current_streak, best_streak, total_time_minutes, average_percentage, last_quiz_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      `, [
        userId,
        deviceId,
        backupData.data.stats.total_quizzes || 0,
        backupData.data.stats.total_correct || 0,
        backupData.data.stats.total_wrong || 0,
        backupData.data.stats.current_streak || 0,
        backupData.data.stats.best_streak || 0,
        backupData.data.stats.total_time_minutes || 0,
        backupData.data.stats.average_percentage || 0,
        backupData.data.stats.last_quiz_date
      ]);
    }

    // Achievements wiederherstellen
    if (backupData.data.achievements) {
      for (const achievement of backupData.data.achievements) {
        try {
          await runQuery(`
            INSERT OR IGNORE INTO achievements (user_id, achievement_type, unlocked_at)
            VALUES (?, ?, ?)
          `, [
            userId,
            achievement.achievement_type,
            achievement.unlocked_at
          ]);
        } catch (error) {
          console.error('Error restoring achievement:', error);
        }
      }
    }

    // Exam Unlocks wiederherstellen
    if (backupData.data.examUnlocks) {
      for (const unlock of backupData.data.examUnlocks) {
        try {
          await runQuery(`
            INSERT OR IGNORE INTO exam_unlocks (user_id, exam_number, unlocked_at)
            VALUES (?, ?, ?)
          `, [
            userId,
            unlock.exam_number,
            unlock.unlocked_at
          ]);
        } catch (error) {
          console.error('Error restoring exam unlock:', error);
        }
      }
    }

    res.json({
      message: 'Backup restored successfully',
      restoredItems: restoredCount,
      restoredAt: new Date().toISOString(),
      backupCreatedAt: backupData.createdAt
    });

  } catch (error) {
    console.error('Backup restore error:', error);
    res.status(500).json({ message: 'Failed to restore backup' });
  }
});

module.exports = router;
