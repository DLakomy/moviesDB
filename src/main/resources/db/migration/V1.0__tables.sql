CREATE TABLE standalones (
  id TEXT PRIMARY KEY NOT NULL,
  title TEXT NOT NULL,
  year INTEGER NOT NULL,
  owner_id TEXT NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE series (
  id TEXT PRIMARY KEY NOT NULL,
  title TEXT NOT NULL,
  owner_id TEXT NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE episodes (
  series_id TEXT NOT NULL,
  number INTEGER NOT NULL,
  title TEXT NOT NULL,
  year INTEGER NOT NULL,
  PRIMARY KEY (series_id, number),
  FOREIGN KEY (series_id) REFERENCES series(id)
);

CREATE TABLE users (
  id TEXT PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  password_hash TEXT NOT NULL
);

-- I'll skip the indexes, it's not going to be used anyway
