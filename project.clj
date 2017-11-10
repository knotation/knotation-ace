(defproject knotation-editor "0.0.13"
  :description "A front-end library implementing a Knotation editor"
  :url "http://github.com/knotation/knotation-editor"
  :license {:name "BSD 3-Clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :plugins [[lein-cljsbuild "1.1.6"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [cljsjs/codemirror "5.24.0-1"]

                 [crate "0.2.4"]]

  :cljsbuild {:builds [{:source-paths ["src/modes" "src/knotation_editor"]
                        :compiler {:output-to "resources/knotation_editor.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}]})
