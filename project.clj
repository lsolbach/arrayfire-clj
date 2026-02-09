(defproject org.soulspace/arrayfire-clj "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for the ArrayFire tensor math library"
  :license {:name "Eclipse Public License 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.suskalo/coffi "1.0.615"]
                 [cnuernber/dtype-next "11.004"]
                 ;[techascent/tech.resource "5.09"] ; generalized resource management
                 ]
  :source-paths ["src"]
  :jvm-opts [; Adjust library path as needed
             "-Djava.library.path=/opt/arrayfire/lib64"
             ;"-Djava.library.path=/Program Files/ArrayFire/v3/lib"
             "--enable-native-access=ALL-UNNAMED"]
  :profiles {:dev [:user {}]
             :clay {:dependencies [[org.scicloj/clay "2.0.5"]]
                    :source-paths ["src" "notebooks"]}}
  :scm {:name "git" :url "https://github.com/lsolbach/arrayfire-clj"}
  :deploy-repositories [["clojars" {:sign-releases false :url "https://clojars.org/repo"}]]

  ;
  )
