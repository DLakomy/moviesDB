-- Insert a standalone for user 1
INSERT INTO standalones (title, year, owner_id) VALUES ('Masmix', 1999, 1);
INSERT INTO standalones (title, year, owner_id) VALUES ('Masmix 2', 2002, 1);

-- Insert a series for user 1
INSERT INTO series (title, owner_id) VALUES ('Zero-Two: Masmix stories', 1);
INSERT INTO episodes (series_id, number, title, year)
SELECT s.id
     , ep.no
     , ep.title
     , ep.year
  FROM series s
       CROSS JOIN (
         SELECT 1 no, 'Pilot' title, 2003 year union all
         SELECT 2, 'The Reckoning', 2004 
       ) ep
 WHERE s.rowid = last_insert_rowid();

-- Insert a standalone for user 2
INSERT INTO standalones (title, year, owner_id) VALUES ('Coffee: first impact', 2010, 2);
INSERT INTO standalones (title, year, owner_id) VALUES ('Doppio, please', 2012, 2);

-- Insert a series for user 2
INSERT INTO series (title, owner_id) VALUES ('Caffeine Chronicles', 2);
INSERT INTO episodes (series_id, number, title, year)
SELECT s.id
     , ep.no
     , ep.title
     , ep.year
  FROM series s
       CROSS JOIN (
         SELECT 1 no, 'Pourovers' title, 2007 year union all
         SELECT 2, 'Espresso', 2008 
       ) ep
 WHERE s.rowid = last_insert_rowid();
