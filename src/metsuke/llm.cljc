(ns metsuke.llm
  "Metsuke-LLM — the sealed advisor. It ONLY ever returns *proposals*
  (metsuke.methods.policy/check decides what happens to them); it never
  writes to the ledger itself. R0 has no real model call wired at all (same
  honesty as kanjo's R0/dossier's R0 — no fabricated 'it works' claim): the
  narrative is generated deterministically from a fixed template built ONLY
  out of metsuke.lexicon's allow-listed hedge markers, so by construction it
  can never emit a banned declarative-fraud predicate. The G5 lexicon check
  in policy.cljc still runs on its output — defense in depth, not 'trust the
  generator' — and the adversarial tests in test/metsuke construct
  hand-written violating proposals to prove the governor (not the
  generator's good behavior) is what actually enforces G5.

  Wiring a real generative model here (e.g. via murakumo-main, per root
  CLAUDE.md's LLM alias convention) is an explicit R1+ follow-up
  (MATURITY.md) — the template below is deliberately conservative rather
  than fluent."
  (:require [metsuke.lexicon :as lexicon]))

(defn- cite [fact-id] {:id fact-id :class :kanjo/fin-fact})

(defn draft-score-proposal
  "A :score/register proposal for ANY scored row (flagged or not) — this is
  what gets recorded to the ledger regardless of the narrative layer."
  [{:keys [company fiscal-year citations composite-z flagged? low-confidence? unscored-axes] :as row}]
  {:op :score/register
   :subject {:kind :company :id company :fiscal-year fiscal-year}
   :citations (mapv cite citations)
   :composite-z composite-z
   :flagged? flagged?
   :low-confidence? low-confidence?
   :unscored-axes unscored-axes
   :confidence (if low-confidence? 0.4 0.9)})

(defn- pct
  "Portable percent formatting (no JS interop / no format string — Math/round
  works on both the JVM and cljs since `Math/*` reads as `js/Math.*` under
  cljs)."
  [x]
  (let [rounded (/ (Math/round (* x 1000.0)) 10.0)]
    (str (if (neg? rounded) "" "+") rounded "%")))

(defn- axis-summary [{:keys [revenue-yoy operating-income-yoy total-assets-yoy margin-delta]}]
  (str "revenue " (pct revenue-yoy) " / operating-income " (pct operating-income-yoy)
       " / total-assets " (pct total-assets-yoy) " / operating-margin delta "
       (pct margin-delta)))

(defn draft-narrative-proposal
  "Only meaningful for `:flagged? true` rows — MetsukeGovernor forces
  :hold on these regardless (G6), but the narrative still has to pass G3/G5
  on its own merits (this is what the adversarial tests exercise). Text is
  bilingual (JP + EN), built ONLY from lexicon/hedge-markers phrasing, and
  ALWAYS states explicitly that public filings alone do not establish
  wrongdoing (G2)."
  [{:keys [company fiscal-year citations composite-z] :as row}]
  (let [summary (axis-summary row)
        text (str "統計的異常: " company " の会計年度 " fiscal-year
                   " は、複数指標( " summary " )が同時に上振れする "
                   "類似の異常パターンとの一致を示す組合せであり、重点調査対象の候補となる。"
                   "これは反証可能な仮説として記録するものであり、公開情報からは断定できない"
                   "（不正の有無は本アクターの範囲外）。 "
                   "[EN] Statistical anomaly: fiscal year " fiscal-year " for " company
                   " shows a combination of simultaneous multi-metric moves (" summary
                   ") that matches a historically-associated pattern and is a "
                   "candidate for closer review. This is recorded as a falsifiable "
                   "hypothesis; whether wrongdoing occurred is not established from "
                   "public filings alone.")]
    ;; The generator asserts its own output is clean before returning it —
    ;; belt-and-suspenders, NOT a substitute for policy.cljc's own check
    ;; (which the caller always runs regardless of this assertion).
    (assert (:ok? (lexicon/check-text text)) "template drifted out of lexicon compliance")
    {:op :narrative/hold
     :subject {:kind :company :id company :fiscal-year fiscal-year}
     :citations (mapv cite citations)
     :composite-z composite-z
     :flagged? true
     :text text
     :confidence 0.7}))
