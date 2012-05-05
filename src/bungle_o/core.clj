(ns bungle-o.core
  (:use ring.adapter.jetty)
  (:require [bungle-o.webapp :as web]))

(defn -main [& args] (run-jetty web/main-routes {:port 8080}))
