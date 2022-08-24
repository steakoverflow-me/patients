(ns patients.script.app
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["react-dom/client" :refer [createRoot]]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]))

(defonce data (r/atom []))

(defonce search (r/atom ""))

(defonce filters (r/atom {:name "" :gender "" :birthdate {:form "" :to ""} :address "" :oms ""}))

(defonce timer (r/atom (js/Date.)))

(defonce time-color (r/atom "#f34"))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

;; (defmacro filter-input [field]
;;   '[:input.filter (merge {:type  "text"
;;                           :value '(field @filters)
;;                           :on-change '#(reset! filters '(assoc @filters field (-> % .-target .-value)))})]
;;  )

(defn get-list-handler [list]
  (reset! data list))

(defn get-list
  ([] (get-list {}))
  ([filters] (GET "/patients" {:params filters
                               :handler get-list-handler})))

(defn data-table-header []
  [:thead
   [:tr [:th "ID"][:th "Name"][:th "Gender"][:th "Birthdate"][:th "Address"][:th "OMS #"]]
   [:tr
    [:th]
    [:th [:input.filter {:type "text"
                         :value (:name @filters)
                         :on-change #((reset! filters (assoc @filters :name (-> % .-target .-value)))(get-list @filters))}]]]])

(defn data-table []
   [:table
    [data-table-header]
    [:tbody
     (or (not-empty (for [row @data]
                      [:tr {:key (str "row-id-" (row "id"))}
                       [:td (str (row "id"))]
                       [:td (row "name")]
                       [:td (row "gender")]
                       [:td (row "birthdate")]
                       [:td (row "address")]
                       [:td (row "oms")]]))
         [:tr [:td "No data..."]])]])

(defn greeting [message]
  [:h1 message])

(defn clock []
  (let [time-str (-> @timer .toTimeString (str/split " ") first)]
    [:div.example-clock
     {:style {:color @time-color}}
     time-str]))

(defn color-input []
  [:div.color-input
   "Time color: "
   [:input {:type "text"
            :value @time-color
            :on-change #(reset! time-color (-> % .-target .-value))}]])

(defn simple-example []
  [:div
   [color-input]
   [data-table]])

(def root (createRoot (js/document.getElementById "app")))

(defn ^:export run []
  (.render root [(r/as-element (simple-example))])
  (get-list))

(run)
