(ns org.knotation.editor.highlight
  (:require [clojure.set :as set]

            [org.knotation.editor.util :as util]))

(defn clear-line-highlights!
  [& eds]
  (doseq [e eds]
    (doseq [i (util/line-range e)]
      (.removeLineClass e i "background"))))
(def clearLineHighlights clear-line-highlights!)

(defn highlight-line!
  ([ed line] (highlight-line! ed line "highlight"))
  ([ed line class] (.addLineClass ed line "background" class)))
(def highlightLine highlight-line!)

(defn highlight-by-subject!
  [editor line]
  (when (util/knotation-mode? editor)
    (letfn [(handle-of [ln] (.getLineHandle editor ln))
            (subject-of [ln] (:subject @(.-stateAfter (handle-of ln))))
            (blank? [ln] (empty? (.-text (handle-of ln))))]
      (if-let [subject (and (not (blank? line)) (subject-of line))]
        (doseq [i (util/line-range editor)]
          (when (and (= subject (subject-of i)) (not (blank? i)))
            (highlight-line! editor i "current-subject")))))))

(defn subject-highlight-on-move!
  [ed]
  (.on ed "cursorActivity" (fn [ed] (highlight-by-subject! ed (util/current-line ed)))))

(defn cross->highlight!
  [line-map editor-a editor-b]
  (clear-line-highlights! editor-a editor-b)
  (let [ln-from (util/current-line editor-a)]
    (when-let [ln-to (get line-map ln-from)]
      (highlight-line! editor-a ln-from)
      (highlight-line! editor-b ln-to)
      (util/scroll-into-view! editor-b :line ln-to))))

(defn cross<->highlight!
  [line-map-atom editor-a editor-b]
  (.on editor-a "cursorActivity"
       (fn [_] (cross->highlight! @line-map-atom editor-a editor-b)))
  (.on editor-b "cursorActivity"
       (fn [_] (cross->highlight! (set/map-invert @line-map-atom) editor-b editor-a))))
