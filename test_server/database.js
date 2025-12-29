const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Datenbank-Pfad
const dbPath = path.join(__dirname, 'klcp_quiz.db');

// Datenbank-Verbindung erstellen
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Error opening database:', err.message);
    process.exit(1);
  } else {
    console.log('Connected to SQLite database.');
    initializeDatabase().catch((initErr) => {
      console.error('Database initialization failed:', initErr);
      process.exit(1);
    });
  }
});

// Datenbank initialisieren
async function initializeDatabase() {
  try {
    // Users Tabelle (für Authentifizierung)
    await runQuery(`
      CREATE TABLE IF NOT EXISTS users (
        id TEXT PRIMARY KEY,
        email TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        is_verified BOOLEAN DEFAULT FALSE,
        verification_token TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `);

    // Quiz Sessions Tabelle
    await runQuery(`
      CREATE TABLE IF NOT EXISTS quiz_sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        device_id TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        total_questions INTEGER NOT NULL,
        correct_answers INTEGER NOT NULL,
        wrong_answers INTEGER NOT NULL,
        percentage REAL NOT NULL,
        duration_minutes INTEGER NOT NULL,
        language TEXT DEFAULT 'en',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
      )
    `);

    // User Stats Tabelle
    await runQuery(`
      CREATE TABLE IF NOT EXISTS user_stats (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL UNIQUE,
        device_id TEXT NOT NULL,
        total_quizzes INTEGER DEFAULT 0,
        total_correct INTEGER DEFAULT 0,
        total_wrong INTEGER DEFAULT 0,
        current_streak INTEGER DEFAULT 0,
        best_streak INTEGER DEFAULT 0,
        total_time_minutes INTEGER DEFAULT 0,
        average_percentage REAL DEFAULT 0,
        last_quiz_date DATETIME,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
      )
    `);

    // Achievements Tabelle
    await runQuery(`
      CREATE TABLE IF NOT EXISTS achievements (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        achievement_type TEXT NOT NULL,
        unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user_id, achievement_type),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
      )
    `);

    // Exam Unlocks Tabelle
    await runQuery(`
      CREATE TABLE IF NOT EXISTS exam_unlocks (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        exam_number INTEGER NOT NULL,
        unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user_id, exam_number),
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
      )
    `);

    // Backup Data Tabelle (für vollständige Backups)
    await runQuery(`
      CREATE TABLE IF NOT EXISTS backup_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id TEXT NOT NULL,
        device_id TEXT NOT NULL,
        backup_data TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
      )
    `);

    // Indizes für Performance
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_quiz_sessions_user_id ON quiz_sessions(user_id)`);
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_quiz_sessions_device_id ON quiz_sessions(device_id)`);
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_user_stats_user_id ON user_stats(user_id)`);
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_achievements_user_id ON achievements(user_id)`);
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_exam_unlocks_user_id ON exam_unlocks(user_id)`);
    await runQuery(`CREATE INDEX IF NOT EXISTS idx_backup_data_user_id ON backup_data(user_id)`);

    console.log('Database initialized successfully.');
  } catch (error) {
    console.error('Database initialization error:', error);
    throw error;
  }
}

// Hilfsfunktionen
function runQuery(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function(err) {
      if (err) {
        reject(err);
      } else {
        resolve({ lastID: this.lastID, changes: this.changes });
      }
    });
  });
}

function getQuery(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) {
        reject(err);
      } else {
        resolve(row);
      }
    });
  });
}

function allQuery(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) {
        reject(err);
      } else {
        resolve(rows);
      }
    });
  });
}

module.exports = {
  db,
  runQuery,
  getQuery,
  allQuery
};
