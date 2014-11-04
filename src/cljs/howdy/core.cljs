(ns howdy.core
  (:require [om.core :as om :include-macros true]
            [howdy.views :as views]
            [howdy.router :as router]))

(def app-state
  (atom {:text "Howdy Mortgages"
         :router {:page :home, :params {}}
         :mortgages [{:id 1
                      :name "A"
                      :startingBalance 250000}
                     {:id 2
                      :name "B"
                      :startingBalance 150000}]}))

(defn main []
  (let [target {:target (. js/document (getElementById "app"))}]
    (om/root
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name [_] "Root")
         om/IRender
         (render [this]
           (om/build (views/for-page (get-in app [:router :page])) app))))
     app-state
     target)))
