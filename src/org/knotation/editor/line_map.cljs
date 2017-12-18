(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]))

(def empty {})

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

(defn compiled->line-map
  ([compiled editors out-key] (compiled->line-map empty compiled editors out-key))
  ([line-map compiled editors out-key]
   (let [modified
         (fn [m ed in out elem]
           (if (and (or in (zero? in)) (or out (zero? out)))
             (let [i [ed in] o [out-key out]]
               (update-in
                (update-in m [ed in] #(conj (or % #{}) [out-key out]))
                [out-key out] #(conj (or % #{}) [ed in])))
             m))]
     (:map
      (reduce
       (fn [memo elem]
         (let [ed (if (= ::st/graph-end (::st/event elem)) (+ 1 (:ed memo)) (:ed memo))
               in (::st/line-number (::st/input elem))
               out (::st/line-number (::st/output elem))
               m (:map memo)]
           (when (and in out) (.log js/console " =>" (clj->js elem)))
           {:ed ed :map (modified m ed in out elem)}))
       {:ed 0 :map line-map}
       compiled)))))

(defn lookup
  [line-map editor-ix line-ix]
  (get-in line-map [editor-ix line-ix] #{}))
