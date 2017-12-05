(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]))

(defn compiled->line-map
  [compiled]
  (:map
   (reduce
    (fn [memo elem]

      (let [ed (if (= ::st/graph-end (::st/event elem)) (+ 1 (:ed memo)) (:ed memo))
            in (::st/line-number (::st/input elem))
            out (::st/line-number (::st/output elem))
            lns (count (::st/lines (::st/input elem)))
            comp (:compensate memo)
            m (:map memo)]
        {:ed ed
         :compensate (if (> lns 1) (+ (dec lns) comp) comp)
         :map (if (and in out)
                (update-in
                 (update-in m [ed (dec in)] #(conj (or % #{}) [:out (+ comp (dec out))]))
                 [:out (+ comp (dec out))] #(conj (or % #{})  [ed (dec in)]))
                m)}))
    {:ed 0 :compensate 0 :map {}}
    compiled)))

(defn lookup
  [line-map editor-ix line-ix]
  (get-in line-map [editor-ix line-ix] #{}))
