(ns howdy.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [om.core :as om :include-macros true]
            [cljs.reader :as reader]
            [howdy.views :as views]
            [howdy.router :as router]
            [howdy.flux :as f]
            [cljs.core.async :refer [chan <! >!]]
            [cljs-uuid.core :as uuid]
            [cljs-time.core :as time]
            [taoensso.sente :as sente :refer [cb-success?]]))


(defn main []
  (.log js/console "blah")
  (f/dispatch! :topic "Blumb")
  )


(comment
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket! "/chsk"
                                    {:type :auto ;; e/o #{:auto :ajax :ws}
                                     })]
    (def chsk       chsk)
    (def ch-chsk    ch-recv) ;; ChannelSocket’s receive channel
    (def chsk-send! send-fn) ;; ChannelSocket’s send API fn
    (def chsk-state state))  ;; Watchable, read-only atom

  (def app-state
    (atom {:router {:page :home, :params {}}
           :mortgage {}}))

  (defn- new-mortgage []
    (let [now (time/now)]
      {:id (str (uuid/make-random))
       :name "Our New House"
       :start-balance 150000
       :start-year (time/year now)
       :start-month (inc (time/month now))
       :plans-next-id 1
       :plans [{:id (str (uuid/make-random))
                :name "Big Blue Building Society"
                :values [{:year 1
                          :month 1
                          :interest-rate 2.99
                          :regular-payment 1188.23
                          :one-off-payment 0}
                         {:year 6
                          :month 1
                          :interest-rate 5.99
                          :regular-payment 1535.11}]}
               {:id (str (uuid/make-random))
                :name "Big Blue with overpayment"
                :values [{:year 1
                          :month 1
                          :interest-rate 2.99
                          :regular-payment 1188.23
                          :one-off-payment 2000}
                         {:year 6
                          :month 1
                          :interest-rate 5.99
                          :regular-payment 1535.11}]}]}))

  (defn- new-plan
    [plans]
    ;; Generate a random Id
    (let [id (->> (uuid/make-random)
                  str
                  string.split
                  (take 8)
                  string.join)]
      (if-not (get-plan-from-plans id plans)
        ;; Return the new plan map
        {:id id
         :name "New Plan"
         :values []}
        ;; Try again if this Id exists
        (recur plans))))

  (defn- handle-page-change
    [app {:keys [page params]}]
    (om/update! app :router {:page page :params params}))

  (defn main []
    (let [mortgage-control-ch (chan)]
      (om/root
       (fn [app owner]
         (reify
           om/IDisplayName
           (display-name [_] "Root")
           om/IWillMount
           (will-mount [_]
             ;; Find out the first page, park here until we get it
             ;; so we don’t render anything before
             (take! router/page-changes-ch #(handle-page-change app %))
             ;; Now spin up a go loop to handle further changes
             (go-loop []
               (handle-page-change app (<! router/page-changes-ch))
               (recur))

             ;; Add/delete mortgages
             (go-loop []
               (let [[action & params] (<! mortgage-control-ch)]
                 (case action
                   :mortgages/add
                   (let [[new-ch] params
                         new-m (new-mortgage)]
                     (flux)
                     (om/transact! app
                                   :mortgages
                                   (fn [ms]
                                     (conj ms new-m)))
                     (put! new-ch new-m))
                   :mortgages/delete
                   (let [[m] params]
                     (om/transact! app
                                   :mortgages
                                   (fn [ms]
                                     (vec (remove #(= m %) ms)))))
                   :plans/add
                   (let [[m new-ch] params
                         new-p (new-plan (:plans @m))]
                     (om/transact! m
                                   :plans
                                   (fn [ps]
                                     (conj ps new-p)))
                     (put! new-ch new-p))
                   :plans/delete
                   (let [[m p] params]
                     (om/transact! m
                                   :plans
                                   (fn [ps]
                                     (vec (remove #(= p %) ps)))))))
               (recur)))
           om/IRender
           (render [this]
             (om/build (views/for-page (get-in app [:router :page])) app))))
       app-state
       {:target (. js/document (getElementById "app"))
        :shared {:mortgage-control-ch mortgage-control-ch}})))
  )
