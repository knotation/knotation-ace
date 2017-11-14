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
  [editor-selector & {:keys [mode theme on-hover focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/apply-style!)
  (let [elem (.querySelector js/document editor-selector)
        opts (clj->js {:lineNumbers true :mode mode :autofocus focus? :theme (str theme " " mode)})
        ed (if (= "TEXTAREA" (.-nodeName elem))
             (.fromTextArea js/CodeMirror elem opts)
             (js/CodeMirror elem opts))]

    (when on-hover
      (set! (.-onmouseover (.getWrapperElement ed))
            (fn [ev]
              (let [char (.coordsChar ed (clj->js {:left (.-clientX ev) :top (.-clientY ev)}))
                    token (.getTokenAt ed (clj->js {:line (.-line char) :ch (.-ch char)}))]
                (set! (.-line token) (.-line char))
                (on-hover token)))))

    ed))

(defn fromSelector
  [editor-selector options]
  (let [opts (merge {:mode "sparql" :theme "default"
                     :on-hover (.-onHover options)
                     :focus? (not (not (.-focus options)))}
                    (dissoc (js->clj options :keywordize-keys true) :focus))]
    (apply editor! editor-selector (mapcat identity opts))))
