(ns patients.script.app
  (:require
   [patients.validation :as v]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [reagent-modals.modals :as rmodals]
   [re-com.core :refer [datepicker-dropdown]]
   ["react-dom/client" :refer [createRoot]]
   [cljc.java-time.local-date :as ld]
   [cljs-time.coerce :as coerce]
   [clojure.string :as str]
   [goog.string :as gstring]
   [ajax.core :refer [GET POST PUT DELETE json-request-format]])
  (:require-macros [patients.script.macros :refer [filter-input form-input]]))

;; State

(defonce genders (r/atom {}))

(defonce data (r/atom []))

(defonce search (r/atom ""))

(defonce filter-birthdate-from (r/atom nil))

(defonce filter-birthdate-to (r/atom nil))

(defonce filters-init {:name nil :gender_id nil :address nil :oms nil})

(defonce filters (r/atom filters-init))

(defonce patient-init {:name nil :gender_id nil :birthdate nil :address nil :oms nil})

(defonce patient (r/atom patient-init))

(defonce errors (r/atom patient-init))

(defonce is-edit (r/atom false))

;; For tests

(defonce birthdate-input-datepicker (r/atom nil))

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

(defn ld-to-iso-str [ld]
  (.substring (ld/to-string ld) 0 8))

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

(defn clear-patient []
  (reset! patient patient-init))

(defn clear-patient-errors []
  (reset! errors patient-init))

;; API functions

(defn get-list
  ([] (get-list (get-filters)))
  ([fs] (GET "/patients" {:params fs
                          :handler #(reset! data %)
                          :error-handler error-handler})))
(defn get-one [id f]
  (GET (str "/patients/" id) {:handler #(f (first %))
                              :error-handler error-handler}))

(defn get-genders []
  (GET "/genders" {:handler #(reset! genders %)
                   :error-handler error-handler}))

(defn create-patient [p f]
  (let [bd-str (ld-to-iso-str (:birthdate p))]
    (POST "/patients" {:params (assoc p :birthdate bd-str)
                       :format (json-request-format)
                       :handler f
                       :error-handler error-handler})))

(defn update-patient [p f]
  (let [bd-str (ld-to-iso-str (:birthdate p))]
    (PUT (str "/patients/" (:id p)) {:params (assoc p :birthdate bd-str)
                                     :format (json-request-format)
                                     :handler f
                                     :error-handler error-handler})))

(defn delete-patient [id f]
  (DELETE (str "/patients/" id) {:handler f
                                 :error-handler error-handler}))

;; Validation

(defn validate []
  (let [bd (if (nil? (:birthdate @patient)) nil (ld/parse (subs (coerce/to-string (:birthdate @patient)) 0 10)))
		es {:name      (v/validate-name (or (:name @patient) ""))
            :gender_id (v/validate-gender-id (:gender_id @patient))
            :birthdate (v/validate-birthdate bd)
            :address   (v/validate-address (or (:address @patient) ""))
            :oms       (v/validate-oms (or (:oms @patient) ""))}]
    (reset! errors es)))

;; Event handler functions

(defn error-handler [e]
  (println "error-handler" e)
  (rmodals/modal! (notification (gstring/unescapeEntities (or (:status-text e) "Error")) (gstring/unescapeEntities (or (:response e) "No information :(")))))

(defn on-change-gender-filter [gender-id]
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

(defn on-click-patient-create []
  (clear-patient)
  (clear-patient-errors)
  (reset! is-edit true))

(defn on-click-patient-edit [id]
  (get-one id #((reset! patient {:id (% "id")
                                 :name (% "name")
                                 :gender_id (% "gender_id")
                                 :birthdate (coerce/from-string (% "birthdate"))
                                 :address (% "address")
                                 :oms (% "oms")})
                (reset! is-edit true))))

(defn on-click-patient-delete [id]
  (rmodals/modal! (patient-delete-confirmation id)))

;; Reagent components

(def input :input.border-amber-700.border-2.rounded.px-2)

(defn button
  ([content on-click] (button content on-click {}))
  ([content on-click props] [:button.rounded.border-amber-700.border.bg-amber-500.text-white.font-bold.m-2.px-2.py-1 (merge {:type "button" :on-click on-click} props) content]))

(defn data-table-header []
  [:thead.bg-blueamber-100
   [:tr.py-2.border-b.border-amber-500 [:th.px-2 "ID"][:th.px-2 "Name"][:th.px-2 "Gender"][:th.px-2 "Birthdate"][:th.px-2 "Address"][:th.px-2 "OMS #"][:th.px-2]]
   [:tr.py-2.border-b-2.border-amber-700
    [:th.px-2]
    [:th.px-2 (filter-input :name {:id "name-filter"})]
    [:th.px-2 [:select#gender-filter.border-amber-700.border-2.rounded {:value (or (:gender_id @filters) "")
                                                          :on-change #(on-change-gender-filter (-> % .-target .-value))}
               [:option {:value ""} "---"]
               (not-empty (for [item @genders]
                            [:option {:key (str "gender-id-" (item "id")) :value (item "id")} (item "name")]))]]
    [:th.px-2 [:div#birthdate-filters.flex.flex-col.justify-center
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
    [:th.px-2 (filter-input :address {:id "address-filter"})]
    [:th.px-2 (filter-input :oms {:on-key-down #(when (not (is-numeric-or-special %))
                                             (.preventDefault %))
                                  :id "oms-filter"})]
    [:th.px-2
     [:div.flex.justify-end
      (button "⌫" on-click-clear-filters {:title "Clear filters"
                                           :id "clear-filters-button"})]]]])

(defn notification [header content]
  [:div.flex.flex-col.justify-between
   [:div.flex.justify-center.bg-amber-500.p-2.rounded-t.text-white.text-lg.font-bold header]
   [:div.container.p-4
    [:div.pb-4.flex.justify-center content]
    [:hr]
    [:div.flex.justify-end.pt-2
     (button "Ok" #(do) {:data-dismiss "modal"})]]])

(defn patient-delete-confirmation [id]
  [:div.flex.flex-col.justify-between
   [:div.flex.justify-center.bg-amber-500.p-2.rounded-t.text-white.text-lg.font-bold "Delete"]
   [:div.container.p-4
    [:div.pb-4.flex.justify-center
     [:dev.text-2xl.font-bold (gstring/format "Delete patient #%s?" id)]]
    [:hr]
    [:div.flex.justify-end.pt-2
     (button "Delete" #(delete-patient id get-list) {:data-dismiss "modal"})
     (button "Cancel" #(do) {:data-dismiss "modal"
                             :class "bg-white text-amber-600"})]]])

(defn patient-edit-dialog []
  (when @is-edit
    [:div.flex.flex-col.justify-between.border-amber-700
     [:div.flex.justify-center.border-b-2.border-amber700.rounded.p-2.rounded-t.text-amber-500.text-lg.font-bold
      (if (some? (:id @patient)) (str "Edit patient #" (:id @patient)) "New patient")]
     [:div.container.p-4.flex.flex-col.justify-between
      (form-input :name {:placeholder "Name..." :id "name-input"})
      [:div.flex.justify-between.p-3
       [:div.flex.flex-col.justify-center
        [:div.flex.justify-center
         [:div.flex.flex-col.justify-center.font-bold "Gender:"]
         [:select#gender-select.border-amber-700.border-2.rounded.m-2 {:value (or (:gender_id @patient) "")
                                                         :on-change #((swap! patient assoc :gender_id (js/parseInt (-> % .-target .-value)))(validate))}
          [:option {:value ""} "---"]
          (not-empty (for [item @genders]
                       [:option {:key (str "gender-id-" (item "id"))
                                 :value (item "id")}
                        (item "name")]))]]
        [:div.text-xs.text-red-400 (:gender_id @errors)]]
       [:div.flex.flex-col.justify-center
        [:div#birthdate-input.my-1.border-amber-700.border-2.rounded-md
         [datepicker-dropdown
          :show-today?   true
          :start-of-week 0
          :placeholder   "Birthdate..."
          :format        "yyyy-mm-dd"
          :model         (:birthdate @patient)
          :on-change     #((swap! patient assoc :birthdate (ts-to-date %))(validate))]]
        [:div.text-xs.text-red-400 (:birthdate @errors)]]]
       (form-input :address {:placeholder "Address..." :id "address-input"})
       (form-input :oms {:placeholder "OMS #"
                         :id "oms-input"
                         :on-key-down #(when (not (is-numeric-or-special %))
                                         (.preventDefault %))})
      [:hr]
      [:div.flex.justify-end.pt-2
       (button "Save"
               (fn []
                 (let [callback #((reset! is-edit false)
                                  (clear-patient)
                                  (clear-patient-errors)
                                  (get-list))]
                   (if (some? (:id @patient))
                     (update-patient @patient callback)
                     (create-patient @patient callback))))
               {:data-dismiss "modal"
                :disabled (not-every? nil? (vals @errors))
                :class (when (not-every? nil? (vals @errors)) "bg-amber-200")
                :id "save-button"})
       (button "Cancel"
               (fn []
                 (reset! is-edit false)
                 (clear-patient)
                 (clear-patient-errors))
               {:data-dismiss "modal"
                :class "bg-white text-amber-600"
                :id "cancel-button"})]]]))

(defn data-table-actions-cell [id]
  [:div.flex.justify-end
   (button "➙" #(on-click-patient-edit id) {:title "Edit patient"
                                             :id (str "edit-patient-" id "-button")})
   (button "⌫" #(on-click-patient-delete id) {:title "Delete patient"
                                               :id (str "delete-patient-" id "-button")})])

(defn data-table []
  [:div.container.border-amber-700.border-2.rounded.p-2
   [:table#data-table.border-collapse
    [data-table-header]
    [:tbody
     (not-empty (for [row @data]
                  [:tr.py-2.border-b.border-amber-500.last-child:border-0 {:key (str "row-id-" (row "id"))}
                   [:td.px-2 [:div.flex.justify-end (str (row "id"))]]
                   [:td.px-2 [:div.flex.justify-start (row "name")]]
                   [:td.px-2 [:div.flex.justify-start (row "gender")]]
                   [:td.px-2 [:div.flex.justify-end (row "birthdate")]]
                   [:td.px-2 [:div.flex.justify-start (row "address")]]
                   [:td.px-2 [:div.flex.justify-end (str/join "-" (re-seq #"\d{1,3}" (str (row "oms"))))]]
                   [:td.px-2 (data-table-actions-cell (row "id"))]]))]]])

(defn top-block []
  [:div.container.w-full.flex.justify-between.px-0.py-2
   (button "New patient..." on-click-patient-create {:id "new-patient-button"})
   [:div.flex.justify-center
    [:input#search-input.border-amber-700.border-2.rounded.px-2 {:type "text"
                                                    :placeholder "Search..."
                                                    :value @search
                                                    :on-change #(on-change-search  (-> % .-target .-value))}]
    (button "⌫" on-click-clear-search {:title "Clear search"
                                       :id "clear-search-button"})]])

(defn app []
  [:div.container.mx-auto.p-4
   [patient-edit-dialog]
   [top-block]
   [data-table]
   [rmodals/modal-window]])

(def root (createRoot (js/document.getElementById "app")))

(defn run []
  (.render root [(r/as-element (app))])
  (get-list {})
  (get-genders))

(run)
