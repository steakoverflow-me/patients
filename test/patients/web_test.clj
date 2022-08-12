(ns patients.web-test
  (:require
   [patients.fixtures :refer [with-server test-port]]
   [clj-http.client :as http]
   [clojure.test :refer [deftest use-fixtures is]]
   [clojure.test.check.generators :as gen]))

(use-fixtures :once with-server)

(defn url [relative]
  (str "http://localhost:" test-port relative))

(defn http-req [method relative]
  (let [req-fn (case (.toLowerCase method)
                 "get"    (partial http/get)
                 "post"   (partial http/post)
                 "put"    (partial http/put)
                 "delete" (partial http/delete)
                 "head"   (partial http/head)
                 (throw (IllegalArgumentException. (format "Wrong HTTP method $s" method))))]
    (req-fn (url relative) {:throw-exceptions false})))
()
(defn http-get [relative]
  (http-req "GET" relative))

(deftest test-ping
  (is (= "pong" ((http-req "get"    "/ping") :body)))
  (is (= "pong" ((http-req "post"   "/ping") :body)))
  (is (= "pong" ((http-req "put"    "/ping") :body)))
  (is (= "pong" ((http-req "delete" "/ping") :body)))
  (is (= (str (.length "pong")) ((:headers (http-req "head" "/ping")) "content-length"))))

(deftest test-static
  (is (= "Application index!" ((http-get "/") :body))))

(defn generate-string-except-routes []
  (let [predicate (fn [in]
                    (= (some #{in} ["" "ping"]) nil))]
        (gen/sample (gen/such-that predicate gen/string-alphanumeric))))

(deftest test-404
  (def u (str "/" (first (generate-string-except-routes))))
  (is (= 404 ((http-get u) :status))))
