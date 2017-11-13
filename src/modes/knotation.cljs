(ns modes.knotation
  (:require [clojure.string :as string]
            [cljsjs.codemirror]

            [knotation-editor.styles :as style]
            [org.knotation.api :as knot]))

(style/add-style!
 ".cm-s-knotation .cm-keyword {color: #708;}
.cm-s-knotation .cm-iri {color: #00c; text-decoration: underline;}
.cm-s-knotation .cm-subject {font-weight: bolder;}
.cm-s-knotation .cm-predicate {color: #085;}
")

(defn intern! [state ks val]
  (swap! state (fn [s] (assoc-in s (vec (cons :env ks)) val))))

(defn prefix? [state word]
  (not (not (get-in @state [:env :prefix word]))))

(.defineMode
 js/CodeMirror "knotation"
 (fn [config]
   (letfn [(token [stream state]
             (let [match? (fn [reg] (when (.match stream reg false) (.match stream reg) true))
                   sol? (.sol stream)]
               (cond (and (not sol?) (.eatSpace stream)) "whitespace"

                     (match? #"@prefix")
                     (let [[_ name val] (re-matches #"@prefix *(.*?): *(.*)" (.-string stream))]
                       (intern! state [:prefix name] val)
                       "keyword")

                     (match? #"<.*?>") "iri"

                     (and sol? (= "#" (.peek stream)))
                     (do (.skipToEnd stream)
                         "comment")

                     (and sol? (.match stream #"\s+"))
                     (do (.skipToEnd stream)
                         "multiline-term")

                     (and sol? (.match stream #": .*" false))
                     (let [[_ sub] (.match stream #": (.*)")]
                       (swap! state assoc :subject sub)
                       "subject")

                     (and sol? (.match stream #".*?:" false))
                     (let [[_ pred] (.match stream #"(.*?):")]
                       (when (and (prefix? state pred) (not (.eatSpace stream)))
                         (.match stream #".*?:"))
                       "predicate")

                     (match? #".*?:.*") "prefixed-name"

                     :else (do (.skipToEnd stream) "term"))))]
     (clj->js {:startState (fn [] (atom {:env {}})) :copyState (fn [state] (atom @state))
               :token token
               ;; :indent (fn [state _textAfter] 0)
               :closeBrackets {:pairs "()[]{}\"\""}
               ;; :blockCommentStart "#|"
               ;; :blockCommentEnd "|#"
               }))))
