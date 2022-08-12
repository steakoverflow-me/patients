(defproject patients "0.1.0-SNAPSHOT"
  :description "Test app"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-cloverage "1.2.4"]]

  :dependencies [[org.clojure/clojure "1.9.0"]

                 [org.clojure/tools.logging "1.2.4"]

                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]

                 [compojure "1.7.0"]

                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.4.1"]]

  :main patients.app

  :aliases {"test" ["cloverage" "--runner" :eftest]}

  :profiles {:dev  {:source-paths ["dev"]}

             :test {:dependencies [[clj-http "3.12.3"]
                                   [eftest "0.5.9"]
                                   [org.clojure/test.check "1.1.1"]]}

             :repl [:test
                    {:plugins [[cider/cider-nrepl "0.28.4"]
                               [mx.cider/enrich-classpath "1.9.0"]]}]})
