(ns org.knotation.editor.util
  (:import [goog.async Debouncer])
  (:require [clojure.string :as string]
  	        [cljsjs.codemirror]

            [org.knotation.util :as util]
            [org.knotation.environment :as en]
            [org.knotation.state :as st]

            [org.knotation.kn :as kn]
            [org.knotation.ttl :as ttl]
            [org.knotation.nq :as nq]
            [org.knotation.tsv :as tsv]))

(defn graph-end? [s] (= ::st/graph-end (::st/event s)))
(defn error? [s] (= ::st/error (::st/event s)))

(defn error-message [s] (->> s ::st/error ::st/error-message))
(defn error-type [s] (->> s ::st/error ::st/error-type))

(defn line-num-in [s] (->> s ::st/input ::st/start ::st/line-number))
(defn line-ct-in [s]
  (inc
   (- (->> s ::st/input ::st/end ::st/line-number)
      (line-num-in s))))

(defn line-num-out [s] (->> s ::st/output ::st/start ::st/line-number))
(defn line-ct-out [s]
  (inc
   (- (->> s ::st/output ::st/end ::st/line-number)
      (line-num-in s))))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn current-line
  [ed]
  (.-line (.getCursor ed)))

(defn env-of
  [all]
  (::en/env (last all)))

(defn mode-of
  [ed]
  (.-name (.getMode ed)))
(def modeOf mode-of)

(defn format-of
  [ed]
  (->> ed .-knotation .-format))
(def formatOf format-of)

(defn knotation-mode?
  [ed]
  (= "knotation" (mode-of ed)))
(def knotationModeP knotation-mode?)

(defn line-range
  [ed]
  (range 0 (.lineCount ed)))

(defn reset-line-count
  [state]
  (assoc-in state [::st/location ::st/line-number] 0))

(defn zip [& lists]
  (apply map (fn [& args] (vec args)) lists))

(defn get-state-at
  ([states line]
   (->> states
       (filter #(contains? % ::st/input))
       (filter #(< (->> % ::st/input ::st/start ::st/line-number) line))
       first))
  ([states line col]
   (->> states
       (filter #(contains? % ::st/input))
       (filter #(< (->> % ::st/input ::st/start ::st/line-number) line))
       (filter #(< (->> % ::st/input ::st/start ::st/column-number) col))
       first)))
