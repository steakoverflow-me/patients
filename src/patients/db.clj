(ns patients.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [cljc.java-time.local-date :as ld]
            [patients.sql :as sql]
            [patients.config :as cfg]))

(def pg-uri (format "postgres://%s:%s@%s:%s/%s" cfg/db-user cfg/db-password cfg/db-host cfg/db-port cfg/db-name))

(defn db-info []
  {:columns (set (j/query pg-uri [sql/db-info-cols]))
   :foregin-keys (set (j/query pg-uri [sql/db-info-fks]))})

(defn drop-tables
  ([] (drop-tables pg-uri))
  ([conn] (j/execute! conn sql/drop-tables)))

(defn drop-all
  "Drops schema 'public' and creates it back. Only for testing purposes!"
  ([] (drop-all pg-uri))
  ([conn] (j/execute! conn sql/drop-all)))

(defn init-database []
  (j/with-db-transaction [t-con pg-uri]
    (drop-tables t-con)
    (j/execute! t-con (j/create-table-ddl :genders
                                          [[:id :serial :primary :key]
                                           [:name "VARCHAR(32)"]]))
    (j/insert-multi! t-con :genders [:name] [["Male"]["Female"]["Others"]])
    (j/execute! t-con (j/create-table-ddl :patients
                                          [[:id        :serial :primary :key]
                                           [:gender_id :smallint "NOT NULL"]
                                           [:birthdate :date "NOT NULL"]
                                           [:oms       "CHAR(10)" "NOT NULL"]
                                           [:name      "VARCHAR(32)" "NOT NULL"]
                                           [:address   "VARCHAR(128)" "NOT NULL"]
                                           ["FOREIGN KEY(gender_id) REFERENCES genders(id)"]]))))

(defn convert-birthdate-to-local-date [patient]
  (update patient :birthdate (fn [bd-in]
                               (let [bd-in (patient :birthdate)
                                     bd    (if (= (type bd-in) String) (subs bd-in 0 10) bd-in)]
                                 (ld/parse bd)))))

(defn get-one [id]
  (j/query pg-uri [sql/get id]))

(defn insert! [patient]
  (assert (nil? (:id patient)))
  (j/insert! pg-uri :patients patient))

(defn update! [patient]
  (assert (some? (:id patient)))
  (j/update! pg-uri :patients patient ["id = ?" (:id patient)]))

(defn delete! [id]
  (j/delete! pg-uri :patients ["id = ?" id]))

(defn list-filtered [filters]
  (let [wheres (str (when (not-empty (:id filters)) (format "\nAND patients.id = %s" (:id filters)))
                    (when (not-empty (:name filters)) (format "\nAND patients.name LIKE '%%%s%%'" (:name filters)))
                    (when (not-empty (:gender_id filters)) (format "\nAND patients.gender_id = %s" (:gender_id filters)))

                    (when (not-empty (filters "birthdate[from]")) (format "\nAND patients.birthdate >= '%s'" (filters "birthdate[from]")))
                    (when (not-empty (filters "birthdate[to]")) (format "\nAND patients.birthdate <= '%s'" (filters "birthdate[to]")))

                    (when (not-empty (:address filters)) (format "\nAND patients.address LIKE '%%%s%%'" (:address filters)))
                    (when (not-empty (:oms filters)) (format "\nAND patients.oms LIKE '%%%s%%'" (:oms filters)))
                    (when (not-empty (:q filters)) (format "\nAND CONCAT(patients.name, '\n', patients.birthdate, '\n', patients.address, '\n', patients.oms, '\n', CONCAT_WS('-', SUBSTRING(patients.oms, 1, 3), SUBSTRING(patients.oms, 4, 3), SUBSTRING(patients.oms, 7, 3), SUBSTRING(patients.oms, 10, 1))) LIKE '%%%s%%'" (:q filters)))
                    "\nORDER BY patients.id;")]
    (println (str "WHERES:\t" wheres))
    (j/query pg-uri (str sql/list " WHERE 1 = 1 " wheres ";"))))

(defn get-genders []
  (j/query pg-uri sql/genders))
