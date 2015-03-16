(ns wrower.core
  (:require
   [clojure.core.async :refer [<! >! close! go go-loop chan alt! put! timeout]]
   [wrower.utils :refer [ticker]]
   [wrower.s4 :refer [connect! memory-map]]))

(defn request-data [cmd-chan]
  (doseq [[addr {size :size}] memory-map]
    (let [cmd (condp = size
                :single "IRS"
                :double "IRD"
                :triple "IRT")]
      (put! cmd-chan (str cmd addr)))))

(defn rower [port commands]
  (let [cmd-chan (chan)
        ticker   (ticker 400)
        stream   (connect! port cmd-chan)]

    (go-loop []
      (alt!
         commands ([v] (if v
                         (do (>! cmd-chan v) (recur))
                         (do (close! ticker) (close! cmd-chan))))
         ticker   ([_] (request-data cmd-chan) (recur))))

    stream))

(comment
  (def cmd-chan (chan))
  (def events (rower "tty.usbmodem1411" cmd-chan))

  (go-loop []
    (when-let [c (<! events)]
      (when-not (#{:ping :pulse} (:type c))
        (println c))
      (recur)))

  (close! cmd-chan))



