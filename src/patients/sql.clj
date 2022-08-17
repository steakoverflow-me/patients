(ns patients.sql
  (:require [patients.config :as cfg]))

(def db-info-cols "
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable, column_default
    FROM information_schema.columns
WHERE table_name IN
    (SELECT table_name
         FROM information_schema.tables
     WHERE table_schema = 'public');")

(def db-info-fks "
SELECT
    tc.table_name,
    kcu.column_name,
    ccu.table_schema AS foreign_table_schema,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM
    information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu
      ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage AS ccu
      ON ccu.constraint_name = tc.constraint_name
      AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name IN
    (SELECT table_name
         FROM information_schema.tables
     WHERE table_schema = 'public');")

(def drop-all (format "
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO %s;
GRANT ALL ON SCHEMA public TO public;" cfg/db-user))

(def list "
SELECT
    patients.id AS id,
    patients.name AS name,
    genders.name AS gender,
    patients.birthdate AS birthdate,
    patients.address AS address,
    patients.oms AS oms
FROM patients LEFT JOIN genders ON genders.id = patients.gender_id")
