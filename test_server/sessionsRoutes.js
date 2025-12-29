const express = require('express');
const { runQuery, allQuery } = require('./database');

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

// Quiz-Sessions für Device abrufen
router.get('/:deviceId', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId } = req.params;

    if (!deviceId) {
      return res.status(400).json({ message: 'Device ID is required' });
    }

    // Quiz-Sessions laden (begrenzt auf letzte 50 für Performance)
    const sessions = await allQuery(`
      SELECT id, timestamp, total_questions as totalQuestions,
             correct_answers as correctAnswers, wrong_answers as wrongAnswers,
             percentage, duration_minutes as durationMinutes, language,
             created_at as createdAt
      FROM quiz_sessions
      WHERE user_id = ? AND device_id = ?
      ORDER BY timestamp DESC
      LIMIT 50
    `, [userId, deviceId]);

    res.json({
      message: 'Sessions retrieved successfully',
      sessions: sessions || [],
      count: sessions ? sessions.length : 0
    });

  } catch (error) {
    console.error('Sessions retrieval error:', error);
    res.status(500).json({ message: 'Failed to retrieve sessions' });
  }
});

// Quiz-Session speichern
router.post('/save', authenticateToken, async (req, res) => {
  try {
    const userId = req.user.userId;
    const { deviceId, session } = req.body;

    if (!deviceId || !session) {
      return res.status(400).json({ message: 'Device ID and session data are required' });
    }

    // Validierung der Session-Daten
    const requiredFields = ['timestamp', 'totalQuestions', 'correctAnswers', 'wrongAnswers', 'percentage'];
    for (const field of requiredFields) {
      if (typeof session[field] === 'undefined') {
        return res.status(400).json({ message: `Session ${field} is required` });
      }
    }

    // Session in Datenbank speichern
    const result = await runQuery(`
      INSERT INTO quiz_sessions
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

    res.json({
      message: 'Session saved successfully',
      sessionId: result.lastID,
      timestamp: new Date().toISOString()
    });

  } catch (error) {
    console.error('Session save error:', error);
    res.status(500).json({ message: 'Failed to save session' });
  }
});

module.exports = router;
