(ns bungle-0.static-call
  (:import (org.hornetq.api.core.client HornetQClient)
           (org.hornetq.api.core DiscoveryGroupConfiguration)
           (org.hornetq.core.remoting.impl.invm InVMAcceptorFactory
                                                InVMConnectorFactory)
           (org.hornetq.api.core TransportConfiguration)))

(require '[clojure.reflect :as r])
(use '[clojure.pprint :only [print-table]])

(print-table (:members (r/reflect HornetQClient)))
(HornetQClient/createServerLocatorWithoutHA (DiscoveryGroupConfiguration. "" 1))
(HornetQClient/createServerLocatorWithoutHA (into-array [ (TransportConfiguration. (.getName InVMConnectorFactory))]))
