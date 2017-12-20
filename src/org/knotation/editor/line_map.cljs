(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.state :as st]

            [org.knotation.editor.util :as util]))

(def editors (atom []))

(defn assign-ix! [ed]
  (when  (not (number? (.-ix (.-knotation ed))))
    (set! (.-ix (.-knotation ed)) (count @editors))
    (swap! editors conj ed))
  nil)

(defn ed->ix [ed] (.-ix (.-knotation ed)))
(defn ix->ed [ix] (get @editors ix))

(defn line-map! [] (atom {}))
(defn clear! [atm] (reset! atm {}))

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
  ([line-map compiled input-editors out-key]
   (let [modified
         (fn [m ed in out elem]
           (if (and (or in (zero? in)) (or out (zero? out)))
             (update-in
              (update-in m [ed in] #(conj (or % #{}) [out-key out]))
              [out-key out] #(conj (or % #{}) [ed in]))
             m))]
     (reduce
      (fn [memo [ed lines]]
        (reduce
         (fn [m elem]
           (let [in (::st/line-number (::st/input elem))
                 out (::st/line-number (::st/output elem))]
             (modified m ed in out elem)))
         memo lines))
      line-map (util/zip input-editors (partition-graphs compiled))))))

(defn update-line-map!
  [atm compiled input-editors output-editor]
  (reset! atm (compiled->line-map @atm compiled (map ed->ix input-editors) (ed->ix output-editor))))

(defn lookup
  [line-map editor line-ix]
  (map
   (fn [[ed-ix ln-ix]] [(ix->ed ed-ix) ln-ix])
   (get-in line-map [(ed->ix editor) line-ix] #{})))
