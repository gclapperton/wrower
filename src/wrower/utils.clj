(ns wrower.utils
  (:require
   [clojure.core.async :refer [<! >! chan close! go go-loop timeout]]))

(defn ticker [ms]
  (let [c (chan)]
    (go
      (when (>! c :tick)
        (loop []
          (<! (timeout ms))
          (when (>! c :tick) (recur)))))
    c))

(defn throttle [ms in]
  (let [c (chan)]
    (go
      (if-let [v (<! in)]
        (do
          (>! c v)
          (loop []
            (<! (timeout ms))
            (if-let [v (<! in)]
              (do (>! c v) (recur))
              (close! c))))
        (close! c)))
    c))
