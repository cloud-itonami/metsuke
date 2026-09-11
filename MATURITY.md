# 目付 (metsuke) — Maturity Scorecard

Honest R0 status, same discipline as kanjo's own MATURITY.md. Per ADR-2607202000.

## What R0 actually proves

| Area | Status |
|---|---|
| Deterministic composite z-scorer (`score.cljc`) | **runnable, unit-tested** — 4-axis composite z, own-trailing-history branch, market-wide peer branch, G8 thin-coverage fallback (`:unscored`/`:low-confidence?`), all exercised by `metsuke.score-test`. |
| MetsukeGovernor (`policy.cljc`) | **runnable, unit-tested, adversarially tested** — `metsuke.policy-test` hand-constructs a proposal violating EACH hard check (missing citation, out-of-catalog citation, individual-shaped field, wrong subject kind, banned predicate, unhedged text) and asserts rejection; also asserts the G6 invariant that `:flagged?` forces `:hold` even on an otherwise-clean proposal. |
| Lexicon discipline (`lexicon.cljc`) | **runnable, unit-tested** — banned-predicate (JP+EN) and missing-hedge-marker checks, both deterministic regex, no LLM call. |
| Append-only ledger (`ledger.cljc`) | **runnable, unit-tested** — chain hashing + `validate-chain` proven to detect an in-place mutation of a prior entry. |
| End-to-end pipeline (`pipeline.cljc`) | **runnable, unit-tested against BOTH a synthetic fixture AND kanjo's real 722-filing dataset** (`bin/metsuke.cljk` run manually against kanjo's `data/facts.merged.kotoba.edn` produces real scored rows — this is a real integration, not a mock). |
| G-Plan historical test fixture | **exists, proven to flag** — `test/metsuke/fixtures/gplan_fy2025.cljk` reproduces G-Plan's own public FY2025/3 headline figures; `metsuke.score-test/gplan-fixture-flags-test` asserts composite-z >= FLAG-THRESHOLD relative to 5 synthetic non-anomalous peers. |

## What is NOT done (by design at R0, honestly listed)

| Question | Status |
|---|---|
| Is there a human/Council release step for a flagged row? | **NO.** R0 has zero code paths past `:hold` — `policy.cljc`'s `disposition` function can only ever return `:recorded` or `:hold`. Building an actual review/release UI (who approves, what surface it publishes to, what audit trail THAT step gets) is real, scope-worthy R1 work, not sketched here beyond "it must exist before anything flagged could ever be shown outside this actor's own ledger". |
| Is there a real generative model behind the narrative? | **NO.** `llm.cljc`'s `draft-narrative-proposal` is a fixed, deterministic template built only from `lexicon.cljc`'s allow-listed hedge phrases. Wiring a real model (e.g. via `murakumo-main`, root CLAUDE.md's LLM-alias convention) is a documented follow-up — and when it happens, the SAME governor checks in `policy.cljc` still gate its output; nothing about wiring a real model changes the enforcement boundary. |
| Is peer distribution sector-specific? | **NO — market-wide.** kanjo's `:fin.fact` carries no sector dimension on the fact itself (sector is a `kabuto`-side join kanjo's own README describes, and metsuke does not read kabuto at all — G1). `facts.cljc`'s docstring states this explicitly. A sourced sector-linkage join (e.g. reading kanjo's own `:fin.agg :dimension :sector` more thoroughly, or a future actor providing a citable sector taxonomy) is a real follow-up, not silently assumed. |
| Any live/production deployment, feed, or publish surface? | **NO.** metsuke has no server, no atproto publish pipeline, no scheduled job. `actor.edn` is a stub identity file only — no `:regen`/`:bundle`, unlike kanjo's. |
| Full-market coverage? | **NO.** Coverage is entirely whatever subset of kanjo's own dataset is passed in — metsuke fabricates nothing and ingests nothing of its own; kanjo's own coverage limits (722 filings / ~47 US filers as of this ADR) are metsuke's limits too. |
| Cryptographic content-addressing on the ledger? | **NO** — `ledger.cljc`'s `stable-hash` is a documented, portable djb2-style hash for LOCAL tamper-evidence only, explicitly NOT presented as a cryptographic content address (unlike kanjo's real CID scheme). |

## Path to R1

1. Design and build the human/Council release step (who approves a `:hold`, what surface — if any —
   it can ever move a row to, and what NEW audit trail that step itself needs) as its own scoped ADR.
2. Wire a real model into `llm.cljc` behind the `murakumo-main` alias, with the same governor gate
   unchanged.
3. A sourced sector-linkage join (contingent on a real citable source — not fabricated) to move the
   peer distribution from market-wide to sector-specific.
4. Swap `ledger.cljc`'s djb2 stand-in for a real content-addressed hash, matching kanjo's own CID
   convention, if/when this actor needs cross-process/cross-machine ledger verification.
