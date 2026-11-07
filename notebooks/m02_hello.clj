(ns m02-hello
  "A Clay notebook. Every top-level form is evaluated, and its value is
  rendered in the browser: maps as data, datasets as tables, Kindly-annotated
  values (Markdown, plots) in their special form."
  (:require [course.data :as data]         ;; Synthetic dataaet
            [tablecloth.api :as tc]        ;; datase api
            [scicloj.clay.v2.api :as clay] ;; renders a namespace as an HTML notebook
            [scicloj.kindly.v4.kind :as kind])) ;; annotations

;; kind/md marks a string as Markdown, so Clay renders it as a heading
;; instead of printing a quoted string.
(kind/md "# Hello, data")
;; => ["# Hello, data"]


;; Generate the default course dataset: 500 listings, seed 42.
(def ds (data/rentals))

;; Check to see the collection type
(type ds)
;; => tech.v3.dataset.impl.dataset.Dataset

;; The first 5 rows. Clay recognizes datasets and shows them as a table.
(tc/head ds 5)
;; => rentals [5 10]:
;;    | :nightly-usd | :dist-km | :sqm | :occupancy | :bedrooms | :id | :booked-90d | :wifi | :neighborhood | :rating |
;;    |-------------:|---------:|-----:|-----------:|----------:|----:|------------:|-------|---------------|--------:|
;;    |           79 |      2.1 |   65 |       0.82 |         1 |   0 |          74 |  true |   Nyarutarama |     4.6 |
;;    |           70 |      8.7 |   94 |       0.83 |         3 |   1 |          75 | false |    Kimihurura |     4.9 |
;;    |           46 |      1.7 |   57 |       0.85 |         1 |   2 |          77 | false |       Kacyiru |     4.4 |
;;    |           33 |      8.8 |   58 |       0.96 |         1 |   3 |          86 |  true |        Remera |     4.7 |
;;    |           62 |      4.6 |   74 |       0.74 |         1 |   4 |          66 |  true |        Remera |     3.8 |


;; A `comment` block is never evaluated when the file loads. It holds
(comment
  ;; Render this namespace to HTML and open it in the browser.
  ;; Re-run it after edits to refresh the notebook.
  (clay/make! {:source-path "notebooks/m02_hello.clj"}))
