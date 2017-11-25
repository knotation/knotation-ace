(ns org.knotation.editor.update
  (:require [clojure.string :as string]

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
       (map #(->> % ::st/output ::st/lines first))
       (filter identity)
       (string/join "\n")))

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
    (.setValue outp result)
    (reset! line-map-atom line-map)))

(defn cross->update!
  [line-map-atom editors]
  (doseq [[ix e] (map-indexed vector (butlast editors))]
    (.on e "changes"
         (util/debounce
          (fn [cs]
            (let [ln (util/current-line e)]
              (compile-content-to line-map-atom editors)
              (high/cross->highlight! @line-map-atom ix editors)))
          500))))
