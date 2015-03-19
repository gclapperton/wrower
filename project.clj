(defproject wrower "0.1.8-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-serial "2.0.2"]]

  :plugins [[de.lein "0.1.0-SNAPSHOT"]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ;["vcs" "commit"]
                  ["git" "commit" "test"]
                  ;["vcs" "tag" "test"]
                  ["git" "tag" "test"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
