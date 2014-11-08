(ns howdy.core
  (:require [om.core :as om :include-macros true]
            [howdy.views :as views]
            [howdy.router :as router]))

(def app-state
  (atom {:text "Howdy Mortgages"
         :router {:page :home, :params {}}
         :mortgages [{:id 1
                      :name "A"
                      :startBalance 250000
                      :startYear 2014
                      :startMonth 4
                      :plans [{:id 1
                               :name "Suggested"
                               :values [{:year 0
                                         :month 0
                                         :interestRate 2.99
                                         :regularPayment 1200
                                         :oneOffPayment 0}
                                        {:year 6
                                         :month 1
                                         :interestRate 5.99
                                         :regularPayment 1500}]
                               }] }
                     {:id 2
                      :name "B"
                      :startBalance 150000
                      :plans []}]}))

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
