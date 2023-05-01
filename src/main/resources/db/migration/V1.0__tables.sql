CREATE TABLE standalones (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT,
  year INTEGER,
  owner_id INTEGER,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE series (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT,
  owner_id INTEGER,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE episodes (
  series_id INTEGER,
  number INTEGER,
  title TEXT,
  year INTEGER,
  PRIMARY KEY (series_id, number),
  FOREIGN KEY (series_id) REFERENCES series(id)
);

CREATE TABLE users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT,
  password_hash TEXT
);

-- I'll skip the indexes, it's not going to be used anyway
