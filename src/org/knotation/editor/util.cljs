(ns org.knotation.editor.util
  (:import [goog.async Debouncer])
  (:require [cljsjs.codemirror]))

(defn dom-loaded [f] (.addEventListener js/document "DOMContentLoaded" f))

(defn tap!
  ([value] (.log js/console (clj->js value)) value)
  ([prefix value] (.log js/console prefix " -- " (clj->js value)) value))

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
  (range 0 (.lineCount ed)))

(defn scroll-into-view!
  [ed & {:keys [line ch margin] :or {ch 0 margin 0}}]
  (.scrollIntoView ed (clj->js {:line line :ch ch}) margin))

(defn zip [& lists]
  (apply map (fn [& args] (vec args)) lists))

(defn set-tree
  [selector data]
  (.treeview (js/$ selector) (clj->js {:data data})))
