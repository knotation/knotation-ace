(ns org.knotation.editor.update
  (:require [clojure.string :as string]
            [crate.core :as crate]

            [org.knotation.api :as api]
            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]
            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.highlight :as high]))

     ;; (let [opts {::api/operation-type :render ::st/format :ttl}
     ;;       processed (api/run-operations [(api/kn (.getValue editor-a)) opts])
     ;;       result (string/join "\n" (filter identity (map (fn [e] (->> e ::st/output ::st/lines first)) processed)))
     ;;       line-pairs (map (fn [e] [(->> e ::st/input ::st/line-number) (->> e ::st/output ::st/line-number)]) processed)]
     ;;   (.setValue editor-b result)
     ;;   (reset! line-map (into {} (map (fn [e][(->> e ::st/input ::st/line-number) (->> e ::st/output ::st/line-number)]) processed)))
     ;;   ;; (.log js/console "NEW LINE-MAP" (clj->js ))
     ;;   (doseq [p line-pairs]
     ;;     (.log js/console "  " (clj->js p))))

(defn compiled->content
  [compiled]
  (->> compiled
       (mapcat #(->> % ::st/output ::st/lines))
       (filter identity)
       (string/join "\n")))

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
      (case (::st/event elem)
        ::st/graph-end
        (swap! cur-ed inc)

        ::st/error
        (let [ed (get editors @cur-ed)
              in (::st/input elem)
              ln-num (dec (::st/line-number in))]
          (high/highlight-line! ed ln-num "line-error")
          (.addWidget
           ed (clj->js {:line ln-num :ch 0})
           (crate/html
            [:pre {:class (str "line-error-message hidden line-" ln-num)}
             (->> elem ::st/error ::st/error-message)]))
          (.setGutterMarker ed ln-num "line-errors" (crate/html [:div {:style "color: #822"} "▶"])))

        nil))))

(defn partition-graphs
  [processed]
  (let [a (volatile! 0)]
    (partition-by
     (fn [elem]
       (let [res @a]
         (when (= ::st/graph-end (::st/event elem))
           (vswap! a inc))
         res))
     processed)))

(defn compile-content-to
  [line-map-atom editors]
  (let [ct (count editors)
        outp (last editors)
        inp (last (butlast editors))
        prefs (butlast (butlast editors))
        processed (api/run-operations
                   (conj
                    (conj
                     (vec (map #(api/env :kn (.getValue %)) prefs))
                     (api/input :kn (.getValue inp)))
                    (api/output :ttl)))
        result (compiled->content processed)
        line-map (ln/compiled->line-map processed)]
    (doseq [[ed graph] (map (fn [a b] [a b]) (butlast editors) (partition-graphs processed))]
      (set! (.-graph (.-knotation ed)) graph))
    (clear-line-errors! editors)
    (mark-line-errors! processed editors)
    (.setValue outp result)
    (reset! line-map-atom line-map)
    processed))

(defn cross->update!
  [line-map-atom compiled-atom editors]
  (doseq [[ix e] (map-indexed vector (butlast editors))]
    (.on e "changes"
         (util/debounce
          (fn [cs]
            (let [ln (util/current-line e)
                  result (compile-content-to line-map-atom editors)]
              (reset! compiled-atom result)
              (high/cross->highlight! @line-map-atom ix editors)))
          500))))
