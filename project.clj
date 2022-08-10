(defproject patients "0.1.0-SNAPSHOT"
  :description "Test app"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]

                 [ring/ring-core "1.9.5"]
                 [ring/ring-jetty-adapter "1.9.5"]

                 [compojure "1.7.0"]]

  :main patients.app

  :profiles {:dev  {:plugins []
                    :dependencies []
                    :source-paths ["dev"]}
             :repl {:plugins [[cider/cider-nrepl "0.28.4"]
                              [mx.cider/enrich-classpath "1.9.0"]]}})
