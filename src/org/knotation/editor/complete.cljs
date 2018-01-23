(ns org.knotation.editor.complete
  (:require [clojure.string :as string]

            [org.knotation.editor.util :as util]))

(defn -merge-completions [original new]
  (distinct (concat original new)))

(defn add-completions! [ed completions]
  (set! (.-completions (.-knotation ed))
        (-merge-completions (.-completions (.-knotation ed)) completions)))

(defn hint [ed opt]
  (let [token (js->clj (.getTokenAt ed (.getCursor ed)))]
    (when (> (count (get token "string")) 1)
      (let [line (util/current-line ed)
            prev-token (.getTokenAt ed (clj->js {:line line :ch (dec (get token "start"))}))]
        (if (= "predicate" (.-type prev-token))
          (let [completions (if (= "predicate" (.-type prev-token))
                              (let [env (:env @(get token "state"))]
                                (clj->js
                                 (concat
                                  (keys (:label env))
                                  (map #(str % ":") (keys (:prefix env))))))
                              (.-completions (.-knotation ed)))]
            (when (not (empty? completions))
              (clj->js {:list (filter #(string/starts-with? % (get token "string")) completions)
                        :from {:line line :ch (get token "start")}
                        :to {:line line :ch (get token "end")}}))))))))

(defn autocomplete [ed change]
  (when (not (empty? (.-completions (.-knotation ed))))
    (let [chg (js->clj change)]
      (when (contains? #{"+input" "+delete"} (get chg "origin"))
        (.execCommand ed "autocomplete")))))
