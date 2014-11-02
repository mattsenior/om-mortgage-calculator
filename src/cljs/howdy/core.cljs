(ns howdy.core
  (:require [om.core :as om :include-macros true]
            [howdy.views :as views]
            [howdy.router :as router]
            [howdy.routes :as routes]))

(def app-state
  (atom {:text "Howdy Mortgages"
         :router {:view views/home}
         :mortgages [{:id 1
                      :name "A"
                      :startingBalance 250000}
                     {:id 2
                      :name "B"
                      :startingBalance 150000}]}))

(defn main []
   (let [router (router/init app-state)
         target {:target (. js/document (getElementById "app"))}]
     (om/root router app-state target)))
