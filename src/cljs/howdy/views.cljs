(ns howdy.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [howdy.routes :as routes]
            [cljs.core.async :refer [put! chan <!]]))

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
      (html [:li
             [:a
              {:href (routes/mortgage {:id (:id m)})}
              (:name m)]]))))

(defn- new-mortgage
  [mortgages]
  {:id (inc (count mortgages))
   :name "New Mortgage"
   :startingBalance 150000})

(defn mortgages
  [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "Mortgages")
    om/IInitState
    (init-state [_]
      {:add-ch (chan)})
    om/IWillMount
    (will-mount [_]
      (let [add-ch (om/get-state owner :add-ch)]
        (go (while true
              (let [_ (<! add-ch)]
                (om/transact! app :mortgages
                              (fn [ms] (conj ms (new-mortgage ms)))))))))
    om/IRenderState
    (render-state [_ {:keys [add-ch]}]
      (html [:div
             (om/build nav app)
             [:h1 "Mortgages"]
             [:ol
              (om/build-all mortgages-li (:mortgages app) {:key :id})]
             [:button {:on-click #(put! add-ch true)} "Add Mortgage"]]))))

(defn mortgage
  [app _]
  (let [m-id (js/parseInt (get-in app [:router :params :id]) 10)
        m (first (filter #(= (:id %) m-id) (:mortgages app)))]
    (reify
      om/IDisplayName
      (display-name [_] "Mortgage")
      om/IRender
      (render [_]
        (html [:div
               (om/build nav app)
               [:h1 (:name m)]
               [:p (str "Starting balance: £" (:startingBalance m))]])))))

(defn for-page
  [page]
  (case page
    :home      home
    :mortgages mortgages
    :mortgage  mortgage))
