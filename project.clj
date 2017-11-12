(defproject knotation-editor "0.0.14-SNAPSHOT"
  :description "A front-end library implementing a Knotation editor"
  :url "http://github.com/knotation/knotation-editor"
  :license {:name "BSD 3-Clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :plugins [[lein-cljsbuild "1.1.6"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.521"]

                 [cljsjs/codemirror "5.24.0-1"]

                 [crate "0.2.4"]

                 [org.knotation/knotation-cljc "0.1.0-SNAPSHOT"]]

  :cljsbuild {:builds [{:source-paths ["src/modes" "src/knotation_editor"]
                        :compiler {:output-to "resources/knotation_editor.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}]})
