(ns patients.config)

(def db-user     (or (System/getenv "DB_USER")
                     "postgres"))

(def db-password (or (System/getenv "DB_PASSWORD")
                     "postgrespw"))

(def db-host     (or (System/getenv "DB_HOST")
                     "localhost"))

(def db-port     (or (System/getenv "DB_PORT")
                     "55000"))

(def db-name     (or (System/getenv "DB_NAME")
                     "postgres"))

(def db-structure '{:columns
#{{:table_name "genders",
   :column_name "id",
   :data_type "integer",
   :character_maximum_length nil,
   :is_nullable "NO",
   :column_default "nextval('genders_id_seq'::regclass)"}
  {:table_name "patients",
   :column_name "birthdate",
   :data_type "date",
   :character_maximum_length nil,
   :is_nullable "NO",
   :column_default nil}
  {:table_name "patients",
   :column_name "id",
   :data_type "integer",
   :character_maximum_length nil,
   :is_nullable "NO",
   :column_default "nextval('patients_id_seq'::regclass)"}
  {:table_name "patients",
   :column_name "oms",
   :data_type "character",
   :character_maximum_length 10,
   :is_nullable "NO",
   :column_default nil}
  {:table_name "patients",
   :column_name "address",
   :data_type "json",
   :character_maximum_length nil,
   :is_nullable "NO",
   :column_default nil}
  {:table_name "patients",
   :column_name "name",
   :data_type "character varying",
   :character_maximum_length 128,
   :is_nullable "NO",
   :column_default nil}
  {:table_name "patients",
   :column_name "gender_id",
   :data_type "smallint",
   :character_maximum_length nil,
   :is_nullable "NO",
   :column_default nil}
  {:table_name "genders",
   :column_name "name",
   :data_type "character varying",
   :character_maximum_length 32,
   :is_nullable "YES",
   :column_default nil}},
 :foregin-keys
 #{{:table_name "patients",
   :column_name "gender_id",
   :foreign_table_schema "public",
   :foreign_table_name "genders",
   :foreign_column_name "id"}}})
