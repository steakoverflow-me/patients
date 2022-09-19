(ns patients.db-test
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [clojure.java.jdbc :as j]
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [cljc.java-time.local-date :as ld]
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
                     :birthdate (ld/parse "1985-08-27")
                     :address address
                     :oms "0123456789"}
        patient-out {:id 1
                     :name "Sasha"
                     :gender_id 1
                     :birthdate "1985-08-27"
                     :address address
                     :oms "0123456789"}]
    (db/insert! patient-in)
    (is (= (dissoc (first (db/get-one 1)) :gender) patient-out)))

  (doseq [patient dataset-list] (db/insert! patient))
  (is (= 101 (-> (j/query db/pg-uri ["SELECT COUNT(*) FROM patients;"]) first :count)))

  (let [id (+ 102 (rand-int 1000))]
    (is (nil? (first (db/get-one id)))))

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
      (let [[dmin dmax] (map ld/to-string (if (< (apply ld/compare-to ds) 0) [(first ds)(second ds)] [(second ds)(first ds)]))]
        (is (every? #(and (>= (compare (:birthdate %) dmin) 0)
                          (<= (compare (:birthdate %) dmax) 0))
                    (db/list-filtered {"birthdate[from]" dmin "birthdate[to]" dmax}))))))

  (let [strs (flatten (repeatedly 10 #(generate-string-except [""] 2)))]
    (doseq [s strs]
      (let [result (db/list-filtered {:q s})
            result-strs (map #(str/join "\n" [(:name %) (:birthdate %) (:address %) (:oms %)]) result)]
        (is (every? #(str/includes? % s) result-strs)))))

  (let [id (+ 102 (rand-int 1000))]
    (is (nil? (first (db/get-one id)))))

  (let [id       (inc (rand-int 101))
        old      (first (db/get-one id))
        new-name (first (generate-string-except [] 3))
        new      (update (update old :name (constantly new-name))
                         :birthdate
                         ld/parse)]
    (db/update! (dissoc new :gender))
    (is (= (update (first (db/get-one id)) :birthdate ld/parse) new)))

  (let [id (inc (rand-int 101))]
    (db/delete! id)
    (is (nil? (first (db/get-one id))))))

