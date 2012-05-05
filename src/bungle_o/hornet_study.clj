(ns bungle-o.thread_study
  (:import (org.hornetq.core.config.impl ConfigurationImpl)
           (org.hornetq.api.core TransportConfiguration)
           (org.hornetq.core.remoting.impl.invm InVMAcceptorFactory
                                                InVMConnectorFactory)
           (org.hornetq.core.server HornetQServers
                                    HornetQServer)
           (org.hornetq.api.core.client HornetQClient
                                        MessageHandler)
           (org.hornetq.api.core SimpleString)))

(def state (ref {}))
(def consumers (ref {}))
(def producers (ref {}))

(defn- memoize! [k f]
  (dosync 
    (if (not (k @state))
      (alter state assoc k (f))) 
    (k @state)))

(defn- config []
  (memoize! :config (fn []
    (let [config (ConfigurationImpl.)]
        (doto config
              (.setPersistenceEnabled false)
              (.setSecurityEnabled false))
        (doto (.getAcceptorConfigurations config)
              (.add (TransportConfiguration. (.getName InVMAcceptorFactory))))
        config)))) 

(defn- server [config]
  (memoize! :server (fn []
    (HornetQServers/newHornetQServer config))))

(defn- session-factory []
  (memoize! :session-factory (fn []
    (let [trans-conf (TransportConfiguration. (.getName InVMConnectorFactory))
          server-locator (HornetQClient/createServerLocatorWithoutHA (into-array [trans-conf]))
          sf (.createSessionFactory server-locator)]
      sf))))

(defn- session []
  (memoize! :session (fn []
    (let [session (.createSession (session-factory) true true 1)]
      (.start session)
      session))))

(defn- conditionally-start-server []
  (if-not (:server @state)
    (.start (server (config)))))

(defn- shutdown-queues []
  (if-let [sf (:session-factory @state)]
    (.close sf))
  (if-let [server (:server @state)]
    (.stop server)))

(defn- consumer [name]
  (dosync
    (if-let [consumer (get @consumers name)]
      consumer
      (let [consumer (.createConsumer (session) name)]
        (alter consumers assoc name consumer)
        consumer))))

(defn- message-handler [a-fn]
  (reify MessageHandler
    (onMessage [this msg] (a-fn msg) (.acknowledge msg))))

(defn- producer [name]
  (dosync 
    (if-let [producer (get @producers name)]
      producer
      (let [producer (.createProducer (session) name)]
        (alter producers assoc name producer)
        producer))))

;
; API starts here
;

(defn queue [name]
  (conditionally-start-server)
  ;cargo culted from the example, really have no idea why we need to create a "core session"
  ;this code works using the cached session
  (let [core-session (.createSession (session-factory) false false false)]
    (.createQueue core-session name name true)
    (.close core-session)))

(def num-processors
  (.. Runtime getRuntime availableProcessors))

(defn qcount [name]
  (let [core-session (.createSession (session-factory) false false false)]
    (try 
      (.getMessageCount (.queueQuery core-session (SimpleString. name))) 
    (finally (.close core-session)))))

(defn qpush [name message]
  (let [producer (producer name)
        messageObj (.createMessage (session) false)
        _ (.putStringProperty messageObj "key" message)]
    (.send producer messageObj)))

(defn qpop [name]
  (let [consumer (consumer name)]
    (if-let [msg (.receive consumer 10)]
      (.getStringProperty msg "key"))))

(defn qwork [name a-fn]
  ;Fucking MAGIC! createSession sig is (autocommit-ack, autocommit-send, ack batch size)
  (let [session (.createSession (session-factory) true true 1)] 
    (dotimes [i (+ 2 num-processors)]
      (let [consumer (.createConsumer session name)]
        (.setMessageHandler consumer (message-handler (fn [msg] (a-fn msg))))))
       (.start session)))

;TODO
;getting hornet to write to something besides stdout/stderr?
;configurable persistent queing 

; Example
(queue "my-queue") ;create our queue

(dotimes [i 20] ;push 20 messages
  (qpush "my-queue" (str "I'm sending a message " i)))

(qwork "my-queue" ;create 2k threads and consume from the queue
       (fn [msg] (println (.getStringProperty msg "key"))))

(while (> (qcount "my-queue") 0) ;wait for the queue to empty, then exit
  (println (qcount "my-queue"))
  (Thread/sleep 100))
(println "QUEUE EMPTIED" (qcount "my-queue"))
(shutdown-queues)
