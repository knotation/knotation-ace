(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]))

(def empty {})

(defn compiled->line-map
  ([compiled] (compiled->line-map empty compiled))
  ([line-map compiled]
   (let [modified
         (fn [m ed in out]
           (if (and in out)
             (update-in
              (update-in m [ed in] #(conj (or % #{}) [:out (dec out)]))
              [:out (dec out)] #(conj (or % #{})  [ed in]))
             m))]
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
            :map (reduce (fn [m delta] (modified m ed (+ delta in) (+ delta comp out))) m (range lns))}))
       {:ed 0 :compensate 0 :map line-map}
       compiled)))))

(defn lookup
  [line-map editor-ix line-ix]
  (get-in line-map [editor-ix line-ix] #{}))
