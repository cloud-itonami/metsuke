(ns metsuke.report-test
  (:require [clojure.test :refer [deftest testing is]]
            [kotoba.lang.text :as str]
            [metsuke.methods.report :as report]
            [metsuke.lexicon :as lexicon]))

(deftest round2-test
  (testing "rounds to 2 decimal places, both directions"
    (is (= 6.46 (report/round2 6.457526627940011)))
    (is (= -1.23 (report/round2 -1.234999)))
    (is (= 0.0 (report/round2 0.0)))))

(deftest flagged-entry-test
  (testing "extracts + rounds the 3 stable diff/report fields, drops everything else"
    (is (= {:company "org.corp.us.tesla" :fiscal-year 2013 :composite-z 41.14}
           (report/flagged-entry {:company "org.corp.us.tesla" :fiscal-year 2013
                                   :composite-z 41.140908572429446
                                   :axis-scores {:revenue-yoy {:z 1.0}}
                                   :citations #{"fact.x"}})))))

(deftest scored-rows->flagged-entries-test
  (testing "only flagged rows survive, sorted composite-z descending"
    (let [scored [{:company "a" :fiscal-year 2020 :composite-z 3.0 :flagged? false}
                  {:company "b" :fiscal-year 2020 :composite-z 9.5 :flagged? true}
                  {:company "c" :fiscal-year 2021 :composite-z 12.25 :flagged? true}]
          entries (report/scored-rows->flagged-entries scored)]
      (is (= 2 (count entries)))
      (is (= ["c" "b"] (mapv :company entries)) "12.25 before 9.5")))
  (testing "stable tie-break: same composite-z sorts by company then fiscal-year"
    (let [scored [{:company "z" :fiscal-year 2020 :composite-z 10.0 :flagged? true}
                  {:company "a" :fiscal-year 2019 :composite-z 10.0 :flagged? true}
                  {:company "a" :fiscal-year 2020 :composite-z 10.0 :flagged? true}]
          entries (report/scored-rows->flagged-entries scored)]
      (is (= [["a" 2019] ["a" 2020] ["z" 2020]]
             (mapv (juxt :company :fiscal-year) entries))))))

(deftest flagged-sets-equal-test
  (testing "identical entry sets (any order) are equal"
    (is (report/flagged-sets-equal?
         [{:company "a" :fiscal-year 2020 :composite-z 6.46}
          {:company "b" :fiscal-year 2021 :composite-z 9.0}]
         [{:company "b" :fiscal-year 2021 :composite-z 9.0}
          {:company "a" :fiscal-year 2020 :composite-z 6.46}])))
  (testing "a composite-z move (even a small one, post-rounding) is NOT equal"
    (is (not (report/flagged-sets-equal?
              [{:company "a" :fiscal-year 2020 :composite-z 6.46}]
              [{:company "a" :fiscal-year 2020 :composite-z 6.47}]))))
  (testing "a new or dropped row is NOT equal"
    (is (not (report/flagged-sets-equal?
              [{:company "a" :fiscal-year 2020 :composite-z 6.46}]
              [{:company "a" :fiscal-year 2020 :composite-z 6.46}
               {:company "b" :fiscal-year 2020 :composite-z 7.0}]))))
  (testing "nil prev (first-ever scan) is never equal to a non-empty curr"
    (is (not (report/flagged-sets-equal? nil [{:company "a" :fiscal-year 2020 :composite-z 6.46}]))))
  (testing "nil prev vs an empty curr IS still treated as changed (no baseline to compare against)"
    (is (not (report/flagged-sets-equal? nil [])))))

(deftest diff-flagged-test
  (let [prev [{:company "a" :fiscal-year 2020 :composite-z 6.46}
              {:company "b" :fiscal-year 2019 :composite-z 7.0}]
        curr [{:company "a" :fiscal-year 2020 :composite-z 8.0}   ; changed
              {:company "c" :fiscal-year 2021 :composite-z 9.5}]  ; new; b dropped
        diff (report/diff-flagged prev curr)]
    (testing "new = flagged now, absent from prev"
      (is (= [{:company "c" :fiscal-year 2021 :composite-z 9.5}] (:new diff))))
    (testing "dropped = flagged in prev, absent from curr"
      (is (= [{:company "b" :fiscal-year 2019 :composite-z 7.0}] (:dropped diff))))
    (testing "changed = same company+fiscal-year in both, composite-z moved"
      (is (= [{:company "a" :fiscal-year 2020 :from 6.46 :to 8.0}] (:changed diff)))))
  (testing "nil prev -> everything currently flagged reports as :new, nothing :dropped/:changed"
    (let [diff (report/diff-flagged nil [{:company "a" :fiscal-year 2020 :composite-z 6.46}])]
      (is (= [{:company "a" :fiscal-year 2020 :composite-z 6.46}] (:new diff)))
      (is (empty? (:dropped diff)))
      (is (empty? (:changed diff))))))

(deftest disclaimer-lexicon-compliance-test
  (testing "the report's own boilerplate disclaimer passes the SAME G5 hedge check the narrative layer does"
    (is (:ok? (lexicon/check-text report/disclaimer)))))

(deftest render-markdown-test
  (let [flagged [{:company "org.corp.us.tesla" :fiscal-year 2013 :composite-z 41.14}]
        base {:scan-date "2026-07-21" :kanjo-sha "abc123"
              :kanjo-commit-url "https://github.com/etzhayyim/com-etzhayyim-kanjo/commit/abc123"
              :row-count 466 :flagged-count 1 :low-confidence-count 0
              :ledger-count 502 :held-count 72 :recorded-count 430
              :flagged-entries flagged}]
    (testing "first-ever report (no prev-entries) never renders a diff section, only the no-baseline note"
      (let [md (report/render-markdown base)]
        (is (str/includes? md "2026-07-21"))
        (is (str/includes? md "abc123"))
        (is (str/includes? md "org.corp.us.tesla"))
        (is (str/includes? md "41.14"))
        (is (str/includes? md "no prior committed report"))
        (is (not (str/includes? md "Newly flagged")))))
    (testing "with prev-entries + diff, renders the changes section"
      (let [diff (report/diff-flagged [] flagged)
            md (report/render-markdown (assoc base :prev-entries [] :diff diff))]
        (is (str/includes? md "Newly flagged"))
        (is (str/includes? md "org.corp.us.tesla"))))
    (testing "renders the hedged disclaimer verbatim, never a bare verdict"
      (is (str/includes? (report/render-markdown base) "does NOT establish")))))

(deftest render-edn-data-test
  (testing "produces a plain map suitable as next run's `prev` (round-trips through flagged-sets-equal?)"
    (let [flagged [{:company "a" :fiscal-year 2020 :composite-z 6.46}]
          data (report/render-edn-data {:scan-date "2026-07-21" :kanjo-sha "abc123"
                                         :row-count 10 :flagged-count 1 :low-confidence-count 0
                                         :ledger-count 12 :held-count 1 :recorded-count 11
                                         :flagged-entries flagged})]
      (is (= "2026-07-21" (:scan/date data)))
      (is (= "abc123" (:scan/kanjo-sha data)))
      (is (= flagged (:scan/flagged data)))
      (is (report/flagged-sets-equal? (:scan/flagged data) flagged)))))
