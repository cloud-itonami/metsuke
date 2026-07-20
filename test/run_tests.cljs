#!/usr/bin/env nbb
;; nbb test/run_tests.cljs — the canonical way to run this repo's test
;; suite (per root CLAUDE.md: nbb is the script host, not bb/JVM). Requires
;; every test namespace and runs them with clojure.test (nbb ships
;; clojure.test support directly, no cljs.test alias needed — same pattern
;; as 70-tools/bmc/run-tests.cljs).
(require '[clojure.test :as t]
         'metsuke.facts-test
         'metsuke.lexicon-test
         'metsuke.score-test
         'metsuke.policy-test
         'metsuke.ledger-test
         'metsuke.pipeline-test
         'metsuke.report-test)

(let [{:keys [fail error]}
      (t/run-tests 'metsuke.facts-test
                    'metsuke.lexicon-test
                    'metsuke.score-test
                    'metsuke.policy-test
                    'metsuke.ledger-test
                    'metsuke.pipeline-test
                    'metsuke.report-test)]
  (js/process.exit (if (pos? (+ fail error)) 1 0)))
