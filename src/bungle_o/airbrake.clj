(ns bungle-o.airbrake
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zz]))

(defn notice-version [zipped]
  (zx/xml1-> zipped (zx/attr :version)))

(defn validate [message]
  (let [zip (zz/xml-zip (xml/parse-str message))
        notice (notice-version zip)]
    (not (nil? notice))))
