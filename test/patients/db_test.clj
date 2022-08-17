(ns patients.db-test
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [clojure.java.jdbc :as j]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [java-time :as jt]
   [patients.fixtures :refer [with-db]]
   [clojure.test :refer [deftest use-fixtures is]]
   [clojure.test.check.generators :as gen]
   [patients.common-test :refer [generate-string-except generate-local-date]]))

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
                     :birthdate (jt/local-date "1985-08-27")
                     :address address
                     :oms "0123456789"}
        patient-out {:id 1
                     :name "Sasha"
                     :gender_id 1
                     :birthdate (jt/local-date "1985-08-27")
                     :address address
                     :oms "0123456789"}]
    (db/insert! patient-in)
    (is (= (first (map db/convert-birthdate-to-local-date (j/query db/pg-uri ["SELECT * FROM patients;"]))) patient-out)))

  (doseq [patient dataset-list] (db/insert! patient))
  (is (= 101 (-> (j/query db/pg-uri ["SELECT COUNT(*) FROM patients;"]) first :count)))

  (let [strs (flatten (repeatedly 10 #(generate-string-except [""] 1)))]
    (doseq [s strs]
      (is (every? #(str/includes? (:name %) s) (db/list-filtered {:name s})))
      (is (every? #(str/includes? (:address %) s) (db/list-filtered {:address s})))))

  (let [strs1 (flatten (repeatedly 10 #(generate-string-except [""] 1)))
        strs2 (flatten (repeatedly 10 #(generate-string-except [""] 1)))
        strs (map vector strs1 strs2)]
    (doseq [ss strs]
      (is (every? #(and (str/includes? (:name %) (first ss))
                        (str/includes? (:address %) (last ss)))
                  (db/list-filtered {:name (first ss) :address (last ss)})))))
  (let [dates1 (repeatedly 100 #(generate-local-date))
        dates2 (repeatedly 100 #(generate-local-date))
        dates (map vector dates1 dates2)]
    (doseq [ds dates]
      (println (jt/format (apply jt/min ds)))
      (is (every? #(and (jt/not-before? (:birthdate %) (apply jt/min ds))
                        (jt/not-after? (:birthdate %) (apply jt/max ds)))
                  (db/list-filtered {:birthdate {:from (apply jt/min ds) :to (apply jt/max ds)}}))))))
