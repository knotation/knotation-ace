(ns knotation-editor.editor
  (:require [cljsjs.codemirror]))

(defn log! [& things]
  (apply js/console.log (map clj->js things)))

(defn dom-loaded [fn]
  (.addEventListener js/document "DOMContentLoaded" fn))

(defn editor! [editor-id & {:keys [theme mode focus?]
                            :or {mode "sqlserver"
                                 theme "github"
                                 focus? true}}]
  (log! "Testing testing! CM: " js/CodeMirror)
  ;; (let [editor (.edit js/ace editor-id)]
  ;;   (.setTheme editor theme)
  ;;   (-> editor (.getSession) (.setMode mode))
  ;;   (when focus? (.focus editor))
  ;;   editor)
  )

;; (defn add-command!
;;   [ed name keys fn]
;;   (.addCommand
;;    (.-commands ed)
;;    (clj->js {:name name :bindKey keys :exec fn})))

;; (defn add-commands!
;;   [ed & commands]
;;   (doseq [[name keys f] commands]
;;     (add-command! ed name keys f)))
