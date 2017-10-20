(ns knotation-editor.editor
  (:require [cljsjs.codemirror]
            [modes.sparql]

            [knotation-editor.styles :as styles]))

(defn log! [& things]
  (apply js/console.log (map clj->js things)))

(defn dom-loaded [fn]
  (.addEventListener js/document "DOMContentLoaded" fn))

(defn add-commands!
  [ed commands]
  (.setOption
   ed "extraKeys"
   (clj->js commands)))

(defn editor!
  [editor-selector & {:keys [mode theme focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/append-style!)
  (let [editor (js/CodeMirror
                (.querySelector js/document editor-selector)
                (clj->js {:lineNumbers true :mode mode :autofocus focus?}))]
    editor))
