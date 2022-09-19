(ns patients.integration-test
  (:require [patients.fixtures :refer [with-db with-server test-port]]
            [webdriver.core :refer :all]
            [clojure.test :refer [deftest use-fixtures is]]
            [clojure.string :as str]))

(use-fixtures :each with-db)

(use-fixtures :once with-server)

(System/setProperty "webdriver.chrome.driver" "D:\\dev\\chromedriver.exe")

(defonce sasha {:name "Sasha"
                :gender "Male"
                :birthdate "27 August 1985"
                :address "Smolensk"
                :oms "0123456789"})

(defonce iuliia {:name "Iuliia"
                 :gender "Female"
                 :birthdate "26 April 1985"
                 :address "Krasnoyarsk"
                 :oms "1234567890"})

(defonce chrome-options (org.openqa.selenium.chrome.ChromeOptions.))
(.setBinary chrome-options "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")
;; (.addArguments chrome-options "headless")
(defonce driver {:driver (org.openqa.selenium.chrome.ChromeDriver. chrome-options)})

(defn set-date [date-str xpath]
  (let [ls    (str/split date-str #" ")
        day   (nth ls 0)
        month (nth ls 1)
        year  (nth ls 2)]
    (click (wait-for-element driver :xpath (str xpath "//div[@class = 'rc-datepicker-dropdown-anchor']")))))

(defn fill-patient [patient]
  (set-element driver
               (wait-for-element driver :id "name-input")
               (:name patient))
  (set-element driver
               (get-visible-element driver :id "gender-select")
               (:gender patient))
  (set-element driver
               (wait-for-element driver :id "address-input")
               (:address patient))
  (set-element driver
               (wait-for-element driver :id "oms-input")
               (:oms patient))
  (set-date (:birthdate patient) "//div[@id = 'birthdate-input']"))

(deftest home
  (to driver (str "http://localhost:" test-port))
  (click (wait-for-element driver :id "new-patient-button"))
  (wait-for-element driver :id "save-button")
  (click (wait-for-element driver :id "cancel-button"))

  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (click (get-visible-element driver :id "new-patient-button"))

  (fill-patient sasha))



