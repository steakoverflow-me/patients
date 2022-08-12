(ns patients.app
  (:require
   [patients.db :as db]

   [ring.adapter.jetty :refer [run-jetty]]
   [compojure.core :refer [GET ANY defroutes]]))

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
   :body (clojure.pprint/write (db/db-info) :stream nil)})

(defroutes app
  (GET "/"        request (page-index request))
  (GET "/db-info" request (page-db-info request))
  (ANY "/ping" _ {:status 200 :headers {"content-type" "text/plain"} :body "pong"})
  page-404)

(defn -main []
  (db/init-database)
  (run-jetty app {:port 8080 :join? true}))
