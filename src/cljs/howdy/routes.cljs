(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.router :as router]
            [howdy.core :refer [app-state]]))

(defn- set-page!
  "Route handler to simply swap the chosen page in the app state"
  [params page]
  (swap! app-state assoc :router {:page page :params params}))

(defroute home      "/"                                    {:as params} (set-page! params :home))
(defroute mortgages "/mortgages"                           {:as params} (set-page! params :mortgages))
(defroute mortgage  "/mortgages/:mortgageId"               {:as params} (set-page! params :mortgage))
(defroute mortgages "/mortgages"                           {:as params} (set-page! params :mortgages))
(defroute plan      "/mortgages/:mortgageId/plans/:planId" {:as params} (set-page! params :plan))

(router/init)
