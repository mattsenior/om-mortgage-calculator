(ns howdy.core
  (:require [om.core :as om :include-macros true]
            [howdy.views :as views]
            [howdy.router :as router]))

(def app-state (atom {:text "Howdy Chestnut!"
                      :messages ["Hello" "hello again" "Bye"]
                      :router {:view views/jobs}}))

(def routes {"/"        views/jobs
             "/job/:id" views/job
             "/post"    views/job-post})

(defn main []
   (let [router (router/init routes app-state)
         target {:target (. js/document (getElementById "app"))}]
     (om/root router app-state target)))
