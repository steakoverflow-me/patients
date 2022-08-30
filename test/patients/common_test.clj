(ns patients.common-test
  (:require [clojure.test :as t]
            [clojure.test.check.generators :as gen]
            [cljc.java-time.local-date :as ld]))

(defn generate-string-except
  ([exceptions] (generate-string-except exceptions 0 10))
  ([exceptions min-length] (generate-string-except exceptions min-length 10))
  ([exceptions min-length seq-length]
   (let [predicate (fn [in]
                     (and (>= (count in) min-length)
                          (= (some #{in} exceptions) nil)))]
     (gen/sample (gen/such-that predicate gen/string-alphanumeric) seq-length))))

(defn generate-string-of-length [length]
  (first (gen/sample (gen/fmap #(apply str %) 
                               (gen/vector gen/char-alphanumeric length)) 1)))

(defn generate-special-string-of-length [length]
  (first (gen/sample (gen/fmap #(apply str %) 
                               (gen/vector (gen/elements "#$%@&^") length)) 1)))

(defn generate-local-date
  ([] (generate-local-date 1900))
  ([min-year] (let [y-str (str (+ min-year (rand-int 122)))
            month (inc (rand-int 12))
            m-str (if (< month 10) (str "0" month) (str month))
            day   (inc (rand-int 28))
            d-str (if (< day 10) (str "0" day) (str day))]
        (ld/parse (str y-str "-" m-str "-" d-str)))))

