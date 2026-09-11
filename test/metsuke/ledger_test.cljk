(ns metsuke.ledger-test
  (:require [clojure.test :refer [deftest testing is]]
            [metsuke.methods.ledger :as ledger]))

(deftest append-only-chain-test
  (let [l1 (ledger/append [] {:op :score/register :v 1})
        l2 (ledger/append l1 {:op :score/register :v 2})
        l3 (ledger/append l2 {:op :narrative/hold :v 3})]
    (is (= 3 (count l3)))
    (is (= 0 (:entry/seq (nth l3 0))))
    (is (= 1 (:entry/seq (nth l3 1))))
    (is (= 2 (:entry/seq (nth l3 2))))
    (is (nil? (:entry/prev (nth l3 0))))
    (is (= (:entry/hash (nth l3 0)) (:entry/prev (nth l3 1))))
    (is (= (:entry/hash (nth l3 1)) (:entry/prev (nth l3 2))))
    (is (:ok? (ledger/validate-chain l3)))))

(deftest tamper-detection-test
  (testing "mutating an already-appended entry's content breaks the chain — append-only is checkable, not just promised"
    (let [l (-> [] (ledger/append {:op :score/register :v 1}) (ledger/append {:op :score/register :v 2}))
          tampered (update l 0 assoc :v 999)]
      (is (:ok? (ledger/validate-chain l)))
      (is (not (:ok? (ledger/validate-chain tampered))))
      (is (= 0 (:broken-at (ledger/validate-chain tampered)))))))

(deftest empty-ledger-is-valid-test
  (is (:ok? (ledger/validate-chain []))))
