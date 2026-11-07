(ns course.data
  "Synthetic datasets for the course.

  Every value is produced by a formula we wrote plus random noise, so we always
  know the 'true' answer an analysis should recover. All generators take a seed:
  the same seed always produces exactly the same data (reproducibility)."
  (:require [tablecloth.api :as tc])  )

(def neighborhoods
  "Neighborhood name -> nightly price premium in USD over the cheapest area
  (Kicukiro). These are the 'true' effects regression should recover in Module 10."
  {"Nyarutarama" 30
   "Kimihurura" 22
   "Kacyiru" 15
   "Remera" 8
   "Kicukiro" 0})


(def neighborhoods
  "Neighborhood name -> nightly price premium in USD over the cheapest area
  (Kicukiro). These are the 'true' effects regression should recover in Module 10."
  {"Nyarutarama" 30 "Kimihurura" 22 "Kacyiru" 15 "Remera" 8 "Kicukiro" 0})

(defn- gaussian
  "Generate Gaussian (normal) distribution random numbers  using Java's built-in 
   java.util.RandomOne mean `mu` and standard deviation `sd`.

  java.util.Random's .nextGaussian returns a standard normal value (mean 0, sd 1);
  scaling by sd and shifting by mu gives Normal(mu, sd). The ^java.util.Random
  type hint avoids slow reflection on every call."
  [^java.util.Random rng mu sd]
  (+ mu (* sd (.nextGaussian rng))))



(defn- round-to
  "Round `x` to `places` decimal places, e.g. (round-to 3.14159 2) => 3.14.

  Math/round only rounds to whole numbers, so we scale up, round, and scale back."
  [x places]
  (let [f (Math/pow 10 places)]          ; 10^places, e.g. 100.0 for 2 places
    (/ (Math/round (* (double x) f)) f)))

(defn rentals
  "Synthetic short-term rental listings as a tablecloth dataset.

  Arities:
    (rentals)          500 listings with seed 42 (the course default)
    (rentals n seed)   n listings from the given random seed

  Columns and how they are generated (the 'truth' you will try to rediscover):
    :neighborhood  one of 5 areas, chosen uniformly at random
    :bedrooms      1-4, uniform
    :sqm           ~ Normal(30 + 22 * bedrooms, 12), at least 25 m2
    :dist-km       distance to center ~ Normal(5, 2.5), at least 0.5 km
    :wifi          true for about 80% of listings
    :nightly-usd   20 + 10*bedrooms + 0.25*sqm - 2.5*dist-km
                   + neighborhood premium + 6 if wifi + Normal(0, 8) noise
    :rating        ~ Normal(4.3, 0.4) clipped to [2.5, 5.0]; ~5% missing (nil)
    :occupancy     share of nights booked, from a logistic curve: falls with
                   price, rises with rating and wifi
    :booked-90d    booked nights in the last 90 days = 90 * occupancy"
  ([] (rentals 500 42))
  ([n seed]
   (let [rng   (java.util.Random. seed) ; one seeded generator => reproducible data
         hoods (vec (keys neighborhoods))] ; vector so we can pick by random index
     (tc/dataset                 ; a seq of row maps becomes a dataset
      (for [i (range n)]
        (let [ ;; --- features -------------------------------------------------
              hood     (hoods (.nextInt rng (count hoods))) ; random area
              bedrooms (inc (.nextInt rng 4)) ; .nextInt 4 gives 0-3, so 1-4
              sqm      (max 25.0 (gaussian rng (+ 30 (* 22 bedrooms)) 12)) ; more rooms, more space
              dist-km  (max 0.5 (gaussian rng 5 2.5)) ; never closer than 0.5 km
              wifi?    (< (.nextDouble rng) 0.8) ; uniform [0,1) < 0.8 => 80% true
              ;; --- target: price = known linear formula + noise --------------
              price    (max 15.0 (+ 20 (* 10 bedrooms) (* 0.25 sqm) (* -2.5 dist-km)
                                    (neighborhoods hood) (if wifi? 6 0)
                                    (gaussian rng 0 8))) ; noise we can't explain
              rating   (min 5.0 (max 2.5 (gaussian rng 4.3 0.4))) ; clip to a valid star range
              ;; --- occupancy via the logistic function ------------------------
              ;; z is a 'score': higher price lowers it, better rating and wifi raise it.
              z        (+ 0.5 (* -0.03 (- price 70)) (* 1.2 (- rating 4.0)) (if wifi? 0.6 0))
              occ      (/ 1.0 (+ 1.0 (Math/exp (- z))))] ; squash z into (0, 1)
          {:id           i
           :neighborhood hood
           :bedrooms     bedrooms
           :sqm          (Math/round (double sqm)) ; whole square metres
           :dist-km      (round-to dist-km 1)
           :wifi         wifi?
           ;; ~5% of ratings are missing, as in real exports (e.g. new listings)
           :rating       (when (> (.nextDouble rng) 0.05) (round-to rating 1))
           :nightly-usd  (Math/round (double price)) ; whole dollars
           :occupancy    (round-to occ 2)
           :booked-90d   (Math/round (* 90.0 occ))}))
      {:dataset-name "rentals"}))))


(defn monthly-nights
  "Synthetic monthly booked nights for one property, as a dataset.

  Arities:
    (monthly-nights)               48 months (2022-2025), seed 7
    (monthly-nights months seed)

  Each month t (0, 1, 2, ...) is the sum of three known components:
    trend     14 + 0.12 * t                      slow growth, +1.44 nights/year
    season    5 * sin(2 * pi * month-index / 12) a yearly cycle of +/- 5 nights
    noise     Normal(0, 1.5)                     month-to-month randomness
  Module 15 decomposes the series back into these parts.

  Columns: :t (month number), :year, :month (1-12), :nights (rounded count)."
  ([] (monthly-nights 48 7))
  ([months seed]
   (let [rng (java.util.Random. seed)]
     (tc/dataset
      (for [t (range months)]
        {:t      t
         :year   (+ 2022 (quot t 12))                ; integer division: 12 months per year
         :month  (inc (mod t 12))                    ; mod gives 0-11; calendar months are 1-12
         :nights (Math/round (+ 14.0 (* 0.12 t)                                    ; trend
                                (* 5.0 (Math/sin (/ (* 2 Math/PI (mod t 12)) 12)))  ; season
                                (gaussian rng 0 1.5)))})))))                        ; noise
