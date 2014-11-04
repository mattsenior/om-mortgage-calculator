(ns howdy.router
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! put! chan]]
            [om.core :as om]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [goog.events.EventType :as EventType]
            [secretary.core :as secretary]
            [sablono.core :refer-macros [html]])
  (:import goog.history.Html5History
           goog.Uri))

;; Google History object
(def history (Html5History.))

;; Locations + redirecting
(def locations (chan))

(defn redirect!
  "Helper for redirecting to given location"
  [token]
  (put! locations token))

(go (while true
      (let [token (<! locations)]
        (.setToken history token))))

(def ^:private current-uri (.parse Uri (.-href (.-location js/document))))

(defn- find-href
  "Get the href from the target element, walking up
  through parent nodes until we find an href. Nil if
  we don’t."
  [node]
  (if-let [href (.-href node)]
    href
    (when-let [parent (.-parentNode node)]
      (recur parent))))

(defn- handle-document-clicks
  "Click handler for all document clicks. For hijacking
  href links and using redirect! instead of following
  url. Walk up the DOM from the target until we find an
  element with an href to follow."
  [e]
  (when-let [href (find-href (.-target e))]
    (let [href-uri (.parse Uri href)]
      (when (and (.hasSameDomainAs href-uri current-uri)
                 (= (.getScheme href-uri) (.getScheme current-uri)))
        (let [path (.getPath href-uri)]
          (when (secretary/locate-route path)
            (.preventDefault e)
            (redirect! path)))))))

(defn init
  "Add our handlers and fire the initial navigate event."
  []
  
  ;; Listen for href clicks
  (events/listen js/document
                 EventType/CLICK
                 handle-document-clicks)


  ;; Add history event listener to dispatch!
  (events/listen history
                 HistoryEventType/NAVIGATE
                 (fn [e]
                   (secretary/dispatch! (.-token e))))

  ;; Don’t use #
  (.setUseFragment history false)
  (.setPathPrefix history "")

  ;; Activate history watching - immediately fires NAVIGATE
  (.setEnabled history true))
