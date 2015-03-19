(defproject wrower "0.1.12-release2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-serial "2.0.2"]]

  :plugins [[de.lein "0.1.1-SNAPSHOT"]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/de-bump-version" "release"]
                  ;["vcs" "commit"]
                  ["git" "commit" :name]
                  ;["vcs" "tag" "test"]
                  ["git" "tag" :name]
                  ["deploy"]
                  ["change" "version" "leiningen.release/de-bump-version"]
                  ;["vcs" "commit"]
                  ["git" "commit" :name]
                  ["vcs" "push"]])
