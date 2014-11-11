(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.router :as router :refer [set-page!]]))

(defroute home      "/"                                      {:as params} (set-page! params :home))
(defroute mortgages "/mortgages"                             {:as params} (set-page! params :mortgages))
(defroute mortgage  "/mortgages/:mortgage-id"                {:as params} (set-page! params :mortgage))
(defroute plan      "/mortgages/:mortgage-id/plans/:plan-id" {:as params} (set-page! params :plan))

(router/init)
