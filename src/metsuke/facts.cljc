(ns metsuke.facts
  "G1 upstream-only source-basis catalog — the ONLY source classes the
  MetsukeGovernor (policy.cljc) will accept as a citation for a scored
  company-fiscal-year row or a narrative proposal.

  metsuke is a READ-ONLY downstream consumer of com-etzhayyim-kanjo's
  published kotoba Datom graph (:fin.filing/* / :fin.fact/* / :fin.metric/* /
  :fin.agg/*, ADR-2606032000) — it never writes to kanjo, and it reads no
  other data source (no paid terminals, no web search, no LLM-inferred
  facts). This mirrors kanjo's own G1 (Tier-A primary disclosure only) and
  cloud-itonami-isic-8291's dossier.facts catalog pattern, narrowed to a
  single upstream instead of a multi-jurisdiction registry list.

  :metsuke.score/* (this actor's own composite-z output, score.cljc) is the
  only NON-kanjo class allowed — it cites the kanjo facts it was derived
  from, so a score row is always traceable back to a :class :kanjo/fin-fact
  or :kanjo/fin-metric citation, never a bare assertion.

  Honesty over coverage, same discipline as kanjo/dossier: this catalog does
  NOT claim market-wide coverage. See `coverage` below and
  score.cljc's MIN-PEER-N fallback (G8).")

(def catalog
  "Each entry: {:id :name :class :covers :access}. :class is the value that
  must appear in a citation's :class for the MetsukeGovernor's source-basis
  check (G3) to accept it."
  [{:id :kanjo-fin-filing
    :name "kanjo :fin.filing/* — primary disclosure filing record"
    :class :kanjo/fin-filing
    :covers #{:filing-provenance}
    :access :local-read-seam}
   {:id :kanjo-fin-fact
    :name "kanjo :fin.fact/* — disclosed financial line item (revenue / operating-income / total-assets / ...)"
    :class :kanjo/fin-fact
    :covers #{:company-financial-line-item}
    :access :local-read-seam}
   {:id :kanjo-fin-metric
    :name "kanjo :fin.metric/* — kanjo-derived ratio/YoY (:synthesized per kanjo G5)"
    :class :kanjo/fin-metric
    :covers #{:company-derived-ratio}
    :access :local-read-seam}
   {:id :kanjo-fin-agg
    :name "kanjo :fin.agg/* — kanjo sector/coverage aggregate (:synthesized, coverage-bounded per kanjo G5)"
    :class :kanjo/fin-agg
    :covers #{:coverage-count}
    :access :local-read-seam}
   {:id :metsuke-score
    :name "metsuke :metsuke.score/* — this actor's own composite z-score (derived, self-citing back to kanjo facts)"
    :class :metsuke/score
    :covers #{:composite-anomaly-score}
    :access :derived}])

(def allowed-source-classes
  "Closed set — a citation whose :class is not here (e.g. :inference,
  :web-search, :paid-terminal, :social-media) is rejected outright by
  policy.cljc's source-basis check, not silently accepted."
  (into #{} (map :class catalog)))

(defn class-allowed? [source-class]
  (contains? allowed-source-classes source-class))

(defn coverage
  "Honest, machine-checkable statement of what this catalog covers — never
  overstated in prose. metsuke's OWN market coverage is bounded entirely by
  whatever subset of kanjo's Datom graph the caller passes to score.cljc; this
  function only describes the fixed set of ALLOWED citation classes, not a
  live count of ingested rows (that honesty belongs to the caller's own
  kanjo dataset, e.g. kanjo's own `coverage` fn in its repo)."
  []
  {:source-count (count catalog)
   :covers (into (sorted-set) (mapcat :covers catalog))
   :note (str "R0 scope: " (count catalog) " allowed citation classes, ALL "
              "either kanjo's own published Datom classes or metsuke's own "
              "derived score class. No other upstream is read (G1). "
              "Sector-linkage (e.g. kabuto :company sector) is NOT read — "
              "metsuke's peer distribution is market-wide within the same "
              "kanjo dataset/fiscal-year, not sector-specific (see "
              "score.cljc docstring); a sourced sector join is a documented "
              "R1 follow-up, not fabricated here.")})
