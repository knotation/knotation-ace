(ns knotation-editor.editor
  (:require [cljsjs.codemirror]
            [modes.sparql]

            [knotation-editor.styles :as styles]))

(defn add-commands!
  [ed commands]
  (.setOption
   ed "extraKeys"
   (clj->js commands)))

(defn editor!
  [editor-selector & {:keys [mode theme focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/apply-style!)
  (let [editor (js/CodeMirror
                (.querySelector js/document editor-selector)
                (clj->js {:lineNumbers true :mode mode :autofocus focus?}))]
    editor))
