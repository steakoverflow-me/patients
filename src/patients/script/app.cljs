(ns patients.script.app
  (:require [reagent.core :as r]
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

(defonce filter-birthdate-from (r/atom nil))

(defonce filter-birthdate-to (r/atom nil))

(defonce filters-init {:name nil :gender_id nil :address nil :oms nil})

(defonce filters (r/atom filters-init))

;; Misc functions

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

(defn get-filters []
  (-> (let [fs (assoc @filters :q @search)] (select-keys fs (for [[k v] fs :when (some? (not-empty v))] k)))
      (cond-> (some? @filter-birthdate-from) (assoc-in [:birthdate :from] (.substring (coerce/to-string @filter-birthdate-from) 0 10)))
      (cond-> (some? @filter-birthdate-to) (assoc-in [:birthdate :to] (.substring (coerce/to-string @filter-birthdate-to) 0 10)))))

(defn clear-filters []
  (reset! filters filters-init)
  (reset! filter-birthdate-from nil)
  (reset! filter-birthdate-to nil))

(defn clear-search []
  (reset! search nil))

;; API functions

(defn get-list
  ([] (get-list (get-filters)))
  ([fs] (GET "/patients" {:params fs
                               :handler #(reset! data %)})))

(defn get-genders []
  (GET "/genders" {:handler #(reset! genders %)}))

;; Event handler functions

(defn on-change-gender [gender-id]
  (swap! filters assoc :gender_id gender-id)
  (get-list))

(defn on-change-birthdate-from [ts]
  (reset! filter-birthdate-from (ts-to-date ts))
  (get-list))

(defn on-change-birthdate-to [ts]
  (reset! filter-birthdate-to (ts-to-date ts))
  (get-list))

(defn on-change-search [q]
  (reset! search q)
  (get-list))

(defn on-click-clear-filters []
  (clear-filters)
  (get-list))

(defn on-click-clear-search []
  (clear-search)
  (get-list))

;; Reagent components

(defn data-table-header []
  [:thead
   [:tr [:th "ID"][:th "Name"][:th "Gender"][:th "Birthdate"][:th "Address"][:th "OMS #"][:th]]
   [:tr
    [:th]
    [:th (filter-input :name {})]
    [:th [:select {:value (or (:gender_id @filters) "")
                   :on-change #(on-change-gender (-> % .-target .-value))}
          [:option {:value ""} "---"]
          (not-empty (for [item @genders]
                       [:option {:key (str "gender-id-" (item "id")) :value (item "id")} (item "name")]))]]
    [:th [:ul
          [:li "From:" [datepicker-dropdown
                        :show-today?   true
                        :start-of-week 0
                        :placeholder   "Date from..."
                        :format        "yyyy-mm-dd"
                        :model         filter-birthdate-from
                        :on-change     on-change-birthdate-from]]
          [:li "To:" [datepicker-dropdown
                      :show-today?   true
                      :start-of-week 0
                      :placeholder   "Date to..."
                      :format        "yyyy-mm-dd"
                      :model         filter-birthdate-to
                      :on-change     on-change-birthdate-to]]]]
    [:th (filter-input :address {})]
    [:th (filter-input :oms {:on-key-down #(when (not (is-numeric-or-special %))
                                             (.preventDefault %))})]
    [:th [:button {:on-click on-click-clear-filters} "Clear filters"]]]])

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

(defn search-input []
  [:div
   [:input.filter {:type "text"
                   :value @search
                   :on-change #(on-change-search  (-> % .-target .-value))}]
   [:button {:on-click on-click-clear-search} "Clear search"]])

(defn app []
  [:div
   [search-input]
   [data-table]])

(def root (createRoot (js/document.getElementById "app")))

(defn run []
  (.render root [(r/as-element (app))])
  (get-list {})
  (get-genders))

(run)
