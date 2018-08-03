(ns org.knotation.editor.update
  (:require [clojure.string :as string]
            [crate.core :as crate]

            [org.knotation.api :as api]
            [org.knotation.state :as st]
            [org.knotation.environment :as en]

            [org.knotation.editor.util :as util]
            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.highlight :as high]))

(defn clear-line-errors!
  [eds]
  (high/clear-line-highlights! eds ["line-error"])
  (doseq [ed eds]
    (.forEach (.querySelectorAll (.getWrapperElement ed) ".line-error-message")
              #(.remove %))
    (doseq [i (util/line-range ed)]
      (.setGutterMarker ed i "line-errors" nil)))
  nil)

(defn mark-line-errors!
  [compiled editors]
  (let [cur-ed (atom 0)]
    (doseq [elem compiled]
      (cond
        (api/graph-end? elem)
        (swap! cur-ed inc)

        (api/error? elem)
        (let [ed (get editors @cur-ed)
              ln-num (dec (api/line-num-in elem))]
          (high/highlight-line! ed ln-num "line-error")
          (.addWidget
           ed (clj->js {:line ln-num :ch 0})
           (crate/html
            [:pre {:class (str "line-error-message hidden line-" ln-num)}
             (api/error-message elem)]))
          (.setGutterMarker ed ln-num "line-errors" (crate/html [:div {:style "color: #822"} "▶"])))))))

(defn compile-content-to!
  [line-map-atom hub inputs output format]
  (let [env (api/env-of hub)
        hub (org.knotation.format/render-states format env hub)
        result (api/render-to format hub)]
    (ln/update-line-map! line-map-atom hub inputs output)
    (mark-line-errors! hub inputs)
    (.setValue output result)
    ;; FIXME - this kind of works right now, but it does a lot more work than it needs to
    ;;         for multiple output editors (assigns each output graph, clobbering them successively
    ;;         until the last one is finally left for consumption). We should figure out a principled
    ;;         way of deciding which (if any) graph to expose.
    (doseq [[ed graph] (util/zip inputs (ln/partition-graphs hub))]
      (set! (.-graph (.-knotation ed)) graph)
      (set! (.-env (.-knotation ed)) env))
    (.signal js/CodeMirror output "compiled-to" output result)
    hub))

(defn cross->>update!
  [line-map-atom & {:keys [env prefix input outputs]}]
  (let [inputs (conj env input)
        out! (fn []
               (let [hub
                     (api/read-from
                      :kn
                      (map #(str (string/trim (.getValue %)) "\n")
                           (conj (vec (concat env prefix)) input)))]
                 (clear-line-errors! inputs)
                 (doseq [out outputs]
                   (compile-content-to! line-map-atom hub inputs out (keyword (util/format-of out))))
                 (doseq [ed inputs] (.signal js/CodeMirror ed "compiled-from"))))]
    (out!)
    (doseq [in inputs]
      (.on in "changes"
           (util/debounce
            (fn [cs]
              (ln/clear! line-map-atom)
              (out!))
            500)))))
