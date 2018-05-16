(ns org.knotation.editor.complete
  (:require [clojure.string :as string]

            [org.knotation.api :as api]
            [org.knotation.editor.util :as util]
            [org.knotation.environment :as en]))

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
        (let [completions (if (= "predicate" (.-type prev-token))
                            (let [env (.-env (.-knotation ed))]
                              (clj->js
                               (concat
                                (api/labels env)
                                (map #(str % ":") (api/prefixes env)))))
                            (.-completions (.-knotation ed)))]
          (when (not (empty? completions))
            (clj->js {:list (filter #(string/starts-with? % (get token "string")) completions)
                      :from {:line line :ch (get token "start")}
                      :to {:line line :ch (get token "end")}})))))))

(defn autocomplete [ed change]
  (when (not (empty? (.-completions (.-knotation ed))))
    (let [chg (js->clj change)]
      (when (contains? #{"+input" "+delete"} (get chg "origin"))
        (.execCommand ed "autocomplete")))))
