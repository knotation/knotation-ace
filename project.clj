(defproject knotation-ace "0.1.0-SNAPSHOT"
  :description "A front-end library implementing a Knotation editor using Ace"
  :url "http://github.com/knotation/knotation-ace"
  :license {:name "BSD 3-Clause License"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :plugins [[lein-cljsbuild "1.1.6"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [crate "0.2.4"]]

  :cljsbuild {:builds [{:source-paths ["src/knotation_ace"]
                        :compiler {:output-to "resources/knotation_ace.js"
                                   :optimizations :whitespace
                                   :pretty-print true}
                        :jar true}]}
  :main knotation-ace.core
  :aot [knotation-ace.core])
