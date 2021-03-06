(ns howdy.views
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [howdy.routes :as routes]
            [howdy.router :as router :refer [redirect!]]
            [howdy.mortgage :refer [get-mortgage-lifespan]]
            [cljs.core.async :refer [put! take! chan <!]]))

(defn for-page
  [page]
  (case page
    :home      home
    :mortgages mortgages
    :mortgage  mortgage
    :plan      plan))

;; Components

(defn nav
  "Navigation component"
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html
       [:ol
        [:li [:a {:href (routes/home)} "Home"]]
        [:li [:a {:href (routes/mortgage {:mortgage-id "a"})} "A"]]
        [:li [:a {:href (routes/mortgage {:mortgage-id "b"})} "B"]]]))))

(defn home
  "Home page"
  [app owner]
  (reify
    om/IRender
    (render [this]
      (html [:div
             (om/build nav app)
             [:h1 "Home"]]))))

(defn mortgages
  "Mortgages index page - will actually generate a new mortgage and
  redirect to it instead of rendering a list"
  [app owner]
  (reify
    om/IRender
    (render [_]
      (let [new-ch (chan)]
        (put! (:mortgage-control-ch (om/get-shared owner)) [:mortgages/add new-ch])
        (take! new-ch
               #(redirect! (routes/mortgage {:mortgage-id (:id %)})))))))

(defn mortgage
  [mortgage _]
  (reify
    om/IDisplayName
    (display-name [_] "Mortgage")
    om/IDidMount
    (did-mount [_]
      ;; Load mortgage from server
      (.log js/console "Load mortgage from server")
      ;; 404 if not found
      )
    om/IRender
    (render [_]
      (html [:div
             [:p
              "A Mortgage"
              [:span "Starting balance: £"]
             ]]))))


;; Abstract bits
(comment

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
  (let [id (get-in app [:router :params :mortgage-id])]
    (->> (:mortgages app)
         (filter #(= (:id %) id))
         first)))

  (defn- get-current-plan
  "Lookup current plan from router params"
  [app]
  (let [m (get-current-mortgage app)
        id (get-in app [:router :params :plan-id])]
    (->> (:plans m)
         (filter #(= (:id %) id))
         first)))

  ;; Components


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
                {:href (routes/mortgage {:mortgage-id (:id m)})}
                (:name m)]
               [:button
                {:on-click #(put! (:mortgage-control-ch (om/get-shared owner)) [:mortgages/delete @m])
                 :title (str "Delete " (:name m))}
                "×"]]))))

  


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
          (go-loop []
            (let [new-ch (<! add-ch)
                  new-p (new-plan (:plans @m))]
              (om/transact! m :plans
                            (fn [ps] (conj ps new-p)))
              (put! new-ch new-p))
            (recur))))
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
                                      #(redirect! (routes/plan {:mortgage-id (:id @m), :plan-id (:id %)})))))}
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
                {:href (routes/plan {:mortgage-id (:id m), :plan-id (:id p)})}
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
        (redirect! (routes/mortgage {:mortgage-id (:id m)})))
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
                  (om/build editable m {:opts {:edit-key :start-balance
                                               :on-edit #(om/transact! m :start-balance sanitise-currency)
                                               :element :span}})]
                 (om/build plans m)
                 (om/build editable p {:opts {:edit-key :name
                                              :on-edit #(on-edit %)
                                              :element :h2}})
                 (let [ml (get-mortgage-lifespan m p)
                       filtered-ml (filter #(zero? (mod (:month-number %) 8)) ml)
                       stats (last ml)]
                   [:div
                    [:p (str "Total paid: £" (:total-paid stats))]
                    [:p (str "Months: " (:month-number stats))]
                    (map (fn [{:keys [month-number total-debt] :as month}]
                           [:div {:key month-number
                                  :style {:width (str (* 50 (/ total-debt (:start-balance m))) "%")
                                          :height "10px"
                                          :background-color "gold"}}
                            ])
                         filtered-ml)])]))))))
