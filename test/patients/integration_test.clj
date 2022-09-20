(ns patients.integration-test
  (:require [patients.fixtures :refer [with-db with-server test-port]]
            [webdriver.core :refer :all]
            [clojure.test :refer [deftest use-fixtures is]]
            [clojure.string :as str]))

(use-fixtures :each with-db)

(use-fixtures :once with-server)

(defonce sasha {:name "Sasha"
                :gender "Male"
                :birthdate "27 August 2015"
                :birthdate-iso "2015-08-27"
                :address "Smolensk"
                :oms "0123456789"})

(defonce iuliia {:name "Iuliia"
                 :gender "Female"
                 :birthdate "26 April 2015"
                 :birthdate-iso "2015-04-26"
                 :address "Krasnoyarsk"
                 :oms "1234567890"})

(System/setProperty "webdriver.chrome.driver" "D:\\dev\\chromedriver.exe")

(defonce chrome-options (org.openqa.selenium.chrome.ChromeOptions.))
(.setBinary chrome-options "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")
;; (.addArguments chrome-options "headless")
(defonce driver {:driver (org.openqa.selenium.chrome.ChromeDriver. chrome-options)})

(defn set-date [date-str xpath]
  (let [ls    (str/split date-str #" ")
        day   (nth ls 0)
        month (nth ls 1)
        year  (nth ls 2)]
    (click (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-dropdown-anchor')]")))
    (let [prev-y (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-prev-year')]"))
          prev-m (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-prev-month')]"))
          curr (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-month')]"))]
      (while (not (str/includes? (.getText curr) (str (inc (Integer/parseInt year)))))
        (click prev-y))
      (while (not= (.getText curr) (str month " " year))
        (click prev-m))
      (click (wait-for-element driver :xpath (str xpath "//td[contains(@class, 'rc-datepicker-date') and not(contains(@class, 'rc-datepicker-out-of-focus')) and text()='" day "']"))))))

(defn fill-patient [patient]
  (set-element driver
               (wait-for-element driver :id "name-input")
               (:name patient))
  (set-element driver
               (get-visible-element driver :id "gender-select")
               (:gender patient))
  (set-element driver
               (get-visible-element driver :id "address-input")
               (:address patient))
  (set-element driver
               (get-visible-element driver :id "oms-input")
               (:oms patient))
  (set-date (:birthdate patient) "//div[@id = 'birthdate-input']"))

(defn get-count []
  (try
    (wait-for-element driver :xpath "//tbody//tr[contains(@class, 'border-amber-500')]")
    (count (get-elements driver :xpath "//tbody//tr[contains(@class, 'border-amber-500')]"))
    (catch Exception e (0))))

(deftest workflow
  (to driver (str "http://localhost:" test-port))
  (click (wait-for-element driver :id "new-patient-button"))
  (wait-for-element driver :id "save-button")
  (click (wait-for-element driver :id "cancel-button"))

  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (click (get-visible-element driver :id "new-patient-button"))

  (fill-patient sasha)
  (click (get-visible-element driver :id "save-button"))

  (Thread/sleep 500)
  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (is (= 1 (get-count)))

  (click (get-visible-element driver :id "new-patient-button"))

  (fill-patient iuliia)
  (click (get-visible-element driver :id "save-button"))

  (Thread/sleep 500)
  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (is (= 2 (get-count))))
