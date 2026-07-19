(ns metsuke.methods.policy
  "MetsukeGovernor — the independent check layer a Metsuke-LLM proposal
  (llm.cljc) must pass before ANYTHING is written to the append-only ledger
  (ledger.cljc). Same shape as cloud-itonami-isic-8291's DisclosureGovernor
  and cloud-itonami-6310's PolicyGovernor: the LLM has no notion of citation
  provenance, schema scope, or lexicon discipline, so a separate system must
  be able to reject a proposal outright.

  **Single invariant**: the Metsuke-LLM never writes/discloses/asserts what
  the MetsukeGovernor would reject. `check` is the only function that
  decides `:ok?`; `disposition` is the only function that decides where a
  checked proposal lands, and it can ONLY ever land on `:recorded` (clean,
  non-flagged) or `:hold` (anything flagged, or anything that failed a hard
  check) — there is no `:commit`/`:published` disposition in this codebase
  at all (G6: no code path auto-releases a flagged row).

  Checks, in order:

    1. source-basis    (HARD) — proposal must carry >= 1 citation (G3).
    2. upstream-only    (HARD) — every citation's :class must be one of
                                 metsuke.facts/allowed-source-classes (G1).
    3. no-individuals   (HARD) — subject must be :kind :company, and no
                                 individual-shaped key appears anywhere in
                                 the proposal (G4 — schema-shape guarantee
                                 PLUS this runtime check, defense in depth).
    4. lexicon          (HARD) — proposal :text, if present, must pass
                                 metsuke.lexicon/check-text (G5).

  A HARD violation forces `:hold`, unconditionally — this is NOT something a
  human approver can override at the point of `check`; a human/Council
  release step is a SEPARATE, not-yet-built process outside this codebase
  (MATURITY.md), which is exactly what makes `:hold` here a true terminal
  state in R0, not a queue waiting on code that already exists."
  (:require [clojure.set :as set]
            [metsuke.facts :as facts]
            [metsuke.lexicon :as lexicon]))

(def individual-shaped-keys
  "Keys that would smuggle a named-individual field into a proposal. There is
  no corresponding field anywhere in this actor's data model (company-year
  score rows only) — this set exists as defense in depth against an LLM (or
  a future schema change) adding one, mirroring cloud-itonami-isic-8291's
  private-life-keys / dossier scope-gate."
  #{:officer-name :officer :director-name :director :individual :individual-name
    :person :person-name :executive :executive-name :beneficial-owner-name})

(defn- source-basis-violations [{:keys [citations]}]
  (when (empty? citations)
    [{:rule :source-basis :detail "proposal carries no citations — every scored row and narrative sentence must cite specific kanjo Datom ids"}]))

(defn- upstream-violations [{:keys [citations]}]
  (let [bad (remove #(facts/class-allowed? (:class %)) citations)]
    (when (seq bad)
      [{:rule :upstream-only :detail (str "citation(s) outside the allowed kanjo/metsuke catalog: " (pr-str bad))}])))

(defn- proposal-keys [proposal]
  (into (set (keys proposal)) (mapcat keys (vals (select-keys proposal [:subject])))))

(defn- no-individuals-violations [{:keys [subject] :as proposal}]
  (let [bad-keys (set/intersection (proposal-keys proposal) individual-shaped-keys)
        wrong-kind? (and subject (not= :company (:kind subject)))]
    (cond-> []
      (seq bad-keys)
      (conj {:rule :no-individuals :detail (str "proposal carries an individual-shaped field: " (vec bad-keys))})
      wrong-kind?
      (conj {:rule :no-individuals :detail (str "subject :kind must be :company, got " (pr-str (:kind subject)))}))))

(defn- lexicon-violations [{:keys [text]}]
  (when (and text (seq text))
    (let [{:keys [ok? violations]} (lexicon/check-text text)]
      (when-not ok? violations))))

(defn check
  "Returns {:ok? bool :hard? bool :violations [..]}. `:ok?` means clean —
  still subject to `disposition`'s :flagged? override (G6)."
  [proposal]
  (let [violations (into []
                          (concat (source-basis-violations proposal)
                                  (upstream-violations proposal)
                                  (no-individuals-violations proposal)
                                  (lexicon-violations proposal)))]
    {:ok? (empty? violations) :hard? (boolean (seq violations)) :violations violations}))

(defn disposition
  "The ONLY two possible outcomes in this codebase: :recorded or :hold.
  :flagged? always forces :hold regardless of how clean the proposal is
  (G6) — this function has no branch that could ever return anything else,
  by construction, not by convention."
  [proposal verdict]
  (cond
    (:hard? verdict) :hold
    (:flagged? proposal) :hold
    :else :recorded))

(defn govern
  "The single call site llm.cljc's caller (pipeline) uses: checks the
  proposal and returns {:verdict .. :disposition ..}."
  [proposal]
  (let [verdict (check proposal)]
    {:verdict verdict :disposition (disposition proposal verdict)}))
