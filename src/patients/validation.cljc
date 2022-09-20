(ns patients.validation
  (:require [cljc.java-time.local-date :as ld]))

(defn validate-name [name]
  (if (nil? (re-matches #"^[a-zA-Zа-яА-Я0-9\s\-_]{2,128}$" name)) "Name should contain from 2 to 128 alphanumeric symbols, spaces, underscores or hyphens" nil))

(defn validate-gender-id [gender-id]
  (if (or (not (integer? gender-id)) (not (pos? gender-id))) (str "Wrong gender ID: " gender-id) nil))

(defn validate-birthdate [birthdate]
  (if (nil? birthdate)
	  "Birthdate can't be empty"
	  (if (ld/is-after birthdate (ld/now)) "Birthdate can't be in the future" nil)))

(defn validate-address [address]
  (if (empty? address) "Address can't be empty" nil))

(defn validate-oms [oms]
  (if (nil? (re-matches #"^\d{10}$" oms)) "OMS should contain 10 digits" nil))

(defn validate [p]
  (let [result [(validate-name      (:name p))
                (validate-gender-id (:gender_id p))
                (validate-birthdate (:birthdate p))
                (validate-address   (:address p))
                (validate-oms       (:oms p))]]
    (flatten (filter some? result))))
