(ns howdy.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [howdy.views :as views]
            [howdy.core :refer [app-state]]))

(defn- swap-view!
  "Route handler to simply swap the chosen view into the
  app state"
  [app params view]
  (swap! app assoc :router {:view view :params params}))

(defroute home            "/"          {:as params} (swap-view! app-state params views/home))
(defroute mortgages "/mortgages" {:as params} (swap-view! app-state params views/mortgages))
(defroute mortgage "/mortgage" {:as params} (swap-view! app-state params views/mortgage))
