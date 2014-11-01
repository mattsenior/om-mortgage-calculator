(ns howdy.router
  (:require [goog.events :as events]
            [om.core :as om]
            [goog.history.EventType :as EventType]
            [secretary.core :refer [add-route! dispatch!]]
            [sablono.core :refer-macros [html]])
  (:import goog.history.Html5History))

(def history (Html5History.))

(defn init
  "Add Secretary routes, hook up history, and return
   an Om component to use as root"
  [routes app]

  (doseq [[route view] routes]
    (add-route! route
                #(swap! app
                        assoc
                        :router
                        {:view view :params %})))

  (events/listen history
                 EventType/NAVIGATE
                 (fn [e]
                   (js/console.log (str "Token " (.-token e)))
                   (dispatch! (.-token e))
                   (.preventDefault e))
                 )

  (.setUseFragment history false)
  (.setPathPrefix history "")
  (.setEnabled history true)

  (fn [app owner]
    (reify
      om/IRender
      (render [this]
        (om/build (get-in app [:router :view]) app)))))

(defn redirect!
  [location]
  (js/console.log "Redirecting")
  (.setToken history location))
