(defproject bungle-o "1.0.0-SNAPSHOT"
            :main bungle-o.core
            :ring {:handler bungle-o.webapp/handler}
            :description "FIXME: write description"
            :dependencies [[compojure "1.0.1"]
                           [ring "1.0.2"]
                           [org.clojure/clojure "1.3.0"]
                           [org.clojure/data.zip "0.1.1"]
                           [org.clojure/data.xml "0.0.3"]
                           [org.hornetq/hornetq-core "2.2.16.Final"]]
            :plugins [[lein-beanstalk "0.2.2"]])
