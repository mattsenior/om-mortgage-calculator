(ns howdy.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [howdy.routes :as routes]
            [howdy.router :refer [redirect!]]
            [cljs.core.async :refer [put! chan <!]]))

;; Abstract bits

(defn- display [show?]
  (if show? {:color "green"} {:display "none"}))

(defn- handle-change [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn- end-edit [text owner cb]
  (om/set-state! owner :editing false)
  (cb text))

(defn- editable
  [data owner {:keys [edit-key on-edit element] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:editing false})
    om/IRenderState
    (render-state [_ {:keys [editing]}]
      (let [text (get data edit-key)]
        (html
         [element
          [:span {:style (display (not editing))
                  :on-double-click (fn [e]
                                     (om/set-state! owner :editing true))} text]
          [:input {:style (display editing)
                   :ref "input"
                   :value text
                   :on-change #(handle-change % data edit-key owner)
                   :on-key-up (fn [e]
                                  (when (== (.-keyCode e) 13)
                                    (end-edit text owner on-edit)))
                   :on-blur #(when (om/get-state owner :editing)
                               (end-edit text owner on-edit))}]])))
    om/IDidUpdate
    (did-update [_ _ prev-state]
      (when (and (om/get-state owner :editing)
                 (not (:editing prev-state)))
        (let [element (om/get-node owner "input")]
          (.focus element)
          (.setSelectionRange element 0 (.. element -value -length)))))))

;; Handler

(defn- on-edit [text]
  (.log js/console (str "Edited to " text)))

;; Components

(defn nav
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:ol
        [:li [:a {:href (routes/home)} "Home"]]
        [:li [:a {:href (routes/mortgages)} "Mortgages"]]]))))

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

(defn- sanitise-currency
  [input]
  (-> input
      (clojure.string/replace #"[^\d\.]" "")
      (js/parseFloat)))

(defn mortgage
  [app _]
  (let [m-id (js/parseInt (get-in app [:router :params :id]) 10)
        m (first (filter #(= (:id %) m-id) (:mortgages app)))]
    ;; If we don’t have this id, we can redirect away
    (when-not m
      (redirect! (routes/mortgages)))
    (reify
      om/IDisplayName
      (display-name [_] "Mortgage")
      om/IRender
      (render [_]
        (html [:div
               (om/build nav app)
               (om/build editable m {:opts {:edit-key :name
                                            :on-edit #(on-edit %)
                                            :element :h1}})
               [:p
                [:span "Starting balance: £"]
                (om/build editable m {:opts {:edit-key :startingBalance
                                             :on-edit #(om/transact! m :startingBalance sanitise-currency)
                                             :element :span}})]])))))

(defn for-page
  [page]
  (case page
    :home      home
    :mortgages mortgages
    :mortgage  mortgage))
