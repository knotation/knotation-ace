(ns org.knotation.editor.line-styles
  (:require [clojure.set :as set]
            [crate.core :as crate]

            [org.knotation.editor.util :as util]))

(defn clear-line-highlights!
  "Given a sequence of editors, clear the following classes from the lines: 
   'highlight', 'current-subject', 'line-error'.
   Given a sequence of editors and a sequence of classes (as strings), clear all
   the classes from the editors."
  ([eds] 
   (clear-line-highlights! eds ["highlight" "current-subject" "line-error"]))
  ([eds classes]
   (doseq [e eds
           i (util/line-range e)
           c classes]
     (.removeLineClass e i "background" c))))
(def clearLineHighlights clear-line-highlights!)

(defn clear-hover-messages!
  "Given a sequence of editors, clear the hover messages on all lines."
  [eds]
  (doseq [e eds]
    (.forEach (.querySelectorAll (.getWrapperElement e) ".line-error-message")
      #(.remove %))))

(defn clear-gutter-markers!
  "Given a sequence of editors, clear the gutter-markers class from the lines."
  [eds]
  (doseq [e eds
          i (util/line-range e)]
    (.clearGutter e "gutter-markers")))

(defn clear-line-styles!
  "Given a sequence of editors, clear the error-associated line styles.
   Given a sequence of editors and a sequence of classes, remove the line styles
   only including the classes for highlighted lines."
  ([eds]
   (do
     (clear-gutter-markers! eds)
     (clear-hover-messages! eds)
     (clear-line-highlights! eds)))
  ([eds classes]
    (do
      (clear-gutter-markers! eds)
      (clear-hover-messages! eds)
      (clear-line-highlights! eds classes))))

(defn add-hover-message!
  "Given an editor, a line number, and a message, add a hover message."
  [ed line msg]
  (.addWidget
    ed (clj->js {:line line :ch 0})
    (crate/html
      [:pre {:class (str "line-error-message hidden line-" line)}
      msg])))

(defn create-div-element!
  "Given a text character and an HTML color code, create a div element 
   containing the colored character."
  [chr color]
  (let [elem (.createElement js/document "div")]
    (set! (.-innerHTML elem) chr)
    (set! (.-color (.-style elem)) color)
    elem))

(defn set-gutter-marker!
  "Given an editor and a line number, set the gutter marker to a dark red 
   triangle for errors.
   Given an editor, a line number, a text character, and an HTML color code, set
   the cutter marker to the colored character."
  ([ed line] 
   (set-gutter-marker! ed line "&#9654;" "#9b0000"))
  ([ed line chr color]
   (.setGutterMarker ed line "gutter-markers" (create-div-element! chr color))))

(defn set-line-error!
  [ed line msg]
  (do
    (add-hover-message! ed line msg)
    (set-gutter-marker! ed line)))

(defn highlight-line!
  "Given an editor and a line, apply the 'highlight' class to the line.
   Given an editor, a line, and a class, apply the class to the line."
  ([ed line] (highlight-line! ed line "highlight"))
  ([ed line class] (.addLineClass ed line "background" class)))
(def highlightLine highlight-line!)

(defn highlight-by-subject!
  "Given an editor and a line, highlight the current subject of the line when 
   the editor mode is 'knotation'."
  [editor line]
  (when (util/knotation-mode? editor)
    (letfn [(handle-of [ln] (.getLineHandle editor ln))
            (subject-of [ln] (:subject @(.-stateAfter (handle-of ln))))
            (blank? [ln] (empty? (.-text (handle-of ln))))]
      (if-let [subject (and (not (blank? line)) (subject-of line))]
        (doseq [i (util/line-range editor)]
          (when (and (= subject (subject-of i)) (not (blank? i)))
            (highlight-line! editor i "current-subject")))))))
