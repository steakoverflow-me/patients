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

(defn button
  ([content on-click] (button content on-click ""))
  ([content on-click alt] [:button.rounded.border-amber-700.border.bg-amber-500.text-white.font-bold.m-2.px-2.py-1 {:on-click on-click} content]))

(defn data-table-header []
  [:thead.bg-blueamber-100
   [:tr.py-2.border-b.border-amber-500 [:th.px-2 "ID"][:th.px-2 "Name"][:th.px-2 "Gender"][:th.px-2 "Birthdate"][:th.px-2 "Address"][:th.px-2 "OMS #"][:th.px-2]]
   [:tr.py-2.border-b-2.border-amber-700
    [:th.px-2]
    [:th.px-2 (filter-input :name {})]
    [:th.px-2 [:select.border-amber-700.border-2.rounded {:value (or (:gender_id @filters) "")
                   :on-change #(on-change-gender (-> % .-target .-value))}
          [:option {:value ""} "---"]
          (not-empty (for [item @genders]
                       [:option {:key (str "gender-id-" (item "id")) :value (item "id")} (item "name")]))]]
    [:th.px-2 [:div.flex.flex-col.justify-center
               [:div.my-1.border-amber-700.border-2.rounded-md
                [datepicker-dropdown
                 :show-today?   true
                 :start-of-week 0
                 :placeholder   "Date from..."
                 :format        "yyyy-mm-dd"
                 :model         filter-birthdate-from
                 :on-change     on-change-birthdate-from]]
               [:div.mb-1.border-amber-700.border-2.rounded-md
                [datepicker-dropdown
                 :show-today?   true
                 :start-of-week 0
                 :placeholder   "Date to..."
                 :format        "yyyy-mm-dd"
                 :model         filter-birthdate-to
                 :on-change     on-change-birthdate-to]]]]
    [:th.px-2 (filter-input :address {})]
    [:th.px-2 (filter-input :oms {:on-key-down #(when (not (is-numeric-or-special %))
                                             (.preventDefault %))})]
    [:th.px-2 (button "⌫" on-click-clear-filters "Clear filters")]]])

(defn data-table []
  [:div.container.border-amber-700.border-2.rounded.p-2
   [:table.border-collapse
    [data-table-header]
    [:tbody
     (not-empty (for [row @data]
                  [:tr.py-2.border-b.border-amber-500.last-child:border-0 {:key (str "row-id-" (row "id"))}
                   [:td.px-2 (str (row "id"))]
                   [:td.px-2 (row "name")]
                   [:td.px-2 (row "gender")]
                   [:td.px-2 (row "birthdate")]
                   [:td.px-2 (row "address")]
                   [:td.px-2 (row "oms")]
                   [:td.px-2]]))]]])

(defn search-input []
  [:div.container.mx-auto.flex.justify-end.px-0.py-2
   [:input.border-amber-700.border-2.rounded.px-2 {:type "text"
                                                   :placeholder "Search..."
                                                   :value @search
                                                   :on-change #(on-change-search  (-> % .-target .-value))}]
   (button "⌫" on-click-clear-search "Clear search")])

(defn app []
  [:div.container.mx-auto.p-4
   [search-input]
   [data-table]])

(def root (createRoot (js/document.getElementById "app")))

(defn run []
  (.render root [(r/as-element (app))])
  (get-list {})
  (get-genders))

(run)
