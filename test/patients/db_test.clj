(ns patients.db-test
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [clojure.java.jdbc :as j]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [java-time :refer [local-date]]
   [patients.fixtures :refer [with-db]]
   [clojure.test :refer [deftest use-fixtures is]]
   [clojure.test.check.generators :as gen]
   [patients.common-test :refer [generate-string-except]]))

(def dataset-list
  (map
   (comp (fn [p] (update p :gender_id (fn [g-id] (Integer/parseInt g-id))))
         db/convert-birthdate-to-local-date)
   (:objects (json/read-json (slurp "dev/dataset.json")))))

(use-fixtures :once with-db)

(deftest test-db
  (is (= db-structure (db/db-info)))

  (let [address "214032, Smolensk, Marshal Eremenko str., 10-88"
        patient-in  {:name "Sasha"
                     :gender_id 1
                     :birthdate (local-date "1985-08-27")
                     :address address
                     :oms "0123456789"}
        patient-out {:id 1
                     :name "Sasha"
                     :gender_id 1
                     :birthdate (local-date "1985-08-27")
                     :address address
                     :oms "0123456789"}]
    (db/insert! patient-in)
    (is (= (first (map db/convert-birthdate-to-local-date (j/query db/pg-uri ["SELECT * FROM patients;"]))) patient-out)))

  (doseq [patient dataset-list] (db/insert! patient))
  (is (= 101 (-> (j/query db/pg-uri ["SELECT COUNT(*) FROM patients;"]) first :count)))

  (let [strs (repeatedly 100 #(generate-string-except [""]))]
    (doseq [name strs]
      (is (every? #(str/includes? (:name %) name) (db/list-filtered {:name name}))))))


