#!/usr/bin/env nbb
;; metsuke CLI — thin nbb wrapper around metsuke.pipeline/process-facts.
;; Usage: nbb bin/metsuke.cljs <path-to-kanjo-facts.merged.kotoba.edn>
;;
;; Prints a summary (row count, flagged count, low-confidence count) and how
;; many ledger entries would be :hold vs :recorded. Does NOT publish or
;; disclose anything — printing to stdout is not a ledger write and is not a
;; disclosure surface; it is the same "read the ledger back" honesty kanjo's
;; own `analyze.py` -> out/intel-report.md step has (R0 has no publish path
;; at all, see MATURITY.md).
(require '[metsuke.io :as io]
         '[metsuke.pipeline :as pipeline])

(let [path (first *command-line-args*)]
  (if-not path
    (do (println "usage: nbb bin/metsuke.cljs <path-to-kanjo-facts.merged.kotoba.edn>")
        (js/process.exit 1))
    (let [facts (io/read-kanjo-fact-rows path)
          filings (io/read-kanjo-filings path)
          {:keys [scored-rows ledger]} (pipeline/process-facts facts filings)
          flagged (filter :flagged? scored-rows)
          low-conf (filter :low-confidence? scored-rows)
          holds (filter #(= :hold (:disposition %)) ledger)]
      (println "metsuke — scored" (count scored-rows) "company-fiscal-year rows from" path)
      (println "  flagged (composite-z >= threshold):" (count flagged))
      (println "  low-confidence (thin peer AND trailing coverage, G8):" (count low-conf))
      (println "  ledger entries:" (count ledger) "(" (count holds) "held," (- (count ledger) (count holds)) "recorded )")
      (doseq [row (sort-by :composite-z > flagged)]
        (println "  HOLD:" (:company row) (:fiscal-year row) "composite-z=" (:composite-z row))))))
