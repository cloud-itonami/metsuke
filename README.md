# 目付 (metsuke)

**Statistical anomaly-detection framework for public-company financial disclosures.** Tier-B actor ·
R0 design-only · ADR-2607202000 · `did:web:etzhayyim.github.io:com-etzhayyim-metsuke`.

目付 (metsuke) was the Edo-period shogunate inspector role whose job was to watch for and REPORT
improper conduct by officials — not to convict them. This actor keeps that same boundary: it
statistically flags company-fiscal-year rows where multiple financial-disclosure metrics move
together in a way that historically correlates with the kind of pattern regulators look at more
closely, and (only for flagged rows) drafts a hedged, falsifiable-hypothesis narrative sentence. It
never asserts that wrongdoing occurred, and no flagged row ever auto-publishes anywhere — every one
terminates at a permanent `:hold` in this actor's own append-only ledger (see Gates below).

## Why this exists

Motivated by the 2026 KDDI/BIGLOBE/G-Plan circular-transaction accounting-fraud case: G-Plan's public
FY2025/3 disclosure showed simultaneous **+38.5% revenue, +175.7% operating income, +204% total
assets**, and an operating margin roughly doubling (~5.2% → ~10.4%) — exactly the kind of
multi-metric SIMULTANEOUS outlier pattern that should trigger a closer look, even though public
filings alone can never *prove* fraud (only a court/regulator can rule on that). This is a
**general-purpose** anomaly-detection framework, not a one-off analysis of that one filer — the same
scorer runs against any company-fiscal-year row kanjo has ingested.

## Architecture — read-only downstream consumer of kanjo, never a modification of it

```
  com-etzhayyim-kanjo (unmodified)          com-etzhayyim-metsuke (this repo)
  :fin.filing/* :fin.fact/*                 src/metsuke/io.cljk        — the ONLY file-reading seam (G1)
  :fin.metric/* :fin.agg/*        ────►      src/metsuke/methods/score.cljk — deterministic composite z (no LLM)
  (published Datom graph,                    src/metsuke/llm.cljk       — sealed advisor, PROPOSALS only
   G2 non-adjudicating, N4                   src/metsuke/methods/policy.cljk — MetsukeGovernor (G1/G3/G4/G5 HARD)
   NOT fraud/solvency adjudication            src/metsuke/methods/ledger.cljk — append-only audit log (G7)
   — both REAFFIRMED unweakened)             src/metsuke/pipeline.cljk  — wires all of the above; the ONLY
                                              caller of ledger/append
```

**metsuke never writes to kanjo.** It reads kanjo's published Datoms (or a local copy of
`data/facts.merged.kotoba.edn`-shaped input) and produces its own, separate `:metsuke.score/*`
output. kanjo's own gates — **G2 non-adjudicating** ("does NOT rate good/bad, rule fraud/solvency, or
label a company... a transparency map, never a verdict") and **N4** ("NOT fraud/solvency adjudication
— ruling is a state/court matter") — are unmodified and explicitly reaffirmed by this actor's own
design, not weakened by metsuke's existence.

## Gates (see `manifest.edn` for the full text)

| Gate | What it enforces |
|---|---|
| G1 upstream-only | reads ONLY kanjo's Datoms + its own derived score; no other source |
| G2 non-adjudicating, hypothesis-only | never asserts fraud occurred; every sentence is a hedged hypothesis |
| G3 source-basis (HARD) | every scored row / narrative sentence cites specific kanjo Datom ids |
| G4 no named individuals (HARD) | company-level only; no officer/director field exists, checked at runtime too |
| G5 lexicon discipline (HARD, deterministic) | banned-predicate + missing-hedge-marker regex check, not an LLM instruction |
| G6 always-escalate, never-auto-publish | flagged rows are `:hold` by construction; no auto-release code path exists |
| G7 append-only audit ledger | every decision is appended, never mutated, chain-checkable |
| G8 sourcing honesty on coverage | thin peer samples fall back to trailing-history z, or `:low-confidence?`, never a fabricated distribution |

## Scoring

```
composite-z = z(revenue YoY) + z(operating-income YoY) + z(total-assets YoY) + z(operating-margin delta)
```

z is computed against the company's OWN trailing history when >= 3 prior fiscal years are available;
otherwise against a **market-wide** (not sector-specific — kanjo carries no sector dimension on
`:fin.fact` itself, see `src/metsuke/facts.cljk`) same-fiscal-year peer distribution, when >= 5 peer
observations exist. `FLAG-THRESHOLD` (composite-z >= 6.0, see `src/metsuke/methods/score.cljk`
docstring for the rationale) is a named, documented constant, not a bare magic number.

## Run

```bash
kbb --backend sci test/run_tests.cljk                          # 31 tests / 103 assertions, offline, no kanjo checkout needed
kbb --backend sci bin/metsuke.cljk ../com-etzhayyim-kanjo/data/facts.merged.kotoba.edn   # score kanjo's real published data
```

## Test fixture — public historical record, not a live claim

`test/metsuke/fixtures/gplan_fy2025.cljk` reproduces G-Plan's own PUBLIC, already-disclosed FY2025/3
headline figures (the numbers above) as a unit-test fixture, proving the scorer places that row in the
flagged tier relative to 5 synthetic, clearly-labeled non-anomalous peer companies. This is a unit
test of the scoring math — not a narrative document, not a standalone accusation, and not exercised
outside the MetsukeGovernor's own test path (which always ends in `:hold`, see
`test/metsuke/pipeline_test.cljk`).

## R0 honesty — see MATURITY.md

No human/Council release UI for flagged rows exists yet (R0 has no code path past `:hold` at all —
that's the point, not a gap to be embarrassed about). No real generative model is wired into
`llm.cljc` — narrative text is a fixed, lexicon-compliant template. Peer distribution is market-wide,
not sector-linked. See `MATURITY.md` for the full honest scorecard.
