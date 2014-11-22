(ns howdy.server
  (:require [clojure.java.io :as io]
            [howdy.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [compojure.handler :refer [api]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [ring.middleware.reload :as reload]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [cljs-uuid.core :as uuid]
            ))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (GET "/mortgages/new" []
       (generate-response {:text "Howdy Mortgages"
         :router {:page :home, :params {}}
         :mortgages [{:id (str (uuid/make-random))
                      :name "Mortgage A"
                      :start-balance 250000
                      :start-year 2014
                      :start-month 4
                      :plans [{:id (str (uuid/make-random))
                               :name "Suggested"
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
                               :name "Overpayment"
                               :values [{:year 1
                                         :month 1
                                         :interest-rate 2.99
                                         :regular-payment 2400
                                         :one-off-payment 0}
                                        {:year 6
                                         :month 1
                                         :interest-rate 8
                                         :regular-payment 1400}]}]}
                     {:id (str (uuid/make-random))
                      :name "Mortgage B"
                      :start-balance 150000
                      :plans []}]}))
  (GET "/*" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (api #'routes))
    (api routes)))

(defn run [& [port]]
  (defonce ^:private server
    (do
      (if is-dev? (start-figwheel))
      (let [port (Integer. (or port (env :port) 10555))]
        (print "Starting web server on port" port ".\n")
        (run-server http-handler {:port port
                          :join? false}))))
  server)

(defn -main [& [port]]
  (run port))
