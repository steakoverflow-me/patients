(ns patients.web-test
  (:require
    [patients.app :refer [app]]
    [ring.adapter.jetty :refer [run-jetty]]
    [clj-http.client :as http]
    [clojure.test :refer [deftest use-fixtures is]]))

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

(use-fixtures :each with-server)

(defn url [relative]
  (str "http://localhost:" test-port relative))

(defn http-get [relative]
  (http/get (url relative) {:throw-exceptions false}))

(deftest test-static
  (is (= "pong" ((http-get "/ping") :body)))
  (is (= "Application index!" ((http-get "") :body))))

(deftest test-404
  (is (= "Page not found." ((http-get "/random") :body))))
