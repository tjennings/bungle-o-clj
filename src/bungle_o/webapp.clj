(ns bungle-o.webapp
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]
            [clojure.java.io :as cio]
            [bungle-o.airbrake :as ab]))

(defn slurp-stream [in]
  (slurp (java.io.InputStreamReader. in))) 

(defn prepare-message [body-stream]
  (let [body (slurp-stream body-stream)]
    {:valid (ab/validate body) :body body}))

(defroutes main-routes
  (GET "/" [] "<h1>Hello From Bungle-o!</h1>")
  (POST "/" [:as req] (println "Post to /" req))
  (POST "/notifier_api/v2/notices/" [:as req]
        (let [message (prepare-message (:body req))]
          (if (:valid message)
            (println "VALID\n\n" message) 
            (println "INVALID\n\n" message ))))
  (route/not-found "<h1>Page not found</h1>"))

(defn run []
  (run-jetty main-routes {:port 8080})) 

