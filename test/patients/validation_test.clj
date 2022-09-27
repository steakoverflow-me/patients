(ns patients.validation-test
  (:require [patients.validation :as v]
            [patients.app :refer [do-validated]]
            [clojure.string :as str]
            [cljc.java-time.local-date :as ld]
            [clojure.test :refer [deftest is]]
            [clojure.test.check.generators :as gen]
            [patients.common-test :refer [generate-string-of-length
                                          generate-special-string-of-length
                                          generate-local-date]]))

(deftest name-validation-test
  (let [empty-str ""
        too-short (generate-string-of-length 1)
        too-long  (generate-string-of-length (+ 128 (rand-int 1000)))
        with-spec (str (generate-string-of-length 10)
                       (generate-special-string-of-length 1)
                       (generate-string-of-length 10))
        valid     (str (generate-string-of-length 5)
                       "-"
                       (generate-string-of-length 5)
                       " "
                       (generate-string-of-length 5)
                       "_"
                       (generate-string-of-length 5))]
    (is (some? (v/validate-name empty-str)))
    (is (some? (v/validate-name too-short)))
    (is (some? (v/validate-name too-long)))
    (is (some? (v/validate-name with-spec)))
    (is (nil?  (v/validate-name valid)))))

(deftest gender-id-validation-test
  (let [float    (first (gen/sample (gen/double* {:min 1 :max 10}) 1))
        zero     0
        negative (dec (first (gen/sample (gen/fmap - gen/nat) 1)))
        valid    (inc (first (gen/sample gen/nat 1)))]
    (is (some? (v/validate-gender-id float)))
    (is (some? (v/validate-gender-id zero)))
    (is (some? (v/validate-gender-id negative)))
    (is (nil?  (v/validate-gender-id valid)))))

(deftest birthdate-validation-test
  (let [invalid (generate-local-date 2050)
        valid   (generate-local-date)]
    (is (some? (v/validate-birthdate invalid)))
    (is (nil?  (v/validate-birthdate valid)))))

(deftest address-validate-test
  (let [invalid-1 ""
        invalid-2 (generate-string-of-length (+ 130 (rand-int 100)))
        valid   (generate-string-of-length (+ 28 (rand-int 100)))]
    (is (some? (v/validate-address invalid-1)))
    (is (some? (v/validate-address invalid-2)))
    (is (nil?  (v/validate-address valid)))))

(deftest oms-validate-test
  (let [too-short (str (inc (rand-int 999999999)))
        too-long  (str (+ 10000000000 (long (rand 90000000000))))
        alphabet  (generate-string-of-length 10)
        valid     (str (+ 1000000000 (long (rand 9000000000))))]
    (is (some? (v/validate-oms too-short)))
    (is (some? (v/validate-oms too-long)))
    (is (some? (v/validate-oms alphabet)))
    (is (nil?  (v/validate-oms valid)))))

(deftest validate-test
  (let [invalid {:name      (generate-special-string-of-length 1)
                 :gender_id -1
                 :birthdate (ld/parse "2050-01-01")
                 :address   ""
                 :oms       (generate-string-of-length 10)}
        valid   {:name      (generate-string-of-length (+ 2 (rand-int 30)))
                 :gender_id (inc (rand-int 10))
                 :birthdate (generate-local-date)
                 :address   (generate-special-string-of-length (+ 50 (rand-int 50)))
                 :oms       (str (+ 1000000000 (long (rand 9000000000))))}]
    (is (= 5 (count (v/validate invalid))))
    (is (empty? (v/validate valid)))))

(deftest do-validated-test
  (let [num     (rand-int 100)
        invalid {:name      (generate-special-string-of-length 1)
                 :gender_id -1
                 :birthdate (ld/parse "2050-01-01")
                 :address   ""
                 :oms       (generate-string-of-length 10)}
        valid   {:name      (generate-string-of-length (+ 2 (rand-int 30)))
                 :gender_id (inc (rand-int 10))
                 :birthdate (generate-local-date)
                 :address   (generate-special-string-of-length (+ 50 (rand-int 50)))
                 :oms       (str (+ 1000000000 (long (rand 9000000000))))}]
    (let [result (do-validated (fn [_] num) invalid)]
      (println result)
      (is (= 5 (count (str/split (:body result) #"\\n")))))
    (is (= num (do-validated (fn [_] num) valid)))))
