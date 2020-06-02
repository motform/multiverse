(ns multiverse.cli
  (:gen-class)
  (:require [clojure.string :as str]
            [multiverse.ml :as ml]
            [multiverse.util :as util]))

(defn prompt
  ([] (prompt ""))
  ([message]
   (when (not (str/blank? message)) (println message))
   (print "> ") (flush) (read-line)))

(defn select [selection]
  (let [*in (atom nil)]
    (while (not (int? @*in))
      (reset! *in (read-string (prompt))))
    (nth selection (dec @*in))))

(defn clear-term! []
  (print (str (char 27) "[2J")
         (str (char 27) "[;H")))

(defn progress [story continuation]
  (str story "\n" continuation))

(defn print-story [story]
  (clear-term!)
  (println story "\n"))

(defn -main []
  (clear-term!)
  (loop [story (prompt "Prompt the network, the more detailed the better")]
    (print-story story)
    (let [continuation (ml/generate-sentences story 2)]
      (run! println (util/enumerate continuation))
      (recur (progress story (select continuation))))))
