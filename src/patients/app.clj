(ns patients.app
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [patients.validation :refer [do-validated]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.util.response :refer [response]]
   [compojure.core :refer [GET POST PUT DELETE ANY defroutes]]
   [compojure.handler :as handler]))

(defn page-index [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "Application index!"})

(defn page-404 [request]
  {:status 404
   :headers {"content-type" "text/plain"}
   :body "Page not found."})

(defn page-db-info [request]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (pr-str (db/db-info))})

(defroutes app
  (GET "/"        request (page-index request))
  (GET "/db-info" request (page-db-info request))
  (ANY "/ping" _ {:status 200 :headers {"content-type" "text/plain"} :body "pong"})

  (GET    "/patients"     {params :params} (db/list-filtered 'params))
  (GET    "/patients/:id" [id]             (db/get-one id))
  (POST   "/patients"     [request]        (do-validated db/insert! (:body request)))
  (PUT    "/patients/:id" [id request]     (do-validated db/update! (assoc (:body request) :id id)))
  (DELETE "/patients/:id" [id]             (response (db/delete! id)))

  page-404)

(defn wrap-content-json [h]
  (fn [req] (assoc-in (h req) [:headers "Content-Type"] "application/json")))

(defn -main []
  (if (not= db-structure (db/db-info)) (db/init-database) nil)
  (run-jetty (wrap-content-json (wrap-json-response (wrap-defaults app api-defaults))) {:port 8080 :join? true}))
