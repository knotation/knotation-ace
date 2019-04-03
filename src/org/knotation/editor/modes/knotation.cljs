(ns org.knotation.editor.modes.knotation
  (:require [clojure.string :as string]
            [cljsjs.codemirror]
            [clojure.walk :as walk]

            [org.knotation.editor.util :as util]
            [org.knotation.editor.styles :as style]
            [org.knotation.state :as st]))

(style/add-style!
 ".cm-s-kn .cm-keyword {color: #708;}
.cm-s-kn .cm-iri {color: #00c; text-decoration: underline;}
.cm-s-kn .cm-subject {font-weight: bolder;}
.cm-s-kn .cm-predicate {color: #085;}
.cm-s-kn .cm-symbol {color: #000;}
.cm-s-kn .cm-prefix {color: #708;}
")

(defn get-token
  "Given a stream, a state (for the mode), a parse type (keyword), and parse 
   content, set the parse type as the previous token and apply the current 
   token."
  [stream state type content]
  (if (.skipTo stream content)
    (let [cur-token (or (:prev-token @state) "comment")]
      (swap! state assoc :prev-token (name type))
      cur-token)
    (do (.skipToEnd stream) "comment")))

(defn read-parses
  "Given a stream, a state (for the mode), and a knotation state, use the parses
   to return highlighting tokens."
  [stream state kn-state]
  (let [parses 
        (if (not (empty? (:parses @state))) 
          (:parses @state) 
          (rest (::st/parse kn-state)))
        p (first parses)
        rest-parses (rest parses)]
    (if (not (empty? rest-parses))
      (swap! state assoc :parses rest-parses)
      (do
        (swap! state assoc :parses [])
        (swap! state update :line inc)))
    (get-token stream state (first p) (second p))))

(defn parse-kn-states
  "Given a stream, a state (for the mode), and all knotation states for the 
   editor, use the states to return highlighting tokens."
  [stream state all-states]
  (let [kn-state (util/get-state-at all-states (:line @state) (:column @state))]
    (read-parses stream state kn-state)))

(.defineMode
  js/CodeMirror "knotation"
  (fn [config]
    (clj->js 
      {:startState (fn [] (atom {:line 1 :column 1:parses []}))
       :copyState (fn [state] (atom @state))
       :token (fn [stream state]
                (parse-kn-states 
                  stream 
                  state 
                  (get (js->clj config) "states")))})))
