(defproject patients "0.1.0-SNAPSHOT"
  :description "Test app"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[lein-cloverage "1.2.4"]
            [lein-cljsbuild "1.1.8"]
            [lein-eftest "0.5.9"]]

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.logging "1.2.4"]

                 [org.clojure/clojurescript "1.11.60"]
                 [reagent "1.1.1"]
                 [org.clojars.frozenlock/reagent-modals "0.2.8"]
                 [re-com "2.13.2"]
                 [cljsjs/react "18.2.0-0"]
                 [cljsjs/react-dom "18.2.0-0"]
                 [cljs-ajax "0.7.5"]

                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]
                 [ring/ring-defaults "0.3.3"]
                 [ring/ring-json "0.5.1"]

                 [compojure "1.7.0"]

                 [org.clojure/java.jdbc "0.7.12"]
                 [org.postgresql/postgresql "42.4.1"]

                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.widdindustries/cljc.java-time "0.1.21"]]

  :main patients.app

  :aliases {"unit-tests" ["cloverage"]
            "integration-tests" ["eftest" "patients.integration.test"]

            "test-all" ["do" ["unit-tests"] ["integration-tests"]]}

  :profiles {:uberjar {:main           patients.app
                       :aot            [patients.app]
                       :omit-source    true
                       :resource-paths ["resources/public"]
                       :uberjar-name   "patients.jar"
                       :prep-tasks     ["compile"]}

             :dev     {:source-paths ["dev"]}

             :test    {:dependencies [[clj-http "3.12.3"]
                                      [eftest "0.5.9"]
                                      [org.clojure/test.check "1.1.1"]
                                      [io.zonky.test/embedded-postgres "2.0.1"]
                                      [com.google.guava/guava "31.1-jre"]
                                      [webdriver "0.17.1"]]
                       :cloverage {:runner :eftest
                                   :test-ns-regex [#"^((?!integration).)*$"]}}}
  :cljsbuild {:builds
              [{:source-paths ["src/patients/script"]
                :jar true
                :compiler {:main "patients.script.app"
                           :output-dir "resources/public"
                           :asset-path ""
                           :output-to "resources/public/out/app.js"
                           :optimizations :none
                           :source-map true}}]}

  :hooks [leiningen.cljsbuild])
