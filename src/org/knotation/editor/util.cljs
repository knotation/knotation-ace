(ns org.knotation.editor.util
  (:import [goog.async Debouncer])
  (:require [cljsjs.codemirror]))

(defn dom-loaded [f] (.addEventListener js/document "DOMContentLoaded" f))

(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

(defn current-line
  [ed]
  (.-line (.getCursor ed)))

(defn mode-of
  [ed]
  (.-name (.getMode ed)))
(def modeOf mode-of)

(defn knotation-mode?
  [ed]
  (= "knotation" (mode-of ed)))
(def knotationModeP knotation-mode?)

(defn line-range
  [ed]
  (range 0 (- (.lineCount ed) 1)))

(defn scroll-into-view!
  [ed & {:keys [line ch] :or {ch 0}}]
  (.scrollIntoView ed (clj->js {:line line :ch ch})))
