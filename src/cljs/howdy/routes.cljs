(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.router :as router :refer [set-page!]]))

(defroute home      "/"                                    {:as params} (set-page! params :home))
(defroute mortgages "/mortgages"                           {:as params} (set-page! params :mortgages))
(defroute mortgage  "/mortgages/:mortgageId"               {:as params} (set-page! params :mortgage))
(defroute plan      "/mortgages/:mortgageId/plans/:planId" {:as params} (set-page! params :plan))

(router/init)
