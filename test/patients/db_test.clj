(ns patients.db-test
  (:require
   [patients.db :as db]
   [patients.config :refer [db-structure]]
   [patients.fixtures :refer [with-db]]
   [clojure.test :refer [deftest use-fixtures is]]))

(use-fixtures :once with-db)

(deftest test-db-init
  (is (= db-structure (db/db-info))))
