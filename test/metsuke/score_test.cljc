(ns metsuke.score-test
  (:require [clojure.test :refer [deftest testing is]]
            [metsuke.methods.score :as score]
            [metsuke.fixtures.gplan-fy2025 :as fx]))

(deftest gplan-fixture-flags-test
  (testing "the public G-Plan FY2025/3 historical fixture places it in the flagged tier"
    (let [scored (score/score-facts fx/all-facts fx/all-filings)
          gplan-row (first (filter #(and (= (:company %) fx/gplan-company) (= (:fiscal-year %) 2025)) scored))]
      (is (some? gplan-row) "gplan FY2025 row should be scorable (both FY2024 and FY2025 present)")
      (is (:flagged? gplan-row) "composite-z should cross FLAG-THRESHOLD given the simultaneous quadruple outlier")
      (is (>= (:composite-z gplan-row) score/FLAG-THRESHOLD))
      (is (not (:low-confidence? gplan-row)) "5 synthetic peers meet MIN-PEER-N, so this should NOT be low-confidence")
      (is (= 6 (count (:citations gplan-row))) "revenue+operating-income+total-assets x2 years = 6 citations (G3 traceability)"))))

(deftest peers-not-flagged-test
  (testing "mild synthetic peers (~5% growth, ~flat margin) do not flag against each other"
    (let [scored (score/score-facts fx/all-facts fx/all-filings)
          peer-rows (filter #(contains? (set fx/peer-companies) (:company %)) scored)]
      (is (= 5 (count peer-rows)))
      (doseq [row peer-rows]
        (is (not (:flagged? row)) (str (:company row) " should not flag: composite-z=" (:composite-z row)))))))

(deftest thin-coverage-fallback-test
  (testing "G8 — a company alone in its fiscal year with < MIN-TRAILING-N own history falls back to :unscored, never a fabricated distribution"
    (let [scored (score/score-facts fx/thin-facts fx/thin-filings)
          row (first scored)]
      (is (some? row))
      (is (:low-confidence? row))
      (is (= (set score/axis-keys) (set (:unscored-axes row))) "every axis is unscored — no peers, no trailing history")
      (is (zero? (:composite-z row)) "unscored axes contribute z=0, not a fabricated number")
      (is (not (:flagged? row)) "zero composite never crosses FLAG-THRESHOLD"))))

(deftest trailing-history-preferred-test
  (testing "a company with >= MIN-TRAILING-N own prior-year observations is scored against its OWN trailing history"
    (let [scored (score/score-facts fx/trailing-facts fx/trailing-filings)
          row (first (filter #(= 2024 (:fiscal-year %)) scored))]
      (is (some? row))
      (is (every? #(= :trailing (:source (get (:axis-scores row) %))) score/axis-keys)
          "all 4 axes should use the :trailing source given 3 prior own-history observations")
      (is (not (:low-confidence? row)))
      (is (not (:flagged? row)) "steady ~5%/year growth against its own steady history should not flag"))))

(deftest citations-nonempty-for-every-scored-row-test
  (testing "G3 — every scored row (not just flagged ones) carries non-empty citations"
    (let [scored (score/score-facts fx/all-facts fx/all-filings)]
      (is (seq scored))
      (doseq [row scored]
        (is (seq (:citations row)) (str (:company row) " " (:fiscal-year row) " has no citations"))))))
