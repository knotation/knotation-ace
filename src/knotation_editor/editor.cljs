(ns knotation-editor.editor
  (:require [cljsjs.codemirror]

            [modes.sparql]
            [modes.turtle]
            [modes.ntriples]

            [knotation-editor.styles :as styles]))

(defn addCommands
  [ed commands]
  (.setOption ed "extraKeys" commands))

(defn add-commands!
  [ed commands]
  (addCommands ed (clj->js commands)))

(defn editor!
  [editor-selector & {:keys [mode theme focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/apply-style!)
  (let [editor (js/CodeMirror
                (.querySelector js/document editor-selector)
                (clj->js {:lineNumbers true :mode mode :autofocus focus?}))]
    editor))
(def fromSelector editor!)
