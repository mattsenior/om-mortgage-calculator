(ns howdy.views
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [howdy.router :as router]
            [howdy.routes :as routes]))

(defn nav
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:ol
        [:li [:a {:href "/"} "Home"]]
        [:li [:a {:href "/mortgages"} "Mortgages"]]]))))

;; Home
(defn home
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:div
             (om/build nav app)
             [:h1 "Home"]]))))

;; Mortgages

(defn mortgages-li
  [m _]
  (reify
    om/IDisplayName
    (display-name [_] "MortgagesLi")
    om/IRender
    (render [_]
      (html [:li "h"]))))

(defn mortgages
  [app _]
  (reify
    om/IDisplayName
    (display-name [_] "Mortgages")
    om/IRender
    (render [_]
      (html [:div
              (om/build nav app)
              [:h1 "Mortgages"]
              [:ol
               (om/build-all mortgages-li (:mortgages app) {:key :id})]]))))

