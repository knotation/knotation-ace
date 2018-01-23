(ns org.knotation.editor.complete
  (:require [clojure.string :as string]

            [org.knotation.editor.util :as util]))

(defn -merge-completions [original new]
  (distinct (concat original new)))

(defn add-completions! [ed completions]
  (set! (.-completions (.-knotation ed))
        (-merge-completions (.-completions (.-knotation ed)) completions)))

(defn hint [ed opt]
  (let [completions (.-completions (.-knotation ed))
        token (js->clj (.getTokenAt ed (.getCursor ed)))]
    ;; (.log js/console "TOKEN" (.getTokenAt ed (.getCursor ed)) (clj->js completions))
    (when (> (count (get token "string")) 1)
      (let [line (util/current-line ed)]
        (when (not (empty? completions))
          (clj->js {:list (filter #(string/starts-with? % (get token "string")) completions)
                    :from {:line line :ch (get token "start")}
                    :to {:line line :ch (get token "end")}}))))))

(defn autocomplete [ed change]
  (when (not (empty? (.-completions (.-knotation ed))))
    (let [chg (js->clj change)]
      (when (contains? #{"+input" "+delete"} (get chg "origin"))
        (.execCommand ed "autocomplete")))))
