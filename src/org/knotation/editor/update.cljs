(ns org.knotation.editor.update
  (:require [clojure.string :as string]
            [clojure.tools.reader :as r]

            [org.knotation.cljs-api :as api]
            [org.knotation.info :as info]
            [org.knotation.environment :as en]
            [org.knotation.state :as st]
            [org.knotation.kn :as kn]
            [org.knotation.ttl :as ttl]

            [org.knotation.editor.line-styles :as ls]
            [org.knotation.editor.styles :as s]
            [org.knotation.editor.util :as util]))

;; Error handling

(defn mark-errors!
  "Given a sequence of states, an input editor, and an offset number, mark the 
   gutters of any lines (minus the offset) associated with a state that has the 
   event st/error."
  [states input offset]
  (doall 
    (map
      (fn [s]
        (let [event (get s ::st/event)
              ln (get-in s [::st/input ::st/start ::st/line-number])]
          (if (= event ::st/error)
            (ls/set-line-error! 
              input 
              (- ln offset)
              (->> s ::st/error ::st/error-message)))))
      states)))

(defn set-bad-parse!
  "Given an input editor, an output editor, and a series of keys (content, 
   start-ln, and end-ln), set the output editor to the error message and 
   highlight the offending lines."
  [input output & {:keys [content start-ln end-ln]}]
  (.setValue output (str "Bad parse: \"" content "\" at line " start-ln))
  (doseq [ln (range (- start-ln 1) end-ln)]
    (ls/highlight-line! input ln "line-error")))

(defn set-error!
  "Given an error message, an input editor, and an output editor, read the error
   message in one of two ways: (1) a bad parse which will be read into a map and
   used to highlight the bad lines, (2) any other error message which will only
   be written to the output editor."
  [e input output]
  (cond
    (re-find #"bad-parse" e)
    (let [m (r/read-string (string/replace e "Error: :bad-parse " ""))
          start-ln (get-in m [::st/input ::st/start ::st/line-number])
          end-ln (get-in m [::st/input ::st/end ::st/line-number])
          content (string/trim-newline (get-in m [::st/input ::st/content]))]
      (set-bad-parse! 
        input
        output 
        :content content 
        :start-ln start-ln 
        :end-ln end-ln))
    ;; Other error message (no line info)
    :else
    (let [msg (string/replace e "Error: " "")]
      (.setValue output msg))))

;; Info display
;; WARNING - this is *initial* functionality and does not yet fully work

(defn get-info
  "Given a state with a line number, get the info HTML for that line."
  [ed state]
  (let [source (.-id (.getWrapperElement ed))
        ln (->> state ::st/location ::st/line-number)]
    (str
      (->> state 
           info/help 
           info/html
           (str "<div class='info source-" source " line-" ln " hidden''>"))
      "</div>")))

(defn get-all-info
  "Given a sequence of states, get the HTML info for each state in a separate 
   div element."
  [ed states]
  (reduce
    (fn [v s]
      (str v (get-info ed s)))
    ""
    states))

;; Input and Output

(defn try-read-input!
  "Given an input editor, a previous state, and an output editor, try to read 
   the input. On error, set a message to the output editor."
  [input prev-state output]
  (try
    (api/read-string :kn prev-state (.getValue input))
    (catch js/Error e
      (do
        (set-error! (str e) input output)
        nil))))

(defn compile-info!
  "Given an output editor and a string result, reset the styles of the editor 
   and add the result as HTML."
  [output results]
  (let [elem (.getWrapperElement output)]
    (set! (.-innerHTML elem) (string/trim (string/join "\n" results)))
    (set! (.-overflow (.-style elem)) nil)
    (set! (.-height (.-style elem)) nil)
    (set! (.-font (.-style elem)) nil)
    (s/remove-class! elem ".CodeMirror")))

(defn compile-content-to!
  "Given a sequence of input editors, a result string, and an output editor, 
   compile the result string to the output editor."
  [inputs result output]
  (let [fmt (keyword (util/format-of output))]
    (.setValue output result)
    (.signal js/CodeMirror output "compiled-to" output result)
    (doseq [i inputs]
      (.signal js/CodeMirror i "compiled-from"))))

;; Main update methods

(defn run-update!
  "Given a sequence of input editors (usually a context and an input), convert 
   the contents of the input to the output format and set the value of the 
   output editor as the new content."
  [context input output]
  (let [fmt (keyword (util/format-of output))
        context-states (try-read-input! context nil output)
        initial-state (util/reset-line-count (last context-states))
        states (try-read-input! input initial-state output)]
    ;; Clear line styles and set errors, if any
    (ls/clear-line-styles! [context input])
    (mark-errors! context-states context 1)
    (mark-errors! states input 0)
    ;; If there is a result, set the output
    (if (some? states)
      (case fmt
        ;; HTML for info messages
        :html
        (let [context-res 
              (if (some? context-states)
                (get-all-info context context-states)
                nil)
              input-res (get-all-info input states)]
          (compile-info! output [context-res input-res]))
        ;; Any other format
        (let [result (api/render-output fmt initial-state states)]
          (if (some? result)
            (compile-content-to! [context input] result output)))))))

(defn update!
  "Given :context (optional), :input, and :output keys, any time an input 
   changes, convert it to the output format and set the value of the output 
   editor as the new content."
  [& {:keys [context input output]}]
  (let [c (if (some? context) (vector context) (vector))
        inputs (conj c input)]
    (run-update! context input output)
    (doseq [i inputs]
      (.on i "changes"
           (util/debounce
            (fn [cs]
              (run-update! context input output))
            500)))))
