(ns patients.script.app
  (:require [patients.script.api :as api]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-com.core :refer [datepicker-dropdown]]
            ["react-dom/client" :refer [createRoot]]
            [cljs-time.coerce :as coerce]
            [clojure.string :as str]
            [ajax.core :refer [GET POST DELETE]])
  (:require-macros [patients.script.macros :refer [filter-input]]))

;; State

(defonce genders (r/atom {}))

(defonce data (r/atom []))

(defonce search (r/atom ""))

(defonce filter-bd-from (r/atom nil))

(defonce filter-bd-to (r/atom nil))

(defonce filters-init {:name nil :gender_id nil :address nil :oms nil})

(defonce filters (r/atom filters-init))

;; Misc functions

(defn drop-filters []
  (reset! filters filters-init)
  (reset! filter-bd-from nil)
  (reset! filter-bd-to nil))

(defn is-numeric-or-special [e]
  (let [code  (.-keyCode e)
        shift (.-shiftKey e)
        alt   (.-altKey e)
        ctrl  (.-ctrlKey e)]
    (or ctrl
        alt
        (and (< 47 code) (> 58 code) (= false shift))
        (and (< 95 code) (> 106 code))
        (= 8 code) (= 9 code)
        (and (< 34 code) (> 40 code))
        (= 46 code))))

(defn ts-to-date [timestamp]
  (coerce/from-long timestamp))

(defn on-change-from [ts]
  (reset! filter-bd-from (ts-to-date ts))
  (api/get-list-filtered))

(defn on-change-to [ts]
  (reset! filter-bd-to (ts-to-date ts))
  (api/get-list-filtered @filters))

;; API functions

(defn get-list
  ([] (get-list {}))
  ([filters] (GET "/patients" {:params filters
                               :handler #(reset! data %)})))

(defn get-list-filtered []
  (get-list (-> (select-keys @filters (for [[k v] @filters :when (some? v)] k))
                (assoc-in [:birthdate :from] (if (some? @filter-bd-from) (.substring (coerce/to-string @filter-bd-from) 0 10) ""))
                (assoc-in [:birthdate :to] (if (some? @filter-bd-to) (.substring (coerce/to-string @filter-bd-to) 0 10) "")))))

(defn get-list-search [str]
  (GET "/patients/search" {:params {:q str}
                           :handler #(reset! data %)}))

(defn get-genders []
  (GET "/genders" {:handler #(reset! genders %)}))

;; Reagent components

(defn data-table-header []
  [:thead
   [:tr [:th "ID"][:th "Name"][:th "Gender"][:th "Birthdate"][:th "Address"][:th "OMS #"][:th]]
   [:tr
    [:th]
    [:th (filter-input :name {})]
    [:th [:select {:on-change #((swap! filters assoc :gender_id  (-> % .-target .-value))(get-list-filtered))}
          [:option {:value ""} "---"]
          (not-empty (for [item @genders]
                       [:option {:key (str "gender-id-" (item "id")) :value (item "id")} (item "name")]))]]
    [:th [:ul
          [:li "From:" [datepicker-dropdown
                        :show-today?   true
                        :start-of-week 0
                        :placeholder   "Date from..."
                        :format        "yyyy-mm-dd"
                        :model         filter-bd-from
                        :on-change     on-change-from]] ;; why literal not works here?
          [:li "To:" [datepicker-dropdown
                      :show-today?   true
                      :start-of-week 0
                      :placeholder   "Date to..."
                      :format        "yyyy-mm-dd"
                      :model         filter-bd-to
                      :on-change     on-change-to]]]] ;; ...and here!
    [:th (filter-input :address {})]
    [:th (filter-input :oms {:on-key-down #(when (not (is-numeric-or-special %))
                                             (.preventDefault %))})]
    [:th]]])

(defn data-table []
   [:table
    [data-table-header]
    [:tbody
     (not-empty (for [row @data]
                      [:tr {:key (str "row-id-" (row "id"))}
                       [:td (str (row "id"))]
                       [:td (row "name")]
                       [:td (row "gender")]
                       [:td (row "birthdate")]
                       [:td (row "address")]
                       [:td (row "oms")]
                       [:td]]))]])

(defn app []
  [:div
   [data-table]])

(def root (createRoot (js/document.getElementById "app")))

(defn ^:export run []
  (.render root [(r/as-element (app))])
  (get-list)
  (get-genders))

(run)
