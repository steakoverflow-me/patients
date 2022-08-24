(ns patients.db
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [java-time :as jt]
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
                                          [[:id :serial :primary :key]
                                           [:name "VARCHAR(32)"]]))
    (j/insert-multi! t-con :genders [:name] [["Male"]["Female"]["Others"]])
    (j/execute! t-con (j/create-table-ddl :patients
                                          [[:id        :serial :primary :key]
                                           [:gender_id :smallint "NOT NULL"]
                                           [:birthdate :date "NOT NULL"]
                                           [:oms       "CHAR(10)" "NOT NULL"]
                                           [:name      "VARCHAR(128)" "NOT NULL"]
                                           [:address   :text "NOT NULL"]
                                           ["FOREIGN KEY(gender_id) REFERENCES genders(id)"]]))))

;; Решено не использовать записи, потому что разные поля могут быть (id? и gender/gender_id)

(defn convert-birthdate-to-local-date [patient]
  (update patient :birthdate (fn [bd-in]
                               (let [bd-in (patient :birthdate)
                                     bd    (if (= (type bd-in) String) (subs bd-in 0 10) bd-in)]
                                 (jt/local-date bd)))))

;; Непонятно, почему не передаётся first
(defn get-one [id]
  (first (j/query pg-uri [sql/get id])))

(defn insert! [patient]
  (assert (nil? (:id patient)))
  (j/insert! pg-uri :patients patient))

(defn update! [patient]
  (assert (some? (:id patient)))
  (j/update! pg-uri :patients (update patient :birthdate jt/local-date) ["id = ?" (:id patient)]))

(defn delete! [id]
  (j/delete! pg-uri :patients ["id = ?" id]))

(defn list-filtered [filters]
  (let [wheres (str (if (empty? (:id filters)) "" (format "\nAND patients.id = %s" (:id filters)))
                    (if (empty? (:name filters)) "" (format "\nAND patients.name LIKE '%%%s%%'" (:name filters)))
                    (if (empty? (:gender_id filters)) "" (format "\nAND patients.gender_id = %s" (:gender_id filters)))
                    (if (and (empty? (get-in filters [:birthdate :from]))
                             (empty? (get-in filters [:birthdate :to])))
                      ""
                      (format "\nAND patients.birthdate >= '%s' AND patients.birthdate <= '%s'"
                              (jt/format (get-in filters [:birthdate :from]))
                              (jt/format (get-in filters [:birthdate :to]))))
                    (if (empty? (:address filters)) "" (format "\nAND patients.address LIKE '%%%s%%'" (:address filters)))
                    (if (empty? (:oms filters)) "" (format "\nAND patients.oms LIKE '%%%s%%'" (:oms filters))))]
    (j/query pg-uri (str sql/list " WHERE 1 = 1 " wheres ";"))))


