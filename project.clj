(defproject genesis "0.1.0-SNAPSHOT"
  :description "HA and horizontally scalable framework for backend services."
  :url "https://github.com/aamedina/genesis"
  :license {:name "GNU General Public License"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.typed "0.2.75"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [org.clojure/core.cache "0.6.5-SNAPSHOT"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]
                 [org.clojure/test.check "0.6.2"]
                 [com.datomic/datomic-pro "0.9.5078"
                  :exclusions [joda-time org.slf4j/slf4j-nop]]
                 [com.cognitect/transit-clj "0.8.259"]
                 [com.stuartsierra/component "0.2.2"]
                 [io.netty/netty-all "5.0.0.Alpha2-SNAPSHOT"]
                 [com.hazelcast/hazelcast-all "3.4"]
                 [javax.cache/cache-api "1.0.0"]
                 [org.glassfish/javax.el "3.0.0"]
                 [org.eclipse.jetty/jetty-server "9.2.6.v20141205"]
                 [org.eclipse.jetty/jetty-webapp "9.2.6.v20141205"]
                 [org.eclipse.jetty/jetty-jsp "9.2.6.v20141205"
                  :exclusions [org.glassfish/javax.el]]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :global-vars {*assert* false}
  :repositories {"sonatype" "https://oss.sonatype.org/content/groups/public/"
                 "my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :jvm-opts ["-server"
             "-Dhazelcast.config=resources/hazelcast.xml"
             "-Dlog4j.configurationFile=resources/log4j2.xml"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.8"]
                                  [criterium "0.4.3"]]
                   :global-vars {*assert* true}
                   :env {:datomic-uri "datomic:mem://genesis"
                         :mancenter-host "127.0.0.1"
                         :mancenter-port 8080
                         :mancenter-war "resources/mancenter-3.4.war"}
                   :source-paths ["dev"]}
             
             :test [:dev
                    {:main genesis.main
                     :aot :all
                     :global-vars {*warn-on-reflection* true
                                   *assert* true}}]})
