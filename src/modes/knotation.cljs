(ns modes.knotation
  (:require [cljsjs.codemirror]

            [modes.simple]))

(.defineSimpleMode
 js/CodeMirror "knotation"
 (clj->js {:start [{:regex #"@prefix" :token "keyword"}
                   {:regex #"<.*?>" :token "iri"}
                   {:regex #": .*" :token "subject"}
                   {:regex #"^.*:" :token "predicate"}]}))
