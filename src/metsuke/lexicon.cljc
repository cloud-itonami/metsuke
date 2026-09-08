(ns metsuke.lexicon
  "G5 lexicon discipline — deterministic, regex/keyword-based check on
  narrative TEXT. This is NOT an LLM instruction the model might ignore; it
  is a hard code gate a proposal must pass before it can even reach HOLD
  state (policy.cljc calls `check-text` and rejects on any violation).

  Two independent checks, both HARD:

    1. `banned-hits`   — the text contains a DECLARATIVE assertion of
                          wrongdoing about the subject (e.g. \"不正を行った\" /
                          \"committed fraud\"). Any match is a violation,
                          full stop — there is no hedge-word escape hatch on
                          a banned phrase; the LLM stub (llm.cljc) never
                          emits these phrases in the first place, so a
                          violation here in practice only fires on an
                          adversarial/malformed proposal (exactly what the
                          adversarial tests construct).
    2. `unhedged`      — the opposite failure mode: narrative text present
                          but containing NONE of the allow-listed hedge
                          markers at all. `every narrative sentence is a
                          hedged, falsifiable hypothesis, never a verdict`
                          is operationalized here, not left to prose
                          discipline alone.

  This satisfies the owner's requirement that the narrative layer MAY
  mention fraud-possibility (G2) while staying safe: the banned list keeps
  it from ever asserting wrongdoing as settled fact, and the hedge-marker
  requirement keeps it phrased as a falsifiable hypothesis."
  (:require [kotoba.lang.text :as str]))

(def banned-predicates
  "Declarative wrongdoing-assertion patterns, JP + EN. Closed, curated list —
  growing it means arguing a new real-world declarative-fraud phrasing, not
  sliding one in. Case-insensitive on the English patterns."
  [#"不正を行った" #"不正が行われた" #"詐欺を働いた" #"詐欺である"
   #"犯罪である" #"粉飾を行った" #"粉飾決算を行った" #"違法である"
   #"(?i)committed fraud" #"(?i)engaged in fraud" #"(?i)is guilty"
   #"(?i)broke the law" #"(?i)is a fraud" #"(?i)perpetrated fraud"
   #"(?i)violated the law"])

(def hedge-markers
  "Allow-listed hedged framings — a narrative sentence must contain at
  least ONE of these to count as a properly-hedged hypothesis rather than a
  bare, unqualified statement. This is the vocabulary llm.cljc's templates
  are built from (see llm.cljc); the check here is independent of the
  generator so a future template change can't silently drift out of
  compliance."
  [#"統計的異常" #"重点調査対象の候補" #"類似の異常パターンとの一致"
   #"反証可能な仮説として" #"公開情報からは断定できない" #"仮説"
   #"(?i)statistical anomaly" #"(?i)candidate for closer review"
   #"(?i)matches a historically-associated pattern"
   #"(?i)not established from public filings alone"
   #"(?i)hypothesis" #"(?i)does not establish"])

(defn- any-match? [patterns text]
  (boolean (some #(re-find % text) patterns)))

(defn banned-hits
  "Returns the vector of banned-predicate patterns that matched `text`
  (empty if clean)."
  [text]
  (filterv #(re-find % text) banned-predicates))

(defn hedged?
  [text]
  (any-match? hedge-markers text))

(defn check-text
  "The single entry point policy.cljc calls. Returns
  {:ok? bool :violations [{:rule kw :detail str}]}."
  [text]
  (let [banned (banned-hits text)
        violations
        (cond-> []
          (seq banned)
          (conj {:rule :banned-predicate
                 :detail (str "declarative wrongdoing assertion matched: "
                              (pr-str (mapv str banned)))})
          (and (not (seq banned)) (not (hedged? text)) (seq (str/trim text)))
          (conj {:rule :missing-hedge-marker
                 :detail "narrative text contains no allow-listed hedge marker (must read as a falsifiable hypothesis, not a bare statement)"}))]
    {:ok? (empty? violations) :violations violations}))
