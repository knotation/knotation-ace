(defproject org.knotation/knotation-editor "1.0.1-SNAPSHOT"
  :description "A front-end library implementing a Knotation editor"
  :url "http://github.com/knotation/knotation-editor"
  :license {:name "BSD 3-Clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :plugins [[lein-cljsbuild "1.1.6"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]

                 [cljsjs/codemirror "5.24.0-1"]

                 [crate "0.2.4"]

                 [instaparse "1.4.8"]
                 [org.knotation/knotation-cljc "0.2.0-SNAPSHOT"]]

  :cljsbuild {:builds [{:source-paths ["src/org/knotation/editor/modes" "src/org/knotation/editor"]
                        :compiler {:output-to "resources/knotation_editor.js"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :language-in :es5}
                        :jar true}]})
