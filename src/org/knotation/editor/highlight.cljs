(ns org.knotation.editor.highlight
  (:require [clojure.set :as set]

            [org.knotation.editor.line-map :as ln]
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
  [line-map source-ix editors]
  (apply clear-line-highlights! editors)
  (let [ed-from (get editors source-ix)
        ln-from (util/current-line ed-from)]
    (when-let [[ed-to-ix ln-to] (ln/lookup line-map source-ix ln-from)]
      (let [ed-to (if (= :out ed-to-ix) (last editors) (get editors ed-to-ix))]
        (highlight-line! ed-from ln-from)
        (highlight-line! ed-to ln-to)
        (util/scroll-into-view! ed-to :line ln-to)))))

(defn cross<->highlight!
  [line-map-atom editors]
  (.on (first editors) "cursorActivity"
       (fn [_] (cross->highlight! @line-map-atom 0 editors)))
  (.on (second editors) "cursorActivity"
       (fn [_] (cross->highlight! @line-map-atom 1 editors))))
