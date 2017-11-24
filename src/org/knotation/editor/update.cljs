(ns org.knotation.editor.update
  (:require [clojure.string :as string]

            [org.knotation.api :as api]
            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]
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

(defn compile-content-to
  [line-map-atom source target]
  (let [processed (api/run-operations [(api/input :kn (.getValue source)) (api/output :ttl)])
        result (string/join "\n" (filter identity (map (fn [e] (->> e ::st/output ::st/lines first)) processed)))
        line-pairs (map (fn [e] [(->> e ::st/input ::st/line-number) (->> e ::st/output ::st/line-number)]) processed)]
    (.setValue target result)
    (reset! line-map-atom (into {} line-pairs))))

(defn cross->update!
  [line-map-atom editor-a editor-b]
  (.on editor-a "changes"
       (util/debounce
        (fn [cs]
          (let [ln (util/current-line editor-a)]
            (compile-content-to line-map-atom editor-a editor-b)
            (high/highlight-line! editor-b ln)
            (util/scroll-into-view! editor-b :line ln)))
        500)))
