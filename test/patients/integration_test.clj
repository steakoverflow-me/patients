(ns patients.integration-test
  (:require [patients.fixtures :refer [with-db with-server test-port]]
            [webdriver.core :refer :all]
            [clojure.test :refer [deftest use-fixtures is]]
            [clojure.string :as str]))

(use-fixtures :each with-db)

(use-fixtures :once with-server)

(defonce sleep 1000)

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

(defn set-date [id date-str]
  (let [ls    (str/split date-str #" ")
        day   (nth ls 0)
        month (nth ls 1)
        year  (nth ls 2)
        xpath (format "//div[@id = '%s']" id)]
    (click (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-dropdown-anchor')]")))
    (let [prev-y (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-prev-year')]"))
          prev-m (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-prev-month')]"))
          curr (wait-for-element driver :xpath (str xpath "//div[contains(@class, 'rc-datepicker-month')]"))]
      (while (not (str/includes? (.getText curr) (str (inc (Integer/parseInt year)))))
        (click prev-y))
      (while (not= (.getText curr) (str month " " year))
        (click prev-m))
      (click (wait-for-element driver :xpath (str xpath "//td[contains(@class, 'rc-datepicker-date') and not(contains(@class, 'rc-datepicker-out-of-focus')) and text()='" day "']"))))))

(defn fill-input [id value]
  (set-element driver (get-visible-element driver :id id) value))

(defn fill-patient [patient]
  (fill-input "name-input" (:name patient))
  (fill-input "gender-select" (:gender patient))
  (set-date "birthdate-input" (:birthdate patient))
  (fill-input "address-input" (:address patient))
  (fill-input "oms-input" (:oms patient))

(defn get-count []
  (count (get-elements driver :xpath "//tbody//tr[contains(@class, 'border-amber-500')]")))

(defn check-count [count]
  (is (= count (get-count))))

(defn check-sasha []
  (is (not (nil? (get-visible-element driver :xpath "//tbody//tr/td/div[text() = 'Sasha']")))))

(defn check-no-sasha []
  (is (nil? (get-visible-element driver :xpath "//tbody//tr/td/div[text() = 'Sasha']"))))

(defn check-iuliia []
  (is (not (nil? (get-visible-element driver :xpath "//tbody//tr/td/div[text() = 'Iuliia']")))))

(defn check-no-iuliia []
  (is (nil? (get-visible-element driver :xpath "//tbody//tr/td/div[text() = 'Iuliia']"))))

(defn clear-filters []
  (click (get-visible-element driver :id "clear-filters-button")))

(defn clear-search []
  (click (get-visible-element driver :id "clear-search-button")))

(deftest workflow
  (to driver (str "http://localhost:" test-port))
  (click (wait-for-element driver :id "new-patient-button"))
  (wait-for-element driver :id "save-button")
  (click (wait-for-element driver :id "cancel-button"))

  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  ;; INSERT

  (click (get-visible-element driver :id "new-patient-button"))

  (fill-patient sasha)
  (click (get-visible-element driver :id "save-button"))

  (Thread/sleep sleep)
  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (check-count 1)

  (click (get-visible-element driver :id "new-patient-button"))

  (fill-patient iuliia)
  (click (get-visible-element driver :id "save-button"))

  (Thread/sleep sleep)
  (is (nil? (get-visible-element driver :id "save-button")))
  (is (nil? (get-visible-element driver :id "cancel-button")))

  (check-count 2)

  ;; SEARCH

  (fill-input "search-input" "sk")
  (Thread/sleep sleep)
  (check-count 2)
  (check-sasha)
  (check-iuliia)
  (clear-search)

  (fill-input "search-input" "Sa")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-search)

  (fill-input "search-input" "90")
  (Thread/sleep sleep)
  (check-count 1)
  (check-iuliia)
  (check-no-sasha)
  (clear-search)

  (fill-input "search-input" "abcde")
  (Thread/sleep sleep)
  (check-count 0)
  (check-no-sasha)
  (check-no-iuliia)
  (clear-search)

  ;; FILTERS

  (Thread/sleep sleep)

  (fill-input "name-filter" "S")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "name-filter" "I")
  (Thread/sleep sleep)
  (check-count 1)
  (check-iuliia)
  (check-no-sasha)
  (clear-filters)

  (fill-input "name-filter" "a")
  (Thread/sleep sleep)
  (check-count 2)
  (check-sasha)
  (check-iuliia)
  (clear-filters)

  (fill-input "name-filter" "A")
  (Thread/sleep sleep)
  (check-count 0)
  (check-no-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "gender-filter-select" "Male")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "gender-filter-select" "Female")
  (Thread/sleep sleep)
  (check-count 1)
  (check-no-sasha)
  (check-iuliia)
  (clear-filters)

  (fill-input "gender-filter-select" "Others")
  (Thread/sleep sleep)
  (check-count 0)
  (check-no-sasha)
  (check-no-iuliia)
  (clear-filters)

  (set-date "birthdate-filter-from" "1 June 2015")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-filters)

  (set-date "birthdate-filter-to" "1 June 2015")
  (Thread/sleep sleep)
  (check-count 1)
  (check-no-sasha)
  (check-iuliia)
  (clear-filters)

  (set-date "birthdate-filter-from" "1 June 2015")
  (set-date "birthdate-filter-to" "1 July 2015")
  (Thread/sleep sleep)
  (check-count 0)
  (check-no-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "sk")
  (fill-input "oms-filter" "0")
  (Thread/sleep sleep)
  (check-count 2)
  (check-sasha)
  (check-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "nsk")
  (fill-input "oms-filter" "0")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "rsk")
  (fill-input "oms-filter" "0")
  (Thread/sleep sleep)
  (check-count 1)
  (check-no-sasha)
  (check-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "sk")
  (fill-input "oms-filter" "90")
  (Thread/sleep sleep)
  (check-count 1)
  (check-no-sasha)
  (check-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "sk")
  (fill-input "oms-filter" "01")
  (Thread/sleep sleep)
  (check-count 1)
  (check-sasha)
  (check-no-iuliia)
  (clear-filters)

  (fill-input "name-filter" "a")
  (fill-input "address-filter" "sk")
  (fill-input "oms-filter" "54")
  (Thread/sleep sleep)
  (check-count 0)
  (check-no-sasha)
  (check-no-iuliia)
  (clear-filters)

  ;; EDIT/VALIDATION

  )
