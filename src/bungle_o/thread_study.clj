(ns bungle-o.thread_study
  (:import (org.hornetq.core.config.impl ConfigurationImpl)
           (org.hornetq.api.core TransportConfiguration)
           (org.hornetq.core.remoting.impl.invm InVMAcceptorFactory
                                                InVMConnectorFactory)
           (org.hornetq.core.server HornetQServers
                                    HornetQServer)
           (org.hornetq.api.core.client HornetQClient)))

(def state (atom {:consumers {} :producers {}}))

(defn memoize! [k f]
  (if (not (k state))
    (swap! state assoc k (f)))
  (k @state))

(defn config []
  (memoize! :config (fn []
    (let [config (ConfigurationImpl.)]
        (doto config
              (.setPersistenceEnabled false)
              (.setSecurityEnabled false))
        (doto (.getAcceptorConfigurations config)
              (.add (TransportConfiguration. (.getName InVMAcceptorFactory))))
        config)))) 

(defn server [config]
  (memoize! :server (fn []
    (HornetQServers/newHornetQServer config))))

(defn session-factory []
  (memoize! :session-factory (fn []
    (let [trans-conf (TransportConfiguration. (.getName InVMConnectorFactory))
          server-locator (HornetQClient/createServerLocatorWithoutHA (into-array [trans-conf]))
          sf (.createSessionFactory server-locator)]
      sf))))

(defn session []
  (memoize! :session (fn []
    (let [session (.createSession (session-factory))]
      (.start session)
      session))))

(defn conditionally-start-server []
  (if-not (:server @state)
    (.start (server (config)))))

(defn queue [name]
  (conditionally-start-server)
  (.createQueue (session) name name))

(defn shutdown-queues []
  (if-let [sf(:session-factory @state)]
    (.close sf))
  (if-let [server (:server @state)]
    (.stop server)))

(defn consumer [name]
  (.createConsumer (session) name))

(defn producer [name]
  (.createProducer (session) name))

(defn qpush [name message]
  (let [producer (producer name)
        messageObj (.createMessage (session) false)
        _ (.putStringProperty messageObj "key" message)]
    (.send producer messageObj)))

(defn qpop [name]
  (let [consumer (consumer name)]
    (if-let [msg (.receive consumer 10)]
      (.getStringProperty msg "key"))))

;TODO
;collect consumers/producers and shut them down on exit

(queue "my-queue")
(qpush "my-queue" "I'm sending a message!")
(println (qpop "my-queue")) 
(shutdown-queues)

;  (qpush "Queue" "message")
;
;(qpop "Queue")
;
;  (qpeek "Queue)
;
;  (qwork "Queue" {:threads 20}
;    (fn [msg] ...))
;  (shutdown-queues)
