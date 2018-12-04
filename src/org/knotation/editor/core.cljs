(ns org.knotation.editor.core
  (:require [cljsjs.codemirror]

            [org.knotation.editor.modes.knotation]
            [org.knotation.editor.modes.turtle]

            [org.knotation.rdf :as rdf]
            [org.knotation.state :as st]

            [org.knotation.editor.complete :as complete]
            [org.knotation.editor.util :as util]
            [org.knotation.editor.styles :as styles]
            [org.knotation.editor.update :as update]

            [clojure.string :as string]
            [clojure.set :as set]))

(def -format-map
  {"ttl" "turtle"
   "nq" "ntriples"
   "rdfa" "sparql"
   "kn" "knotation"
   "tree" "javascript"
   "md" "markdown"})

(defn -ev->token
  [ed ev]
  (let [char (.coordsChar ed (clj->js {:left (.-pageX ev) :top (.-pageY ev)}))
        line (.-line char)
        token (.getTokenAt ed (clj->js {:line line :ch (.-ch char)}))]
    (set! (.-line token) line)
    (set! (.-compiled token) (.getCompiledLine (.-knotation ed) line))
    token))

(defn add-hook! [ed k f]
  (swap! (k (.-hooks ed)) conj f))

(defn run-hooks! [ed k ev]
  (doseq [h @(get (.-hooks ed) k)]
    (h (-ev->token ed ev))))

(defn on-click! [ed f] (add-hook! ed :on-click f))
(def onClick on-click!)

(defn on-hover! [ed f] (add-hook! ed :on-hover f))
(def onHover on-hover!)

(defn on-leave! [ed f] (add-hook! ed :on-leave f))
(def onLeave on-leave!)

(defn add-codemirror-hooks!
  "Given a CodeMirror editor, add a series of hooks."
  [ed]
  (set! 
    (.-hooks ed) 
    {:on-click (atom []) :on-hover (atom []) :on-leave (atom [])})
  (set! (.-onmousedown (.getWrapperElement ed)) #(run-hooks! ed :on-click %))
  (set! (.-onmouseover (.getWrapperElement ed)) #(run-hooks! ed :on-hover %))
  (set! (.-onmouseout (.getWrapperElement ed)) #(run-hooks! ed :on-leave %))
  (on-hover!
     ed (fn [token]
          (let [ln (.-line token)
                all-errs 
                (.querySelectorAll 
                  (.getWrapperElement ed) ".line-error-message")
                err 
                (.querySelector 
                  (.getWrapperElement ed) (str ".line-error-message.line-" ln))]
            (.forEach all-errs #(.add (.-classList %) "hidden"))
            (when err (.remove (.-classList err) "hidden"))))))

(defn add-info-hooks!
  "Given a input editor and an output editor, set an on-click hook for 
   displaying information about the current line."
  [input-ed output-ed]
  (on-click!
    input-ed 
    (fn [token]
      (let [ln (.-line token)
            ;; the element for display is the output editor
            elem (.getWrapperElement output-ed)
            all-info (.querySelectorAll elem ".info")
            ;; the current info DIV has source-x and line-x classes attached
            ;; source being the ID of the input editor
            info 
            (.querySelector elem 
              (str ".info.source-" 
                (.-id (.getWrapperElement input-ed)) ".line-" (+ ln 1)))]
        (.forEach all-info #(.add (.-classList %) "hidden"))
        (when info (.remove (.-classList info) "hidden"))))))

(defn get-codemirror
  "Given a document element and a map of options, return the CodeMirror editor."
  [elem opts]
  (if (= "TEXTAREA" (.-nodeName elem))
    (.fromTextArea js/CodeMirror elem opts)
    (js/CodeMirror elem opts)))

(defn get-codemirror-opts
  "Given an optional set of keys, return the options as a JS map for."
  [& {:keys [mode focus? theme]}]
  (clj->js {:lineNumbers true 
            :gutters ["CodeMirror-linenumbers" "gutter-markers"]
            :mode (or (get -format-map mode) mode) 
            :autofocus focus?
            :theme (str theme " " mode)
            :extraKeys {"Ctrl-Space" "autocomplete"}
            :hintOptions {:completeSingle true :hint complete/hint}}))

(defn set-codemirror-opts!
  "Given a CodeMirror editor and a mode, set the options."
  [ed mode]
  (set! (.-knotation ed)
        (clj->js {:format (or (get (set/map-invert -format-map) mode) mode)
                  :getCompiled
                  (fn [] (or (.-graph (.-knotation ed)) []))
                  :getCompiledLine
                  (fn [ln-num]
                    (when-let [g (.-graph (.-knotation ed))]
                      (first 
                        (filter #(= (inc ln-num) (util/line-num-in %)) g))))})))

(defn editor!
  "Given an editor selector (the name of the element) and a set of optional 
   keys, apply the style to the CodeMirror editor, set the options, and add 
   hooks. Return the CodeMirror editor."
  [editor-selector & {:keys [mode theme focus? completions read-only]
                      :or {mode "sparql"
                           theme "default"
                           focus? true
                           completions []
                           read-only false}}]
  (styles/apply-style!)
  (let [elem (.querySelector js/document editor-selector)
        opts (get-codemirror-opts :mode mode :focus? focus? :theme theme)
        ed (get-codemirror elem opts)]
    (set-codemirror-opts! ed mode)
    (add-codemirror-hooks! ed)
    (complete/add-completions! ed completions)
    (if read-only
      (.setOption ed "readOnly" true)
      (.on ed "change" complete/autocomplete))
    ed))

(defn fromSelector
  "Given an editor selector (the name of the element) and an optional option 
   map, create a CodeMirror editor for that document element."
  [editor-selector & options-map]
  (let [options (or (first options-map) {})
        opts (update
         (merge {:mode "sparql" :theme "default"
                 :read-only (or (.-readOnly options) false)
                 :focus? (not (not (.-focus options)))}
                (dissoc (js->clj options :keywordize-keys true) :focus))
         :completions #(js->clj %))]
    (apply editor! editor-selector (mapcat identity opts))))

(defn linked
  "Given input and output keys, link the editors so that the content of the 
   input editor is compiled to the output editor."
  [& {:keys [context input output]}]
  ;; Maybe add info hooks for a context editor
  (when (some? context)
    (set! (.-id (.getWrapperElement context)) "context")
    (add-info-hooks! context output))
  ;; Add info hooks for the input editor
  (set! (.-id (.getWrapperElement input)) "input")
  (add-info-hooks! input output)
  (update/update! :context context :input input :output output))

(defn link!
  "Given an input editor and an output editor, link the editors so that the 
   content of the input editor is compiled to the output editor."
  [context input output]
  (linked :context context :input input :output output))
(def link link!)


