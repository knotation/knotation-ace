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

(.defineMode
 js/CodeMirror "knotation"
 (fn [config]
   (let [lines (string/split-lines (.-value config))
         pipeline {:inputs [{:format :kn :lines lines}]}
         buffer-state (atom (knot/process-inputs pipeline))]
     (letfn [(match [stream reg label & {:keys [sol?] :or {sol? false}}]
               (when (and (or (not sol?) (and sol? (.sol stream)))
                          (.match stream reg false))
                 (.match stream reg) label))
             (token [stream]
               (if (.eatSpace stream)
                 "whitespace"
                 (or (match stream #"@prefix" "keyword")
                     (match stream #"<.*?>" "iri")
                     (match stream #": .*" "subject" :sol? true)
                     (match stream #".*?:" "predicate" :sol? true)
                     (when (not (.eatWhile stream #"[^\s]"))
                       (.skipToEnd stream)))))]
       (clj->js
        {:startState (fn [] {})
         :token (fn [stream state] (token stream))
         :indent (fn [state _textAfter] 0)
         :closeBrackets {:pairs "()[]{}\"\""}
         :lineComment "#"
         :blockCommentStart "#|"
         :blockCommentEnd "|#"})))))
