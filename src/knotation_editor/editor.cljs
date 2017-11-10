(ns knotation-editor.editor
  (:require [cljsjs.codemirror]

            [modes.sparql]
            [modes.turtle]
            [modes.ntriples]
            [modes.knotation]

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
(defn fromSelector
  [editor-selector options]
  (let [opts (merge {:mode "sparql" :theme "default"
                     :focus? (not (not (:focus options)))}
                    (dissoc (js->clj options :keywordize-keys true) :focus))]
    (apply editor! editor-selector (mapcat identity opts))))
