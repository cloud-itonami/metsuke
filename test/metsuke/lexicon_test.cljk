(ns metsuke.lexicon-test
  (:require [clojure.test :refer [deftest testing is]]
            [metsuke.lexicon :as lexicon]))

(deftest banned-predicate-jp-test
  (is (not (:ok? (lexicon/check-text "この会社は不正を行った。"))))
  (is (= :banned-predicate (:rule (first (:violations (lexicon/check-text "この会社は不正を行った。")))))))

(deftest banned-predicate-en-test
  (is (not (:ok? (lexicon/check-text "This company committed fraud."))))
  (is (not (:ok? (lexicon/check-text "The executives are guilty."))))
  (is (not (:ok? (lexicon/check-text "The company broke the law.")))))

(deftest missing-hedge-marker-test
  (testing "text with no banned phrase but also no hedge marker is still rejected"
    (let [result (lexicon/check-text "Revenue grew significantly this year.")]
      (is (not (:ok? result)))
      (is (= :missing-hedge-marker (:rule (first (:violations result))))))))

(deftest properly-hedged-text-passes-test
  (is (:ok? (lexicon/check-text "統計的異常のパターンが見られ、重点調査対象の候補となる。反証可能な仮説として記録する。")))
  (is (:ok? (lexicon/check-text "This is a statistical anomaly and a candidate for closer review; not established from public filings alone."))))

(deftest empty-text-is-ok-test
  (testing "an empty/blank narrative (e.g. a non-flagged row's proposal that carries no :text at all) is not itself a violation"
    (is (:ok? (lexicon/check-text "")))))
