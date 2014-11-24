(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.router :as router :refer [set-page!]]))

(defroute home      "/"                          {:as params} (set-page! params :home))
(defroute mortgages "/m"                         {:as params} (set-page! params :mortgages))
(defroute mortgage  "/m/:mortgage-id"            {:as params} (set-page! params :mortgage))
(defroute plan      "/m/:mortgage-id/p/:plan-id" {:as params} (set-page! params :plan))

(router/init)
