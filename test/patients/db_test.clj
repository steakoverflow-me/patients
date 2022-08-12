(ns patients.db-test
  (:require
   [patients.db :as db]
   [patients.fixtures :refer [with-server test-port]]
   [clojure.test :refer [deftest use-fixtures]]))

