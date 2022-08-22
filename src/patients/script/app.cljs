(ns patients.script.app
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            ["react-dom/client" :refer [createRoot]]
            [clojure.string :as str]
            [ajax.core :refer [GET POST]]))

(defonce data (r/atom []))

(defonce timer (r/atom (js/Date.)))

(defonce time-color (r/atom "#f34"))

(defonce time-updater (js/setInterval
                       #(reset! timer (js/Date.)) 1000))

(defn data-table []
  [:table [[:tr [:th "ID"][:th "Name"][:th "Gender"][:th "Address"][:rh "OMS #"]]] (str @data)])
                ;; (map #([:tr
                ;;          [:td (str (:id %))]
                ;;          [:td (:name %)]
                ;;          [:td (:gender %)]
                ;;          [:td (:address %)]
                ;;          [:td (:oms %)]]
                ;;       )

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

(defn get-list-handler [list]
  (println list)
  (reset! data list))

(defn get-list
  ([] (get-list {}))
  ([filters] (GET "/patients" {:params filters
                               :handler get-list-handler})))

(defn simple-example []
  [:div
   [greeting "Hello world, it is now"]
   [clock]
   [color-input]
   [data-table]])

(def root (createRoot (js/document.getElementById "app")))

(defn ^:export run []
  (prn "Start render")
  (.render root [(r/as-element (simple-example))])
  (prn "Render complete")
  (get-list)
  (prn data))

(run)
