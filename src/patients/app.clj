(ns patients.app
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [patients.validation :refer [do-validated]]
   [clojure.data.json :as json]
   [cljc.java-time.local-date :as ld]
   [cljc.java-time.format.date-time-formatter :refer [basic-iso-date]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :refer [response resource-response header]]
   [compojure.core :refer [GET POST PUT DELETE ANY defroutes routes]]
   [compojure.route :as route]
   [compojure.handler :as handler]))

(defn page-404 [request]
  {:status 404
   :headers {"content-type" "text/plain"}
   :body "Page not found."})

(defn page-db-info [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (pr-str (db/db-info))})

(defroutes static
  (GET "/"        [] (-> (resource-response "index.html" {:root "public"})
                         (header "Content-Type" "text/html; charset=utf-8")))
  (route/resources "/")
  (GET "/db-info" request (page-db-info request))
  (ANY "/ping" _ {:status 200 :headers {"content-type" "text/plain"} :body "pong"}))

(defroutes api
  (GET    "/patients"                      {params :params} (db/list-filtered params))
  (GET    ["/patients/:id", :id #"[0-9]+"] [id]             (db/get-one (Integer/parseInt id)))
  (POST   "/patients"                      [request]        (do-validated db/insert! (:body request)))
  (PUT    ["/patients/:id", :id #"[0-9]+"] {params :params body :body} (do-validated db/update! (update (assoc body :id (Integer/parseInt (:id params))) :birthdate #(ld/parse % basic-iso-date))))
  (DELETE ["/patients/:id", :id #"[0-9]+"] [id]             (db/delete! (Integer/parseInt id)))

  (GET "/genders" [] (db/get-genders))

  page-404)

(defn wrap-content-json [h]
  (fn [req] (assoc-in (h req) [:headers "Content-Type"] "application/json")))

(def app
  (routes static (wrap-content-json (wrap-json-body (wrap-json-response (wrap-defaults api api-defaults)) {:keywords? true}))))

(def dataset-list
  (map
   (comp (fn [p] (update p :gender_id (fn [g-id] (Integer/parseInt g-id))))
         db/convert-birthdate-to-local-date)
   (:objects (json/read-json (slurp "dev/dataset.json")))))

(defn -main []
  (if (not= db-structure (db/db-info)) (db/init-database) nil)

  ;;(doseq [patient dataset-list] (db/insert! patient))

  (run-jetty app {:port 8080 :join? true}))
