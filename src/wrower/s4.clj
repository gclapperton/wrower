(ns wrower.s4
  (:require
   [clojure.core.async :refer [<! >! chan close! go-loop pipe put! sliding-buffer]]
   [wrower.utils :refer [throttle]]
   [wrower.serial :as sp]))

(def commands
  {:start             "USB"
   :reset             "RESET"
   :stop              "EXIT"
   :info              "IV?"
   :intensity-mps     "DIMS"
   :intensity-mph     "DIMPH"
   :intensity-500     "DI500"
   :intensity-2km     "DI2KM"
   :intensity-watts   "DIWA"
   :intensity-cal-ph  "DICH"
   :intensity-avg-mps "DAMS"
   :intensity-avg-mph "DAMPH"
   :intensity-avg-500 "DA500"
   :intensity-avg-2km "DA2KM"
   :distance-meters   "DDME"
   :distance-miles    "DDMI"
   :distance-km       "DDKM"
   :distance-strokes  "DDST"})

(def memory-map
  {"054" {:type :total-distance-dec    :size :single :base 16}
   "055" {:type :total-distance-m      :size :double :base 16}
   "05A" {:type :clock-countdown-dec   :size :single :base 16}
   "05B" {:type :clock-countdown       :size :double :base 16}
   "140" {:type :total-strokes         :size :double :base 16}
   "1A9" {:type :stroke-rate           :size :single :base 16}
   "08A" {:type :total-kcal            :size :triple :base 16}
   "1A0" {:type :heart-rate            :size :single :base 16}
   "14A" {:type :avg-distance-cmps     :size :double :base 16}
   "1E0" {:type :display-sec-dec       :size :single :base 10}
   "1E1" {:type :display-sec           :size :single :base 10}
   "1E2" {:type :display-min           :size :single :base 10}
   "1E3" {:type :display-hr            :size :single :base 10}
   "1E8" {:type :total-workout-time    :size :double :base 16}
   "1EA" {:type :total-workout-mps     :size :double :base 16}
   "1EC" {:type :total-workout-strokes :size :double :base 16}})

(defn event
  [type & [value]]
  {:type type :value value :at (System/currentTimeMillis)})

(defn read-reply
  [s]
  (if-let [{:keys [type size base]} (get memory-map (subs s 3 6))]
    (event type (condp = size
                  :single (Integer/valueOf (subs s 6 8) base)
                  :double (Integer/valueOf (subs s 6 10) base)
                  :triple (Integer/valueOf (subs s 6 12) base)))
    (.println *err* (str "cannot read reply from " s))))

(def ping         (partial event :ping))
(def pulse        (partial event :pulse))
(def stroke-start (partial event :stroke-start))
(def stroke-end   (partial event :stroke-end))
(def ok           (partial event :ok))
(def error        (partial event :error))
(def model        (partial event :model))

(defn event-for
  [cmd]
  (try
      (cond
       (= "PING"  cmd)            (ping)
       (= \P      (first cmd))    (pulse (Integer/valueOf (subs cmd 1) 16))
       (= "SS"    cmd)            (stroke-start)
       (= "SE"    cmd)            (stroke-end)
       (= "OK"    cmd)            (ok)
       (= "IV"    (subs cmd 0 2)) (model (subs cmd 2))
       (= "IDS"   (subs cmd 0 3)) (read-reply cmd)
       (= "IDD"   (subs cmd 0 3)) (read-reply cmd)
       (= "IDT"   (subs cmd 0 3)) (read-reply cmd)
       (= "ERROR" cmd)            (error)
       :else                      (event :unknown cmd))
    (catch Exception e
      (.println *err* (str "failure finding event for " cmd)))))

(def map-events
  (comp
    (map char)
    (filter #(not= % \return))
    (partition-by #(= % \newline))
    (map (partial apply str))
    (filter #(not= % (str \newline)))
    (map event-for)))

(def map-commands
  (map (fn [cmd]
        (-> (if (keyword? cmd) (get commands cmd) cmd)
            ((fn [cmd] (.toUpperCase cmd)))
            (str "\r\n")))))

    ; it is recommended a PC sends data no faster than every 25mS (1 packet)
(defn connect!
  ([port commands] (connect! port commands (chan)))
  ([port commands events]
    (let [to-sp     (chan 1 map-commands)
          throttled (throttle 25 commands)
          from-sp   (chan (sliding-buffer 100) map-events)]

      (pipe from-sp events)

      (sp/connect! port to-sp from-sp)

      (go-loop []
        (if-let [cmd (<! throttled)]
          (do (>! to-sp cmd)
              (recur))
          (do (>! to-sp :stop)
              (close! to-sp))))

      (put! to-sp :start)

      events)))

(comment
  (def cmd-chan (chan))
  (def events (connect! "tty.usbmodem1411" cmd-chan))

  (go-loop []
    (when-let [c (<! events)]
      (println c)
      (recur)))

  (close! cmd-chan))
