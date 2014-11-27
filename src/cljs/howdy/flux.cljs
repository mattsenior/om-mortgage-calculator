(ns howdy.flux
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [>! <! pub sub unsub put! chan]]))

(def ^:private flux-ch (chan))
(def ^:private flux (pub flux-ch first))

(defn register [topic]
  (sub flux topic (chan)))

(defn unregister [topic ch]
  (unsub flux topic ch))

(defn dispatch! [& topic-and-args]
  (put! flux-ch topic-and-args))


(defn store
  [source topic f]
  (let [in (sub source topic (chan))
        out (chan)
        out-pub (pub out first)]
    (go-loop []
      (let [event (<! in)
            [_ payload] event]
        (.log js/console (pr-str "Event here" event))
        (f payload)
        (>! out event))
      (recur))
    (.log js/console (pr-str out-pub))
    out-pub)
    )

(def store-a
  (store flux :topic (fn [e] (.log js/console (pr-str "Whoa" e)))))

(let [blah (sub store-a :topic (chan))]
  (go-loop []
    (let [event (<! blah)]
      (.log js/console (pr-str "Received from store-a" event)))
    (recur)))
