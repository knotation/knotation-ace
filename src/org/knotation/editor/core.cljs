(ns org.knotation.editor.core
  (:require [cljsjs.codemirror]

            [org.knotation.editor.modes.sparql]
            [org.knotation.editor.modes.turtle]
            [org.knotation.editor.modes.ntriples]
            [org.knotation.editor.modes.knotation]
            [org.knotation.editor.modes.javascript]

            [org.knotation.editor.addons.hint]

            [org.knotation.editor.styles :as styles]
            [org.knotation.editor.util :as util]
            [org.knotation.editor.line-map :as ln]
            [org.knotation.editor.highlight :as high]
            [org.knotation.editor.update :as update]

            [org.knotation.api :as api]
            [org.knotation.state :as st]

            [clojure.string :as string]))


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

(defn on-leave! [ed f] (add-hook! ed :on-hover f))
(def onLeave on-leave!)

(defn editor!
  [editor-selector & {:keys [mode theme focus?]
                      :or {mode "sparql" theme "default" focus? true}}]
  (styles/apply-style!)
  (let [elem (.querySelector js/document editor-selector)
        opts (clj->js {:lineNumbers true :gutters ["CodeMirror-linenumbers" "line-errors"]
                       :mode mode :autofocus focus?
                       :theme (str theme " " mode)

                       :hintOptions
                       {:completeSingle false
                        :hint (fn [ed opt]
                                (let [completions ["one" "two" "three" "four"]
                                      line (util/current-line ed)
                                      token (js->clj (.getTokenAt ed (.getCursor ed)))]
                                  (clj->js {:list (filter #(string/starts-with? % (get token "string")) completions)
                                            :from {:line line :ch (get token "start")}
                                            :to {:line line :ch (get token "end")}})))}})
        ed (if (= "TEXTAREA" (.-nodeName elem))
             (.fromTextArea js/CodeMirror elem opts)
             (js/CodeMirror elem opts))]

    (set! (.-knotation ed)
          (clj->js {:getCompiled
                    (fn [] (or (.-graph (.-knotation ed)) []))

                    :getCompiledLine
                    (fn [ln-num]
                      (when-let [g (.-graph (.-knotation ed))]
                        (first (filter #(= (inc ln-num) (->> % ::st/input ::st/line-number)) g))))}))

    (ln/assign-ix! ed)

    (set! (.-hooks ed) {:on-hover (atom []) :on-leave (atom [])})
    (set! (.-onmouseover (.getWrapperElement ed)) #(run-hooks! ed :on-hover %))
    (set! (.-onmouseout (.getWrapperElement ed)) #(run-hooks! ed :on-leave %))

    (.on
     ed "change"
     (fn [ed change]
       (let [chg (js->clj change)]
         (when (contains? #{"+input" "+delete"} (get chg "origin"))
           (.execCommand ed "autocomplete")))))

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

(defn linked-editors
  [& {:keys [env prefix
             input
             ttl nq rdfa]
      :or {env [] prefix []}}]
  (let [line-map (ln/line-map!)
        high! (fn [out] (when out (high/cross<->highlight! line-map (conj env input out))))]
    (update/cross->>update! line-map :env env :input input :ttl ttl :nq nq :rdfa rdfa)
    (high! ttl) (high! nq) (high! rdfa)
    (doseq [e (conj env input)] (high/subject-highlight-on-move! e))))

(defn linkedEditors
  [options]
  (let [opts (js->clj options :keywordize-keys true)]
    ;; the more obvious (apply linked-ediors (select-keys opts [...])) doesn't work here for some reason.
    ;; It's a bug in either the CLJS implementation of apply or select-keys
    (linked-editors :env (:env opts) :prefix (:prefix opts) :input (:input opts)
                    :ttl (:ttl opts) :nq (:nq opts) :rdfa (:rdfa opts))))
