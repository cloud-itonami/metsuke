(ns metsuke.pipeline-test
  "End-to-end: score.cljc -> llm.cljc -> policy.cljc -> ledger.cljc, over
  the G-Plan historical fixture. This is the ONLY place a narrative
  mentioning 'statistical anomaly' about a real-world-shaped company is
  exercised, and it only ever reaches this actor's own append-only ledger
  as a :hold entry — never any external disclosure surface (there is no
  such surface built in R0)."
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.set :as set]
            [metsuke.pipeline :as pipeline]
            [metsuke.methods.ledger :as ledger]
            [metsuke.fixtures.gplan-fy2025 :as fx]))

(deftest gplan-row-ends-in-hold-test
  (testing "G6 — the flagged G-Plan row's narrative proposal is ALWAYS :hold in the ledger, never :recorded or anything else"
    (let [{:keys [scored-rows ledger]} (pipeline/process-facts fx/all-facts fx/all-filings)
          gplan-entries (filter #(= fx/gplan-company (get-in % [:subject :id])) ledger)
          narrative-entries (filter #(= :narrative/hold (:op %)) gplan-entries)]
      (is (some :flagged? scored-rows))
      (is (seq narrative-entries) "a flagged row must produce a :narrative/hold proposal")
      (doseq [entry narrative-entries]
        (is (= :hold (:disposition entry)))
        (is (:ok? (:verdict entry)) "the narrative itself passes G3/G4/G5 on its own merits")
        (is (seq (:citations entry)) "G3 — the narrative cites the same kanjo facts the score was derived from")))))

(deftest clean-peer-rows-are-recorded-not-held-test
  (testing "non-flagged rows record cleanly — :hold is not the default disposition for everything"
    (let [{:keys [ledger]} (pipeline/process-facts fx/all-facts fx/all-filings)
          peer-entries (filter #(contains? (set fx/peer-companies) (get-in % [:subject :id])) ledger)]
      (is (seq peer-entries))
      (doseq [entry peer-entries]
        (is (= :recorded (:disposition entry)))))))

(deftest ledger-chain-is-valid-test
  (testing "G7 — the full run produces a valid, unbroken append-only chain"
    (let [{:keys [ledger]} (pipeline/process-facts fx/all-facts fx/all-filings)]
      (is (seq ledger))
      (is (:ok? (ledger/validate-chain ledger))))))

(deftest no-commit-or-published-disposition-anywhere-test
  (testing "there is no :commit/:published disposition ANYWHERE in a full pipeline run — :recorded and :hold are the only two values that ever appear"
    (let [{:keys [ledger]} (pipeline/process-facts fx/all-facts fx/all-filings)
          dispositions (into #{} (map :disposition) ledger)]
      (is (seq dispositions))
      (is (= dispositions (set/intersection dispositions #{:recorded :hold}))))))

(deftest thin-coverage-row-is-recorded-not-held-test
  (testing "a :low-confidence?, non-flagged thin-coverage row still records (low confidence is not itself a hold reason — only :flagged? is, per G6)"
    (let [{:keys [scored-rows ledger]} (pipeline/process-facts fx/thin-facts fx/thin-filings)]
      (is (some :low-confidence? scored-rows))
      (is (every? #(not (:flagged? %)) scored-rows))
      (is (every? #(= :recorded (:disposition %)) ledger)))))
