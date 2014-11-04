(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.core :refer [app-state]]
            [howdy.router :as router]))

(defn- set-page!
  "Route handler to simply swap the chosen page in the app state"
  [params page]
  (swap! app-state assoc :router {:page page :params params}))

(defroute home      "/"              {:as params} (set-page! params :home))
(defroute mortgages "/mortgages"     {:as params} (set-page! params :mortgages))
(defroute mortgage  "/mortgages/:id" {:as params} (set-page! params :mortgage))

(router/init)
