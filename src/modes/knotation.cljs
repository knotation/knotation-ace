(ns modes.knotation
  (:require [cljsjs.codemirror]

            [modes.simple]
            [knotation-editor.styles :as style]))

(style/add-style!
 ".cm-s-knotation .cm-keyword {color: #708;}
.cm-s-knotation .cm-iri {color: #00c; text-decoration: underline;}
.cm-s-knotation .cm-subject {font-weight: bolder;}
.cm-s-knotation .cm-predicate {color: #085;}
")

(.defineSimpleMode
 js/CodeMirror "knotation"
 (clj->js {:start [{:regex #"@prefix" :token "keyword"}
                   {:regex #"<.*?>" :token "iri"}
                   {:regex #": .*" :token "subject" :sol true}
                   {:regex #"^.*?:" :token "predicate" :sol true}]}))
