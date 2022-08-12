(ns patients.fixtures
  (:require
   [patients.app :refer [app]]
   [ring.adapter.jetty :refer [run-jetty]]))

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
