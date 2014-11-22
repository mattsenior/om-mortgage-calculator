(ns howdy.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.reader :as reader]
            [howdy.views :as views]
            [howdy.router :as router]
            [cljs.core.async :refer [put! chan <!]]
            [cljs-uuid.core :as uuid]
            [om-sync.util :refer [edn-xhr]]))

(def app-state
  (atom {:text "Howdy Mortgages"
         :router {:page :home, :params {}}
         :mortgages []}))

(defn- new-mortgage []
  {:id (str (uuid/make-random))
   :name "New Mortgage"
   :start-balance 150000
   :plans []})

(defn- new-plan
  [plans]
  {:id (str (uuid/make-random))
   :name "New Plan"
   :values []})

(edn-xhr {:method :get
          :url "/mortgages/new"
          :on-complete (fn [res]
                         (.log js/console (pr-str res))
                         (reset! app-state res)
                         )})

(defn main []
  (let [mortgage-control-ch (chan)]
    (om/root
     (fn [app owner]
       (reify
         om/IDisplayName
         (display-name [_] "Root")
         om/IWillMount
         (will-mount [_]
           ;; Handle route changes
           (go (while true
                 (let [{:keys [params page]} (<! router/page-changes-ch)]
                   (om/update! app :router {:page page :params params}))))
           ;; Add/delete mortgages
           (go (while true
                 (let [[action & params] (<! mortgage-control-ch)]
                   (case action
                     :mortgages/add
                     (let [[new-ch] params
                           new-m (new-mortgage)]
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
                                       (vec (remove #(= p %) ps))))))))))
         om/IRender
         (render [this]
           (om/build (views/for-page (get-in app [:router :page])) app))))
     app-state
     {:target (. js/document (getElementById "app"))
      :shared {:mortgage-control-ch mortgage-control-ch}})))
