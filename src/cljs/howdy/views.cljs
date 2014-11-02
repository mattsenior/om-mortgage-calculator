(ns howdy.views
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [howdy.router :as router]))

(defn message
  [m owner]
  (reify
    om/IRender
    (render [this] (html [:li m]))))

(defn message-list
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:ol (om/build-all message (:messages app))])))) 

(defn jobs
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:div
             [:a {:href "/"} "Home"]
             [:a {:href "http://google.com/"} "G"]
             [:a {:href "https://localhost:10555/"} "HTTPS"]
             [:p "Jobs"]
             [:a {:href "/job/1"} "Job 1"]]))))

(defn job
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:div
             [:a {:href "/"} "Home"]
             [:a {:href "http://google.com/"} "G"]
             [:a {:href "https://localhost:10555/"} "HTTPS"]
             [:h1 "Job"]
             [:p (:text app)]]))))

(defn job-post
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:p "Job post"]))))
