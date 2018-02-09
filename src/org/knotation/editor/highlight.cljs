(ns org.knotation.editor.highlight
  (:require [clojure.set :as set]

            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.util :as util]))

(defn clear-line-highlights!
  ([eds] (clear-line-highlights! eds ["highlight" "current-subject"]))
  ([eds classes]
   (doseq [e eds
           i (util/line-range e)
           c classes]
     (.removeLineClass e i "background" c))))
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
  [line-map ed-from editors]
  (clear-line-highlights! editors ["current-subject" "highlight"])
  (let [ln-from (util/current-line ed-from)]
    (doseq [entry (ln/lookup line-map ed-from ln-from)]
      (let [ed-to (first entry)
            ln-to (second entry)]
        (highlight-line! ed-from ln-from)
        (highlight-line! ed-to ln-to)
        (util/scroll-into-view! ed-to :line ln-to :margin 100)))))

(defn cross<->highlight!
  [line-map-atom editors]
  (doseq [ed editors]
    (let [when-focused (fn [_] (when (.hasFocus ed) (cross->highlight! @line-map-atom ed editors)))
          always (fn [_] (cross->highlight! @line-map-atom ed editors))]
      (.on ed "cursorActivity" when-focused)
      (.on ed "focus" always)
      (.on ed "changes" always)
      (.on ed "compiled-from" when-focused))))
