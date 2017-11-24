(ns org.knotation.editor.core
  (:require [cljsjs.codemirror]

            [org.knotation.editor.modes.sparql]
            [org.knotation.editor.modes.turtle]
            [org.knotation.editor.modes.ntriples]
            [org.knotation.editor.modes.knotation]

            [org.knotation.editor.styles :as styles]
            [org.knotation.editor.util]
            [org.knotation.editor.highlight]

            [org.knotation.n3 :as n3]
            [org.knotation.api :as api]
            [org.knotation.state :as st]

            [clojure.string :as string]))

(defn _parseTTL
  [string]
  ;; (let [parser (.Parser js/N3)
  ;;       res (atom [])]
  ;;   (.parse parser string
  ;;           (fn [err trip prefs]
  ;;             (.log js/console "TRIP" err trip prefs)
  ;;             (swap! res conj [err trip prefs])))
  ;;   (clj->js @res))
  (let [lines (string/split-lines string)
        prefixes (->> lines
                      (filter #(re-find #"^@prefix" %))
                      (map #(re-find #"^@prefix (.*): <?([^>]+)>?" %))
                      (map (fn [[_ k v]] [k v]))
                      (into {}))]
    {:prefixes prefixes :quads (js->clj (.parse (.Parser js/N3) string))}))

(defn _writeTTL
  ([trips callback] (_writeTTL (clj->js {}) trips callback))
  ([prefixes trips callback]
   (.log js/console "_writeTTL CALLED" prefixes trips callback)
   (let [writer (.Writer js/N3 (clj->js {:prefixes (js->clj prefixes)}))]
     (doseq [t trips] (.addTriple writer t))
     (.end writer callback))))

(defn _compiledFromTTL
  [string]
  (let [parsed (_parseTTL string)]
    (.log js/console "PARSED" (clj->js parsed))
    (_writeTTL (clj->js (:prefixes parsed)) (clj->js (:quads parsed))
               (fn [err res]
                 (.log js/console "CALLBACK CALLED err:" err " res:" (clj->js (api/input :nq res)))
                 (.log js/console "END RESULT" (clj->js (api/run-operations [(api/input :nq res)])))))))

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
