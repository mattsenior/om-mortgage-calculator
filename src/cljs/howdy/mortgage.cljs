(ns howdy.mortgage)

(defn- calculate-total-months
  "Get total month count for given number of years and months
  E.g. Year 1 month 1 = 1; Year 1 month 2 = 2; Year 2 month 1 = 13"
  [{:keys [year month]}]
  (+ month (* 12 (dec year))))

(defn- assoc-total-months
  "Calculate and assoc :total-month key in each map in the coll"
  [coll]
  (map #(assoc % :total-month (calculate-total-months %)) coll))

(defn- assoc-cascaded-values
  [coll]
  (reductions
   (fn [prev this] (merge (select-keys prev [:interest-rate :regular-payment]) this))
   coll))

(defn- expand-single-value-map
  "Repeat the given map n times (or inf), and dissoc :one-off-payment
  from all but the first one. Also dissoc some unnecessary keys."
  ([x] (cons x (repeat (dissoc x
                               :one-off-payment
                               :year
                               :month
                               :total-month))))
  ([x n] (take n (expand-single-value-map x))))

(defn- expand-months
  "Repeat each value map so there is one per month"
  [coll]
  (concat
   (mapcat
    (fn [this next]
      (expand-single-value-map this (- (:total-month next) (:total-month this))))
    coll
    (rest coll))
   (expand-single-value-map (last coll))))

(defn- assoc-month-numbers
  [coll]
  (map (fn [month-num x]
         {:month-number month-num :values x})
       (drop 1 (range))
       coll))

(defn- apply-interest-and-repayments
  [start-debt month-values]
  (let [init {:total-debt             start-debt
              :total-paid             0
              :total-interest-charged 0
              :total-debt-repaid      0}
        totals (reductions
                (fn [prev this]
                  (let [prev-total-debt        (:total-debt prev)
                        ;; Get the interest rate for this month as a percentage, e.g. 2.99
                        interest-rate          (get-in this [:values :interest-rate])
                        ;; Calculate the interest charged - NB months are even 12ths of a year
                        month-interest-charged (* prev-total-debt (/ interest-rate 100 12))
                        ;; Add up regular + one-off repayments this month
                        month-paid             (+ (get-in this [:values :regular-payment] 0)
                                                  (get-in this [:values :one-off-payment] 0))
                        ;; Prevent overpayment by reducing month-paid
                        month-paid             (min (+ prev-total-debt month-interest-charged) month-paid)
                        ;; Calculate how much debt was repaid
                        month-debt-repaid      (- month-paid month-interest-charged)]
                    (merge this
                           {:prev-total-debt        prev-total-debt
                            :month-interest-charged month-interest-charged
                            :month-paid             month-paid
                            :month-debt-repaid      month-debt-repaid
                            :total-debt             (- prev-total-debt month-debt-repaid)
                            :total-paid             (+ (:total-paid prev) month-paid)
                            :total-interest-charged (+ (:total-interest-charged prev) month-interest-charged)
                            :total-debt-repaid      (+ (:total-debt-repaid prev) month-debt-repaid)})))
                init
                month-values)
        totals (->> totals
                    (drop 1)
                    (take 2400)
                    (take-while #(pos? (:prev-total-debt %))))]
    totals))

(defn get-mortgage-lifespan
  [m p]
  (->> (:values p)
       assoc-total-months
       assoc-cascaded-values
       expand-months
       assoc-month-numbers
       (apply-interest-and-repayments (:start-balance m))))
