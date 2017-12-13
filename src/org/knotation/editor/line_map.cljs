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
  ([compiled editors] (compiled->line-map empty compiled editors))
  ([line-map compiled editors]
   (let [modified
         (fn [m ed in out]
           (if (and in out)
             (update-in
              (update-in m [ed (dec in)] #(conj (or % #{}) [:out out]))
              [:out out] #(conj (or % #{}) [ed (dec in)]))
             m))]
     (:map
      (reduce
       (fn [memo elem]
         (let [ed (if (= ::st/graph-end (::st/event elem)) (+ 1 (:ed memo)) (:ed memo))
               in (::st/line-number (::st/input elem))
               out (::st/line-number (::st/output elem))
               lns (count (::st/lines (::st/input elem)))
               m (:map memo)]
           {:ed ed :map (reduce (fn [m delta] (modified m ed (+ delta in) (+ delta out))) m (range lns))}))
       {:ed 0 :map line-map}
       compiled)))))

(defn lookup
  [line-map editor-ix line-ix]
  (get-in line-map [editor-ix line-ix] #{}))
