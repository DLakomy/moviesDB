-- Insert a standalone for user 1
INSERT INTO standalones (id, title, year, owner_id) VALUES ('c4db840e-1167-4616-9af3-1971bf2d83e2', 'Masmix', 1999, '4e37a586-fb27-11ed-be56-0242ac120002');
INSERT INTO standalones (id, title, year, owner_id) VALUES ('97f4739b-0977-4fb5-aa0d-3a3c2240b4fc', 'Masmix 2', 2002, '4e37a586-fb27-11ed-be56-0242ac120002');

-- Insert a series for user 1
INSERT INTO series (id, title, owner_id) VALUES ('d1d9e530-c742-46a5-97d0-b85e93d1f75d', 'Zero-Two: Masmix stories', '4e37a586-fb27-11ed-be56-0242ac120002');
INSERT INTO episodes (series_id, number, title, year)
SELECT 'd1d9e530-c742-46a5-97d0-b85e93d1f75d', 1, 'Pilot', 2003 union all
SELECT 'd1d9e530-c742-46a5-97d0-b85e93d1f75d', 2, 'The Reckoning', 2004;

-- Insert a standalone for user 2
INSERT INTO standalones (id, title, year, owner_id) VALUES ('59c2dd90-39f5-4522-b548-c3860e915e21', 'Coffee: first impact', 2010, '4e37a950-fb27-11ed-be56-0242ac120002');
INSERT INTO standalones (id, title, year, owner_id) VALUES ('085ab486-205d-413c-82cc-e459317cf1b4', 'Doppio, please', 2012, '4e37a950-fb27-11ed-be56-0242ac120002');

-- Insert a series for user 2
INSERT INTO series (id, title, owner_id) VALUES ('263ddbd5-771d-4b25-b3e7-d206cc3a650a', 'Caffeine Chronicles', '4e37a950-fb27-11ed-be56-0242ac120002');
INSERT INTO episodes (series_id, number, title, year)
SELECT '263ddbd5-771d-4b25-b3e7-d206cc3a650a', 1, 'Pourovers', 2007 union all
SELECT '263ddbd5-771d-4b25-b3e7-d206cc3a650a', 2, 'Espresso', 2008;
