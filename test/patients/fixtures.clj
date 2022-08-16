(ns patients.fixtures
  (:require
   [patients.app :refer [app]]
   [patients.db :as db]
   [ring.adapter.jetty :refer [run-jetty]])
  (:import io.zonky.test.db.postgres.embedded.EmbeddedPostgres))

(declare test-port)

(defn with-server
  [f]
  (let [server (run-jetty app {:port 0 :join? false})
        port (-> server .getConnectors first .getLocalPort)]
    (with-redefs [test-port port]
      (try
        (f)
        (finally
          (.stop server))))))

(defn with-db
  [f]
  (let [pg (EmbeddedPostgres/start)
        pg-uri (format "postgres://localhost:%s/postgres?user=postgres" (.getPort pg))]
    (db/init-database)
    (try
      (f)
      (finally
        (.close pg)))))
