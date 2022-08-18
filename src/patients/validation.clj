(ns patients.validation
  (:require [java-time :as jt]))

(defn validate-name [name]
  (if (nil? (re-matches #"^[\pL\pN\s-_]{2,128}$" name)) "Name should contain from 2 to 128 alphanumeric symbols, spaces, underscores or hyphens" nil))

(defn validate-gender-id [gender-id]
  (if (or (not (integer? gender-id)) (not (pos? gender-id))) "Wrong gender ID" nil))

(defn validate-birthdate [birthdate]
  (if (jt/after? birthdate (jt/local-date)) "Birthdate can't be in the future" nil))

(defn validate-address [address]
  (if (empty? address) "Address can't be empty" nil))

(defn validate-oms [oms]
  (if (nil? (re-matches #"^\d{10}$" oms)) "OMS should contain 10 digits" nil))

(defn validate [p]
  (let [result [(validate-name      (:name p))
                (validate-gender-id (:gender-id p))
                (validate-birthdate (:birthdate p))
                (validate-address   (:address p))
                (validate-oms       (:oms p))]]
    (flatten (filter some? result))))

(defn do-validated [f patient]
  (let [result (validate patient)]
    (if (empty? result)
      (f patient)
      {:errors result})))
