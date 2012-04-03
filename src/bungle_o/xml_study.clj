(ns bungle-o.xml_study
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.zip :as zz]))

(def zipped 
  (let [input (java.io.FileInputStream. "samples/airbrake-sample.xml")]
     (zz/xml-zip (xml/parse input))))

;use xml1-> for these?
(defn exception-class []
  (zx/xml1-> zipped :error :class zx/text))

(defn exception-message []
  (zx/xml1-> zipped :error :message zx/text))

;Access to the root does not use the root tag
(defn notice-version []
  (zx/xml1-> zipped (zx/attr :version)))

(def backtrace-line-transform
  (fn [l] [{:number (zx/attr l :number) :file (zx/attr l :file) :method (zx/attr l :method)}]))

(defn backtrace []
  (zx/xml-> zipped :error :backtrace :line backtrace-line-transform))

(defn exception-map []
  {:class (exception-class) :message (exception-message) :backtrace (backtrace) :raw zipped})

