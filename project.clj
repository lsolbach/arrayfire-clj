(defproject arrayfire-clj "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for the ArrayFire tensor math library"
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [org.suskalo/coffi "1.0.615"]
                 [cnuernber/dtype-next "11.004"]
                 [techascent/tech.resource "5.09"] ; generalized resource management
                 ]
  :source-paths ["src"]
  :jvm-opts [  ; Adjust library path as needed
             "-Djava.library.path=/opt/arrayfire/lib64"
             ;"-Djava.library.path=/Program Files/ArrayFire/v3/lib"
             "--enable-native-access=ALL-UNNAMED"]
  ;
  )
