(ns wrower.serial
  (:require
   [clojure.core.async :refer [<! close! go-loop put!]]
   [serial.core :as sp]))

(defn list-ports []
  (->> (sp/port-ids) (map #(.getName %))))

(defn connect! [port in out]
  (let [sp       (sp/open port)]
    (sp/listen sp #(put! out (.read %)) false)

    (go-loop []
      (if-let [cmd (<! in)]
        (do (sp/write sp (.getBytes cmd))
            (recur))
        (do (sp/close sp)
            (close! out))))

    out))
