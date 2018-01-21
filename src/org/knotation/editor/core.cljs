(ns org.knotation.editor.core
  (:require [cljsjs.codemirror]

            [org.knotation.editor.modes.sparql]
            [org.knotation.editor.modes.turtle]
            [org.knotation.editor.modes.ntriples]
            [org.knotation.editor.modes.knotation]
            [org.knotation.editor.modes.javascript]
            [org.knotation.editor.modes.dot]

            [org.knotation.editor.styles :as styles]
            [org.knotation.editor.util :as util]
            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.highlight :as high]
            [org.knotation.editor.update :as update]

            [org.knotation.n3 :as n3]
            [org.knotation.api :as api]
            [org.knotation.state :as st]

            [clojure.string :as string]
            [clojure.set :as set]))

(defn addCommands
  [ed commands]
  (.setOption ed "extraKeys" commands))

(defn add-commands!
  [ed commands]
  (addCommands ed (clj->js commands)))

(defn -ev->token
  [ed ev]
  (let [char (.coordsChar ed (clj->js {:left (.-pageX ev) :top (.-pageY ev)}))
        line (.-line char)
        token (.getTokenAt ed (clj->js {:line line :ch (.-ch char)}))]
    (set! (.-line token) line)
    (set! (.-compiled token) (.getCompiledLine (.-knotation ed) line))
    token))

(defn add-hook! [ed key f]
  (swap! (:on-hover (.-hooks ed)) conj f))

(defn run-hooks! [ed key ev]
  (doseq [h @(get (.-hooks ed) key)]
    (h (-ev->token ed ev))))

(defn on-hover! [ed f] (add-hook! ed :on-hover f))
(def onHover on-hover!)

(defn on-leave! [ed f] (add-hook! ed :on-leave f))
(def onLeave on-leave!)

(def -format-map
  {"ttl" "turtle"
   "nq" "ntriples"
   "rdfa" "sparql"
   "kn" "knotation"
   "tree" "javascript"})

(defn editor!
  [editor-selector & {:keys [mode theme focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/apply-style!)
  (let [elem (.querySelector js/document editor-selector)
        opts (clj->js {:lineNumbers true :gutters ["CodeMirror-linenumbers" "line-errors"]
                       :mode (or (get -format-map mode) mode) :autofocus focus?
                       :theme (str theme " " mode)})
        ed (if (= "TEXTAREA" (.-nodeName elem))
             (.fromTextArea js/CodeMirror elem opts)
             (js/CodeMirror elem opts))]

    (set! (.-knotation ed)
          (clj->js {:format (or (get (set/map-invert -format-map) mode) mode)

                    :getCompiled
                    (fn [] (or (.-graph (.-knotation ed)) []))

                    :getCompiledLine
                    (fn [ln-num]
                      (when-let [g (.-graph (.-knotation ed))]
                        (first (filter #(= (inc ln-num) (->> % ::st/input ::st/line-number)) g))))}))

    (ln/assign-ix! ed)

    (set! (.-hooks ed) {:on-hover (atom []) :on-leave (atom [])})
    (set! (.-onmouseover (.getWrapperElement ed)) #(run-hooks! ed :on-hover %))
    (set! (.-onmouseout (.getWrapperElement ed)) #(run-hooks! ed :on-leave %))

    (on-hover!
     ed (fn [token]
          (let [ln (.-line token)
                all-errs (.querySelectorAll (.getWrapperElement ed) ".line-error-message")
                err (.querySelector (.getWrapperElement ed) (str ".line-error-message.line-" ln))]
            (.forEach all-errs #(.add (.-classList %) "hidden"))
            (when err (.remove (.-classList err) "hidden")))))

    ed))

(defn fromSelector
  [editor-selector options]
  (let [opts (merge {:mode "sparql" :theme "default"
                     :on-hover (.-onHover options)
                     :focus? (not (not (.-focus options)))}
                    (dissoc (js->clj options :keywordize-keys true) :focus))]
    (apply editor! editor-selector (mapcat identity opts))))

(defn linked
  [& {:keys [env prefix input outputs]
      :or {env [] prefix []}}]
  (let [line-map (ln/line-map!)]
    (update/cross->>update! line-map :env env :prefix prefix :input input :outputs outputs)
    (doseq [out outputs] (high/cross<->highlight! line-map (conj env input out)))
    (doseq [e (conj env input)] (high/subject-highlight-on-move! e))))

(defn link!
  [env input & outputs]
  (.log js/console "OUT MAP" (clj->js (group-by util/format-of outputs)) (clj->js (group-by util/mode-of outputs)))
  (linked :env [env] :input input :outputs outputs))
(def link link!)
