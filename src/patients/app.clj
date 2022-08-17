(ns patients.app
  (:require
   [patients.db :as db]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [compojure.core :refer [GET ANY defroutes]]
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
  (GET "/list"    {params :params} (db/list-filtered 'params))
  page-404)

(defn -main []
  (db/init-database)
  (run-jetty (wrap-defaults app api-defaults) {:port 8080 :join? true}))
