(defproject foursquare-1self "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :min-lein-version "2.0.0"
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [compojure "1.3.1"]
                           [ring/ring-defaults "0.1.2"]
                           [com.cemerick/friend "0.2.1"]
                           [friend-oauth2 "0.1.1" :exclusions [org.apache.httpcomponents/httpcore]]
                           [cheshire "5.2.0"]]
            :plugins [[lein-ring "0.8.13"]]
            :ring {:handler foursquare-1self.handler/app}
            :profiles
            {:dev        {:dependencies [[javax.servlet/servlet-api "2.5"]
                                         [ring-mock "0.1.5"]]}

             :foursquare {:ring {:handler foursquare-1self.handler/app}}}

            :aliases {"foursquare" ["with-profile" "dev,foursquare"
                                    "do" "ring" "server-headless"]
                      })
