(ns patients.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [patients.sql :as sql]
            [patients.config :as cfg]))

(def pg-uri (format "postgres://%s:%s@%s:%s/%s" cfg/db-user cfg/db-password cfg/db-host cfg/db-port cfg/db-name))

(defn db-info []
  {:columns (set (j/query pg-uri [sql/db-info-cols]))
   :foregin-keys (set (j/query pg-uri [sql/db-info-fks]))})

(defn init-database []
  (j/with-db-transaction [t-con pg-uri]
    (j/execute! t-con sql/drop-all)
    (j/execute! t-con (j/create-table-ddl :genders
                                          [[:id   :serial :primary :key]
                                           [:name "VARCHAR(32)"]]))
    (j/insert-multi! t-con :genders [:name] [["Male"]["Female"]["Others"]])
    (j/execute! t-con (j/create-table-ddl :patients
                                          [[:id        :serial "PRIMARY KEY"]
                                           [:gender_id :smallint "NOT NULL"]
                                           [:birthdate :date "NOT NULL"]
                                           [:oms       "CHAR(10)" "NOT NULL"]
                                           [:name      "VARCHAR(128)" "NOT NULL"]
                                           [:address   :json "NOT NULL"]
                                           ["FOREIGN KEY(gender_id) REFERENCES genders(id)"]]))))

