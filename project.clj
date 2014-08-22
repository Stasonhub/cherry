(defproject cherry "0.1.0-SNAPSHOT"
  :description "Server listening to Hipchat and taking action on a Mopidy server through websocket using Wit.AI"
  :url "https://github.com/wit-ai/cherry"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2307"]
                 [org.clojure/core.async "0.1.319.0-6b1aca-alpha"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]
  :jvm-opts ["-Xmx512m"]
  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src"]
              :compiler {:output-to "dist/cherry.js"
                         :target :nodejs
                         :output-dir "out"
                         :optimizations :simple}}
             ;; advanced compil + node.js doesn't work
             #_{:id "prod"
              :source-paths ["src"]
              :compiler {:output-to "dist/cherry.min.js"
                         :target :nodejs
                         :output-dir "out-prod"
                         :optimizations :advanced}}]})
