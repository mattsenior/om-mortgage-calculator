(ns howdy.views
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [howdy.routes :as routes]
            [howdy.router :refer [redirect!]]
            [cljs.core.async :refer [put! take! chan <!]]))

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

;; Helpers

(defn- sanitise-currency
  [input]
  (let [v (-> input
              (clojure.string/replace #"[^\d\.]" "")
              (js/parseFloat))]
    (if (js/isNaN v) 0 v)))

(defn- get-current-mortgage
  "Lookup current mortgage from router params"
  [app]
  (let [id (get-in app [:router :params :mortgageId])]
    (->> (:mortgages app)
         (filter #(= (:id %) id))
         first)))

(defn- get-current-plan
  "Lookup current plan from router params"
  [app]
  (let [m (get-current-mortgage app)
        id (get-in app [:router :params :planId])]
    (->> (:plans m)
         (filter #(= (:id %) id))
         first)))

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
  [m owner]
  (reify
    om/IDisplayName
    (display-name [_] "MortgagesLi")
    om/IRender
    (render [_]
      (html [:li
             [:a
              {:href (routes/mortgage {:mortgageId (:id m)})}
              (:name m)]
             [:button
              {:on-click #(put! (:mortgage-control-ch (om/get-shared owner)) [:mortgages/delete @m])
               :title (str "Delete " (:name m))}
              "×"]]))))

(defn mortgages
  [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "Mortgages")
    om/IRenderState
    (render-state [_ {:keys [add-ch]}]
      (html [:div
             (om/build nav app)
             [:h1 "Mortgages"]
             [:ol
              (om/build-all mortgages-li (:mortgages app) {:key :id})]
             [:button
              {:on-click (fn [_]
                           (let [new-ch (chan)]
                             (put! (:mortgage-control-ch (om/get-shared owner)) [:mortgages/add new-ch])
                             (take! new-ch
                                    #(redirect! (routes/mortgage {:mortgageId (:id %)})))))}
              "Add Mortgage"]]))))

(defn mortgage
  [app _]
  (let [m (get-current-mortgage app)]
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
                (om/build editable m {:opts {:edit-key :startBalance
                                             :on-edit #(om/transact! m :startBalance sanitise-currency)
                                             :element :span}})]
               (om/build plans m)])))))

(defn plans
  [m owner]
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
              (let [new-ch (<! add-ch)
                    new-p (new-plan (:plans @m))]
                (om/transact! m :plans
                              (fn [ps] (conj ps new-p)))
                (put! new-ch new-p))))))
    om/IRenderState
    (render-state [_ {:keys [add-ch]}]
      (html [:div
             [:ol
              (om/build-all plans-li (:plans m) {:key :id, :opts {:mortgage m}})]
             [:button
              {:on-click (fn [_]
                           (let [new-ch (chan)]
                             (put! (:mortgage-control-ch (om/get-shared owner)) [:plans/add m new-ch])
                             (take! new-ch
                                    #(redirect! (routes/plan {:mortgageId (:id @m), :planId (:id %)})))))}
              "Add Plan"]]))))

(defn plans-li
  [p owner {m :mortgage}]
  (reify
    om/IDisplayName
    (display-name [_] "PlansLi")
    om/IRender
    (render [_]
      (html [:li
             [:a
              {:href (routes/plan {:mortgageId (:id m), :planId (:id p)})}
              (:name p)]
             [:button
              {:on-click #(put! (:mortgage-control-ch (om/get-shared owner)) [:plans/delete m @p])
               :title (str "Delete " (:name p))}
              "×"]]))))

(defn plan
  [app _]
  (let [m (get-current-mortgage app)
        p (get-current-plan app)]
    ;; If we don’t have this id, we can redirect away
    (when-not p
      (redirect! (routes/mortgage {:mortgageId (:id m)})))
    (when-not m
      (redirect! (routes/mortgages)))
    (reify
      om/IDisplayName
      (display-name [_] "Plan")
      om/IRender
      (render [_]
        (html [:div
               (om/build nav app)
               (om/build editable m {:opts {:edit-key :name
                                            :on-edit #(on-edit %)
                                            :element :h1}})
               [:p
                [:span "Starting balance: £"]
                (om/build editable m {:opts {:edit-key :startBalance
                                             :on-edit #(om/transact! m :startBalance sanitise-currency)
                                             :element :span}})]
               (om/build editable p {:opts {:edit-key :name
                                            :on-edit #(on-edit %)
                                            :element :h1}})])))))

(defn for-page
  [page]
  (case page
    :home      home
    :mortgages mortgages
    :mortgage  mortgage
    :plan      plan))
