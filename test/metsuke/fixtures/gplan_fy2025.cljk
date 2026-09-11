(ns metsuke.fixtures.gplan-fy2025
  "TEST FIXTURE ONLY — NOT A LIVE CLAIM ABOUT ANY REAL PERSON OR COMPANY'S
  WRONGDOING.

  `gplan-*` below reproduces G-Plan's own PUBLIC, ALREADY-DISCLOSED FY2025/3
  headline figures (the same public numbers cited in the 2026
  KDDI/BIGLOBE/G-Plan circular-transaction accounting-fraud reporting that
  motivated this actor's design): revenue +38.5% YoY, operating income
  +175.7% YoY, total assets +204% YoY, operating margin ~5.2% -> ~10.4%.
  Prior-year (FY2024/3) figures are BACK-COMPUTED from these public
  percentages (kanjo's own :fin.fact/sourcing :representative discipline —
  headline, rounded, not a literal XBRL line-item pull) purely so this
  fixture is internally consistent; this file asserts nothing about WHY
  those percentages moved, and this actor's own G2/G5 gates forbid ever
  asserting that in a narrative sentence.

  `peer-*` companies are entirely SYNTHETIC (no real company, real id, or
  real filing behind them) — 5 to 10 point growth, ~flat operating margin —
  used ONLY to give score.cljc a peer distribution large enough to exercise
  the market-wide peer z-score branch (G8's MIN-PEER-N)."
  )

;; ───────────────────────── G-Plan (real public headline figures) ─────────────────────────

(def gplan-company "org.corp.jp.gplan-test-fixture")

(def gplan-filings
  [{:fin.filing/id "fil.jp.edinet.gplan.2024" :fin.filing/company gplan-company :fin.filing/fiscal-year 2024}
   {:fin.filing/id "fil.jp.edinet.gplan.2025" :fin.filing/company gplan-company :fin.filing/fiscal-year 2025}])

(def gplan-facts
  [;; FY2024/3 (back-computed prior year, :representative)
   {:fin.fact/id "fact.gplan.2024.revenue" :fin.fact/filing "fil.jp.edinet.gplan.2024" :fin.fact/company gplan-company
    :fin.fact/concept :revenue :fin.fact/value 59477 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.gplan.2024.operating-income" :fin.fact/filing "fil.jp.edinet.gplan.2024" :fin.fact/company gplan-company
    :fin.fact/concept :operating-income :fin.fact/value 3093 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.gplan.2024.total-assets" :fin.fact/filing "fil.jp.edinet.gplan.2024" :fin.fact/company gplan-company
    :fin.fact/concept :total-assets :fin.fact/value 35460 :fin.fact/sourcing :representative}
   ;; FY2025/3 (real public headline figures — revenue +38.5%, operating income
   ;; +175.7%, total assets +204% YoY, per the public decachi disclosure)
   {:fin.fact/id "fact.gplan.2025.revenue" :fin.fact/filing "fil.jp.edinet.gplan.2025" :fin.fact/company gplan-company
    :fin.fact/concept :revenue :fin.fact/value 82377 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.gplan.2025.operating-income" :fin.fact/filing "fil.jp.edinet.gplan.2025" :fin.fact/company gplan-company
    :fin.fact/concept :operating-income :fin.fact/value 8526 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.gplan.2025.total-assets" :fin.fact/filing "fil.jp.edinet.gplan.2025" :fin.fact/company gplan-company
    :fin.fact/concept :total-assets :fin.fact/value 107795 :fin.fact/sourcing :representative}])

;; ───────────────────────── synthetic, non-anomalous peers ─────────────────────────

(def peer-specs
  "[company-id [prior-revenue prior-opinc prior-assets] [curr-revenue curr-opinc curr-assets]] —
  hand-picked to be mild (~4-6% growth, ~flat margin), the opposite shape of
  gplan's simultaneous quadruple outlier above."
  [["org.corp.synthetic.peer-a" [50000 4000 60000] [52500 4200 62400]]
   ["org.corp.synthetic.peer-b" [30000 2500 40000] [31800 2600 41200]]
   ["org.corp.synthetic.peer-c" [80000 6000 90000] [84000 6300 93600]]
   ["org.corp.synthetic.peer-d" [45000 3600 55000] [47700 3816 57200]]
   ["org.corp.synthetic.peer-e" [62000 5000 70000] [65100 5250 72800]]])

(defn- peer-filings [company]
  [{:fin.filing/id (str "fil." company ".2024") :fin.filing/company company :fin.filing/fiscal-year 2024}
   {:fin.filing/id (str "fil." company ".2025") :fin.filing/company company :fin.filing/fiscal-year 2025}])

(defn- peer-facts [company [pr po pa] [cr co ca]]
  [{:fin.fact/id (str "fact." company ".2024.revenue") :fin.fact/filing (str "fil." company ".2024") :fin.fact/company company
    :fin.fact/concept :revenue :fin.fact/value pr :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact." company ".2024.operating-income") :fin.fact/filing (str "fil." company ".2024") :fin.fact/company company
    :fin.fact/concept :operating-income :fin.fact/value po :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact." company ".2024.total-assets") :fin.fact/filing (str "fil." company ".2024") :fin.fact/company company
    :fin.fact/concept :total-assets :fin.fact/value pa :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact." company ".2025.revenue") :fin.fact/filing (str "fil." company ".2025") :fin.fact/company company
    :fin.fact/concept :revenue :fin.fact/value cr :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact." company ".2025.operating-income") :fin.fact/filing (str "fil." company ".2025") :fin.fact/company company
    :fin.fact/concept :operating-income :fin.fact/value co :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact." company ".2025.total-assets") :fin.fact/filing (str "fil." company ".2025") :fin.fact/company company
    :fin.fact/concept :total-assets :fin.fact/value ca :fin.fact/sourcing :representative}])

(def peer-companies (mapv first peer-specs))
(def peer-filings-all (vec (mapcat (fn [[c]] (peer-filings c)) peer-specs)))
(def peer-facts-all (vec (mapcat (fn [[c prior curr]] (peer-facts c prior curr)) peer-specs)))

(def all-filings (into gplan-filings peer-filings-all))
(def all-facts (into gplan-facts peer-facts-all))

;; ───────────────────────── thin-coverage fixture (G8) ─────────────────────────
;; A single company, single fiscal-year pair, in ITS OWN fiscal year with NO
;; peers at all and < MIN-TRAILING-N prior observations — exercises the
;; :unscored / :low-confidence? fallback (never a fabricated distribution).

(def thin-company "org.corp.synthetic.thin-co")
(def thin-filings
  [{:fin.filing/id "fil.thin.2029" :fin.filing/company thin-company :fin.filing/fiscal-year 2029}
   {:fin.filing/id "fil.thin.2030" :fin.filing/company thin-company :fin.filing/fiscal-year 2030}])
(def thin-facts
  [{:fin.fact/id "fact.thin.2029.revenue" :fin.fact/filing "fil.thin.2029" :fin.fact/company thin-company
    :fin.fact/concept :revenue :fin.fact/value 10000 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.thin.2029.operating-income" :fin.fact/filing "fil.thin.2029" :fin.fact/company thin-company
    :fin.fact/concept :operating-income :fin.fact/value 800 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.thin.2029.total-assets" :fin.fact/filing "fil.thin.2029" :fin.fact/company thin-company
    :fin.fact/concept :total-assets :fin.fact/value 12000 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.thin.2030.revenue" :fin.fact/filing "fil.thin.2030" :fin.fact/company thin-company
    :fin.fact/concept :revenue :fin.fact/value 10500 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.thin.2030.operating-income" :fin.fact/filing "fil.thin.2030" :fin.fact/company thin-company
    :fin.fact/concept :operating-income :fin.fact/value 830 :fin.fact/sourcing :representative}
   {:fin.fact/id "fact.thin.2030.total-assets" :fin.fact/filing "fil.thin.2030" :fin.fact/company thin-company
    :fin.fact/concept :total-assets :fin.fact/value 12300 :fin.fact/sourcing :representative}])

;; ───────────────────────── trailing-history fixture ─────────────────────────
;; A company with 4 fiscal years of its OWN history (>= MIN-TRAILING-N = 3
;; prior YoY observations by the 4th year) — exercises the :trailing branch,
;; which the fixture picks EVEN THOUGH it also has peers, so the test can
;; assert trailing is preferred over peer when both are available.

(def trailing-company "org.corp.synthetic.trailing-co")
(defn- trailing-year [fy revenue opinc assets]
  [{:fin.filing/id (str "fil.trailing." fy) :fin.filing/company trailing-company :fin.filing/fiscal-year fy}
   {:fin.fact/id (str "fact.trailing." fy ".revenue") :fin.fact/filing (str "fil.trailing." fy) :fin.fact/company trailing-company
    :fin.fact/concept :revenue :fin.fact/value revenue :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact.trailing." fy ".operating-income") :fin.fact/filing (str "fil.trailing." fy) :fin.fact/company trailing-company
    :fin.fact/concept :operating-income :fin.fact/value opinc :fin.fact/sourcing :representative}
   {:fin.fact/id (str "fact.trailing." fy ".total-assets") :fin.fact/filing (str "fil.trailing." fy) :fin.fact/company trailing-company
    :fin.fact/concept :total-assets :fin.fact/value assets :fin.fact/sourcing :representative}])

;; FY2020..2024 each ~5% growth on all 3 axes -> delta (YoY) rows exist for
;; FY2021/2022/2023/2024; scoring FY2024 then has 3 prior trailing
;; observations (FY2021, FY2022, FY2023), meeting MIN-TRAILING-N = 3.
(def trailing-entries
  (vec (mapcat (fn [[fy r o a]] (trailing-year fy r o a))
               [[2020 38095 3048 45714]
                [2021 40000 3200 48000]
                [2022 42000 3360 50400]
                [2023 44100 3528 52920]
                [2024 46305 3704 55566]])))
(def trailing-filings (filterv #(contains? % :fin.filing/id) trailing-entries))
(def trailing-facts (filterv #(contains? % :fin.fact/id) trailing-entries))
