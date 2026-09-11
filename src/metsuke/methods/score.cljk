(ns metsuke.methods.score
  "G1/G3/G8 deterministic composite anomaly z-score. NO LLM, NO browser — a
  pure data transform over kanjo's own published :fin.fact rows, in the same
  spirit as 90-docs/design-quality/audit.cljc's non-LLM fitness function
  (data-driven axes, each yielding a score + a citable basis).

  ## Input shape (G1 — kanjo is the ONLY upstream)

  `facts`   — seq of kanjo :fin.fact/* maps, e.g.
              {:fin.fact/id \"fact.org.corp.jp.toyota.2024.pl.revenue.consolidated\"
               :fin.fact/filing \"fil.jp.edinet.toyota.2024\"
               :fin.fact/company \"org.corp.jp.toyota\"
               :fin.fact/concept :revenue   ; :revenue | :operating-income | :total-assets | ...
               :fin.fact/value 45095000.0
               :fin.fact/sourcing :representative}  ; :authoritative | :representative
              (:synthesized facts are never emitted by kanjo itself for
              :fin.fact — kanjo's own G5 forbids it — but rows carrying it
              are defensively dropped here too, never treated as a disclosed
              fact.)
  `filings` — seq of kanjo :fin.filing/* maps, e.g.
              {:fin.filing/id \"fil.jp.edinet.toyota.2024\"
               :fin.filing/company \"org.corp.jp.toyota\"
               :fin.filing/fiscal-year 2024}
              (used only to resolve each fact's fiscal-year; :fin.fact/company
              is already denormalized onto the fact itself per kanjo's own
              schema, per its registerFinancialFact lexicon.)

  ## Peer distribution — market-wide, NOT sector-specific (honest R0 scope)

  kanjo's :fin.fact stream carries no sector dimension on the fact itself
  (sector is joined from a separate actor, `kabuto`, which metsuke does NOT
  read — G1 upstream-only). Rather than fabricate a sector join metsuke has
  no sourced basis for, R0's peer group for a given fiscal year is simply
  \"every other company with usable metrics in the same kanjo dataset for
  that fiscal year\" — market-wide, not sector-specific. This is a
  documented, honest limitation (see `facts/coverage`), not silently
  presented as sector-matched. A sourced sector-linkage join is a follow-up
  (MATURITY.md), not built here.

  ## Composite score

  composite-z = z(revenue YoY) + z(operating-income YoY) + z(total-assets YoY)
                + z(operating-margin delta)

  Each axis's z is computed against the company's OWN trailing history when
  >= MIN-TRAILING-N prior-year observations of that axis exist; otherwise
  against the market-wide same-fiscal-year peer distribution when
  >= MIN-PEER-N peer observations exist (G8). If NEITHER threshold is met for
  an axis, that axis contributes z=0 to the composite and the row is marked
  `:low-confidence? true` with the unscored axis named — never a fabricated
  distribution standing in for real coverage."
  (:require [kotoba.lang.text :as str]))

;; ───────────────────────── named, documented constants ─────────────────────────

(def MIN-PEER-N
  "Minimum number of OTHER companies in the same kanjo dataset + fiscal year
  required before a market-wide peer z-score is trusted (G8). Below this,
  score.cljc falls back to trailing-history (if available) or marks the axis
  :low-confidence rather than compute a z off a handful of companies."
  5)

(def MIN-TRAILING-N
  "Minimum number of the company's OWN prior-year YoY/margin-delta
  observations required before a trailing-history z-score is trusted —
  \"own trailing history when >= 3 prior fiscal years are available\"."
  3)

(def FLAG-THRESHOLD
  "Composite z-score at/above which a company-fiscal-year row is :flagged?
  (G6 HOLD-by-construction). Rationale, stated explicitly rather than left a
  bare magic number: this is a SUM of 4 axis z-scores, so a threshold of 6.0
  corresponds to (loosely) all four axes landing simultaneously ~1.5 std
  devs above their respective distribution's mean — i.e. the signal this
  actor exists to surface is specifically SIMULTANEOUS multi-axis outlier
  behavior, not any single extreme axis on its own (a company with one wildly
  volatile line item but three unremarkable ones should NOT flag; the
  G-Plan-shaped fixture in test/metsuke/fixtures exercises exactly this
  simultaneity)."
  6.0)

(def EPSILON
  "Floor added to a distribution's stdev before dividing, to avoid a
  divide-by-zero when every peer/trailing observation is identical. Chosen
  small enough not to meaningfully distort a real (non-degenerate)
  distribution."
  1.0e-6)

;; ───────────────────────── row extraction ─────────────────────────

(def tracked-concepts #{:revenue :operating-income :total-assets})

(defn- filing-index [filings]
  (into {} (map (fn [f] [(:fin.filing/id f) f])) filings))

(def ^:private sourcing-rank {:authoritative 2 :representative 1})

(defn best-facts
  "Groups `facts` by [company fiscal-year concept], keeping the
  highest-sourcing-rank row per group (:authoritative beats :representative;
  :synthesized rows are dropped — a disclosed fact is never :synthesized per
  kanjo's own G5, and this actor does not treat a derived figure as if it
  were a filed one). Returns {[company fy concept] fact-map-with-:fiscal-year}."
  [facts filings]
  (let [by-filing (filing-index filings)]
    (->> facts
         (filter #(contains? tracked-concepts (:fin.fact/concept %)))
         (filter #(contains? sourcing-rank (:fin.fact/sourcing %)))
         (keep (fn [f]
                 (when-let [fy (:fin.filing/fiscal-year (get by-filing (:fin.fact/filing f)))]
                   (assoc f :fiscal-year fy))))
         (group-by (juxt :fin.fact/company :fiscal-year :fin.fact/concept))
         (reduce-kv (fn [m k rows]
                      (assoc m k (apply max-key #(sourcing-rank (:fin.fact/sourcing %)) rows)))
                    {}))))

(defn company-year-metrics
  "{[company fy] {:revenue {:value v :id id} :operating-income {...}
                  :total-assets {...}}} — only concepts actually present."
  [best]
  (reduce-kv (fn [m [company fy concept] fact]
               (assoc-in m [[company fy] concept]
                         {:value (:fin.fact/value fact) :id (:fin.fact/id fact)}))
             {} best))

(defn- pct-change [prev curr]
  (when (and prev (not (zero? prev))) (/ (- curr prev) (double prev))))

(defn- margin [row]
  (let [rev (get-in row [:revenue :value]) op (get-in row [:operating-income :value])]
    (when (and rev op (not (zero? rev))) (/ op (double rev)))))

(defn axis-deltas
  "For every [company fy] where fy AND fy-1 both have revenue +
  operating-income + total-assets, computes the 4 axes + the citation set.
  Returns [{:company .. :fiscal-year .. :revenue-yoy .. :operating-income-yoy
  .. :total-assets-yoy .. :margin-delta .. :citations #{fact-id ...}} ...]."
  [company-year-metrics-map]
  (for [[[company fy] row] company-year-metrics-map
        :let [prev (get company-year-metrics-map [company (dec fy)])]
        :when (and prev
                   (every? #(contains? row %) tracked-concepts)
                   (every? #(contains? prev %) tracked-concepts))
        :let [rev-yoy (pct-change (get-in prev [:revenue :value]) (get-in row [:revenue :value]))
              oi-yoy  (pct-change (get-in prev [:operating-income :value]) (get-in row [:operating-income :value]))
              ta-yoy  (pct-change (get-in prev [:total-assets :value]) (get-in row [:total-assets :value]))
              m-curr  (margin row)
              m-prev  (margin prev)]
        :when (and rev-yoy oi-yoy ta-yoy m-curr m-prev)]
    {:company company
     :fiscal-year fy
     :revenue-yoy rev-yoy
     :operating-income-yoy oi-yoy
     :total-assets-yoy ta-yoy
     :margin-delta (- m-curr m-prev)
     :citations (into (sorted-set)
                       (map :id [(get row :revenue) (get row :operating-income) (get row :total-assets)
                                 (get prev :revenue) (get prev :operating-income) (get prev :total-assets)]))}))

;; ───────────────────────── distributions ─────────────────────────

(defn- mean [xs] (/ (reduce + 0.0 xs) (count xs)))

(defn- stdev [xs]
  (let [m (mean xs) n (count xs)]
    (Math/sqrt (/ (reduce + 0.0 (map #(let [d (- % m)] (* d d)) xs)) n))))

(defn distribution
  "{:mean :stdev :n} over a seq of numeric axis values, or nil if empty."
  [xs]
  (when (seq xs) {:mean (mean xs) :stdev (stdev xs) :n (count xs)}))

(def axis-keys [:revenue-yoy :operating-income-yoy :total-assets-yoy :margin-delta])

(defn peer-distributions
  "axis-key -> distribution, over every OTHER company's row in the SAME
  fiscal year as `row` (market-wide, not sector-specific — see ns docstring)."
  [row all-rows]
  (let [peers (remove #(= (:company %) (:company row))
                       (filter #(= (:fiscal-year %) (:fiscal-year row)) all-rows))]
    (into {} (map (fn [k] [k (distribution (map k peers))])) axis-keys)))

(defn trailing-distributions
  "axis-key -> distribution, over `row`'s own company's prior-fiscal-year
  observations (strictly earlier fiscal-year than `row`)."
  [row all-rows]
  (let [own-prior (filter #(and (= (:company %) (:company row))
                                 (< (:fiscal-year %) (:fiscal-year row)))
                           all-rows)]
    (into {} (map (fn [k] [k (distribution (map k own-prior))])) axis-keys)))

(defn- z [x {:keys [mean stdev] :as _dist}]
  (/ (- x mean) (max stdev EPSILON)))

(defn axis-score
  "Picks trailing (if n >= MIN-TRAILING-N) else peer (if n >= MIN-PEER-N)
  else :unscored (z contributes 0, :low-confidence). Returns
  {:z n :source (:trailing|:peer|:unscored) :n int}."
  [x trailing-dist peer-dist]
  (cond
    (and trailing-dist (>= (:n trailing-dist) MIN-TRAILING-N))
    {:z (z x trailing-dist) :source :trailing :n (:n trailing-dist)}

    (and peer-dist (>= (:n peer-dist) MIN-PEER-N))
    {:z (z x peer-dist) :source :peer :n (:n peer-dist)}

    :else
    {:z 0.0 :source :unscored :n (or (:n trailing-dist) (:n peer-dist) 0)}))

(defn score-row
  "Scores one axis-delta row (from `axis-deltas`) against `all-rows` (the
  full seq of axis-delta rows the caller is scoring together — used to build
  peer/trailing distributions). Returns the row plus :axis-scores,
  :composite-z, :flagged?, :low-confidence?, :unscored-axes, :citations
  (carried through unchanged — G3 traceability)."
  [row all-rows]
  (let [peer (peer-distributions row all-rows)
        trailing (trailing-distributions row all-rows)
        axis-scores (into {} (map (fn [k] [k (axis-score (get row k) (get trailing k) (get peer k))])) axis-keys)
        composite (reduce + 0.0 (map (comp :z second) axis-scores))
        unscored (into [] (comp (filter #(= :unscored (:source (second %)))) (map first)) axis-scores)]
    (assoc row
           :axis-scores axis-scores
           :composite-z composite
           :flagged? (>= composite FLAG-THRESHOLD)
           :low-confidence? (boolean (seq unscored))
           :unscored-axes unscored)))

(defn score-rows
  "Scores every row in `axis-delta-rows` against the whole set (peer
  distributions are built per-fiscal-year across the full input, so pass ALL
  company-fiscal-year rows you want considered together, not one at a time)."
  [axis-delta-rows]
  (mapv #(score-row % axis-delta-rows) axis-delta-rows))

(defn score-facts
  "Top-level entry point: kanjo `facts` + `filings` -> scored rows. This is
  the function callers (e.g. a future nbb CLI, or a test) actually call."
  [facts filings]
  (-> (best-facts facts filings)
      company-year-metrics
      axis-deltas
      score-rows))
