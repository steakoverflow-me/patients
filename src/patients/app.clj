(ns patients.app
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [patients.validation :refer [validate]]
   [clojure.data.json :as json]
   [clojure.string :refer [join]]
   [cljc.java-time.local-date :as ld]
   [cljc.java-time.format.date-time-formatter :refer [basic-iso-date]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
   [ring.util.response :refer [response resource-response header bad-request]]
   [compojure.core :refer [GET POST PUT DELETE ANY defroutes routes]]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [clojure.stacktrace :refer [print-stack-trace]]))

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

(defn wrap-content-json [h]
  (fn [req]
    (let [resp (h req)
          body (or (:body resp) {:result "OK"})
          body-resp (assoc resp :body body)
          headers-resp (assoc-in body-resp [:headers "Content-Type"] "application/json")]
      ;; (println (str "RESP:\t" headers-resp))
      headers-resp)))

(defn do-validated [f patient]
  (let [result (validate patient)]
    (if (empty? result)
      (f patient)
      (bad-request (json/write-str (join "\n" result))))))

(defn prepare-patient [p]
  (if (string? (:birthdate p)) (update p :birthdate #(ld/parse % basic-iso-date)) p))

(defroutes api
  (GET    "/patients"                      {params :params} (db/list-filtered params))
  (GET    ["/patients/:id", :id #"[0-9]+"] [id]             (db/get-one (Integer/parseInt id)))
  (POST   "/patients"                      req (do-validated db/insert! 
															 (prepare-patient (:body req))))
  (PUT    ["/patients/:id", :id #"[0-9]+"] req (do-validated db/update!
                                                             (prepare-patient (assoc (:body req) :id (Integer/parseInt (get-in req [:params :id]))))))
  (DELETE ["/patients/:id", :id #"[0-9]+"] [id]             (db/delete! (Integer/parseInt id)))

  (GET "/genders" [] (db/get-genders))

  page-404)

(def app
  (routes static (wrap-content-json (wrap-json-body (wrap-json-response (wrap-defaults api api-defaults)) {:keywords? true}))))

(def dataset-list
  (map
   (comp (fn [p] (update p :gender_id (fn [g-id] (Integer/parseInt g-id))))
         db/convert-birthdate-to-local-date)
   (:objects (json/read-json (slurp "dev/dataset.json")))))

(defn -main []
  (if (not= db-structure (db/db-info)) (db/init-database) nil)

  ;; (doseq [patient dataset-list] (db/insert! patient))

  (run-jetty app {:port 8080 :join? true}))
