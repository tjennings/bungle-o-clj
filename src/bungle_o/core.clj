(ns bungle-o.core
  (:require [bungle-o.webapp :as web])
  (:gen-class :main true))

(defn -main [& args]
  (web/run))
