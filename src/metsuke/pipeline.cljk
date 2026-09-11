(ns metsuke.pipeline
  "Wires score.cljc -> llm.cljc -> policy.cljc -> ledger.cljc into the single
  path this actor writes through. There is exactly one entry point
  (`process-facts`) and it is the ONLY place that decides what gets appended
  to the ledger — no other function in this codebase calls
  `metsuke.methods.ledger/append`.

  For every scored company-fiscal-year row (flagged or not):
    1. draft a :score/register proposal (llm.cljc) and govern it
       (policy.cljc) — this ALWAYS happens, so even a clean row leaves an
       auditable trail (G7).
    2. if (and only if) `:flagged?`, ALSO draft a :narrative/hold proposal
       and govern it separately — its disposition can only ever be :hold
       (policy/disposition forces this unconditionally for a flagged
       subject; see policy.cljc's docstring), which is the concrete,
       checkable form of G6's 'no code path auto-releases a flagged row'."
  (:require [metsuke.methods.score :as score]
            [metsuke.llm :as llm]
            [metsuke.methods.policy :as policy]
            [metsuke.methods.ledger :as ledger]))

(defn- governed-entry [proposal]
  (let [{:keys [verdict disposition]} (policy/govern proposal)]
    (assoc proposal :verdict verdict :disposition disposition)))

(defn process-row
  "ledger, scored-row -> new ledger (with 1 or 2 new entries appended)."
  [ledger row]
  (let [ledger (ledger/append ledger (governed-entry (llm/draft-score-proposal row)))]
    (if (:flagged? row)
      (ledger/append ledger (governed-entry (llm/draft-narrative-proposal row)))
      ledger)))

(defn process-rows [scored-rows]
  (reduce process-row [] scored-rows))

(defn process-facts
  "Top-level entry point: kanjo `facts` + `filings` -> {:scored-rows [..]
  :ledger [..]}. `ledger` is the full append-only audit trail
  (`metsuke.methods.ledger/validate-chain` can verify it end to end)."
  [facts filings]
  (let [scored (score/score-facts facts filings)]
    {:scored-rows scored :ledger (process-rows scored)}))
