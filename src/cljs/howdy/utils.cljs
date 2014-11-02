(ns howdy.utils
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [chan put!]]
            [goog.events :as events]))

(defn listen
  "Listen on the given element for events of the given
  type, putting events onto a channel. Will create a new
  channel and return it if none provided."
  ([el type] (listen element type (chan)))
  ([el type ch]
     (events/listen element
                    type
                    #(put! ch %))
     ;; Return the channel of events
     ch))
