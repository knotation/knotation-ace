(ns org.knotation.editor.line-map
  (:require [clojure.set :as set]

            [org.knotation.api :as api]
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

(defn update-line-map!
  [atm hub format input-editors output-editor]
  (let [out-ix (ed->ix output-editor)
        raw-maps (api/line-maps-of format hub)
        inverse-map (->> raw-maps
                         (map clojure.set/map-invert))
        line-maps raw-maps]
    (.log js/console "RAW " (clj->js raw-maps))
    (.log js/console "  --" (clj->js inverse-map))
    (reset!
     atm
     line-maps
     (->> line-maps
          (map (fn [a b] [a b]) (range))
          (into {})))))

(defn lookup
  [line-map editor line-ix]
  (map
   (fn [[ed-ix ln-ix]] [(ix->ed ed-ix) ln-ix])
   (get-in line-map [(ed->ix editor) line-ix] #{})))
