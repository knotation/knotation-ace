(ns org.knotation.editor.highlight
  (:require [clojure.set :as set]

            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.util :as util]))

(defn clear-line-highlights!
  ([eds] (clear-line-highlights! eds ["highlight" "current-subject"]))
  ([eds classes]
   (doseq [e eds
           i (util/line-range e)]
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
  (clear-line-highlights! editors)
  (let [ed-from (if (= :out source-ix) (last editors) (get editors source-ix))
        ln-from (util/current-line ed-from)]
    (doseq [[ed-to-ix ln-to] (ln/lookup line-map source-ix ln-from)]
      (let [ed-to (if (= :out ed-to-ix) (last editors) (get editors ed-to-ix))]
        (highlight-line! ed-from ln-from)
        (highlight-line! ed-to ln-to)
        (util/scroll-into-view! ed-to :line ln-to)))))

(defn cross<->highlight!
  [line-map-atom editors]
  (let [setup (fn [ed ix]
                (.on ed "cursorActivity" (fn [_] (when (.hasFocus ed) (cross->highlight! @line-map-atom ix editors))))
                (.on ed "focus" (fn [_] (cross->highlight! @line-map-atom ix editors))))]
    (doseq [[ix e] (map-indexed vector (butlast editors))] (setup e ix))
    (setup (last editors) :out)))
