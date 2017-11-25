(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]))

(defn compiledToLineMap
  [compiled]
  (:map
   (reduce
    (fn [memo elem]
      (let [ed (if (= ::st/graph-end (::st/event elem)) (+ 1 (:ed memo)) (:ed memo))
            in (::st/line-number (::st/input elem))
            out (::st/line-number (::st/output elem))
            m (:map memo)]
        {:ed ed
         :map (if (and in out)
                (conj m [ed in :out out])
                m)}))
    {:ed 0 :map []}
    compiled)))

(defn compiled->line-map
  [compiled]
  (->> compiled
       (map (fn [e]
              [(->> e ::st/input ::st/line-number)
               (->> e ::st/output ::st/line-number)]))
       (filter (fn [[a b]] (and a b)))
       (map (fn [[a b]] [(- a 1) (- b 1)]))
       (into {})))

(defn lookup
  [line-map editor line]
  (get-in line-map [editor line]))
