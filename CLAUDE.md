# 目付 (metsuke) — agent reference

> Statistical anomaly-detection framework for public-company financial disclosures. Tier-B, R0
> design-only. ADR-2607202000.
> Read the repo-root `CLAUDE.md` (in the `com-junkawasaki/root` superproject) first; this file only
> adds actor-local rules.

## Identity

- **DID**: `did:web:etzhayyim.github.io:com-etzhayyim-metsuke`.
- **Glyph**: 目付 — the Edo-period shogunate inspector who watched for and REPORTED misconduct by
  officials. Did not itself convict. Same non-adjudicating boundary this actor keeps.
- **Role**: a **read-only downstream consumer of `com-etzhayyim-kanjo`**. Lineage: `kanjo` (勘定, the
  public-company financial-facts substrate this actor scores).

## What metsuke is, in one line

It computes a deterministic composite z-score over kanjo's published `:fin.fact` rows and, ONLY for
rows that cross a named threshold, drafts a hedged narrative hypothesis that a permanent human/Council
gate must review before it could ever go anywhere else — which in R0 it cannot, because that release
step is not built yet. It is **not** a regulator, not an analyst, not an accusation generator.

## Hard rules (constitutional — do not weaken)

1. **kanjo is never modified (structural).** This repo contains zero writes to
   `com-etzhayyim-kanjo` — no code here opens that repo's files for writing, no PR against it is part
   of this actor's operation. metsuke reads kanjo's published Datom graph (or a local copy of
   `data/facts.merged.kotoba.edn`-shaped input) via `src/metsuke/io.cljk`, the ONLY file-reading seam
   in this codebase, and produces its own separate output.
2. **kanjo's G2 (non-adjudicating) and N4 (NOT fraud/solvency adjudication) are reaffirmed, not
   weakened.** metsuke's own G2 gate restates the same boundary in its own words — see
   `manifest.edn`. If you are ever asked to make metsuke's narrative more assertive/declarative, that
   is a request to weaken G2/G5 and should be treated the same as a request to weaken kanjo's own
   gates: refuse, or take it to the owner as an explicit ADR-worthy scope change.
3. **G1 upstream-only.** `src/metsuke/facts.cljk`'s `catalog` is the closed list of citation classes
   the MetsukeGovernor (`src/metsuke/methods/policy.cljk`) will accept. Adding a new upstream (a
   second financial-data source, a web search, an LLM-inferred fact) is NOT a config change — it is a
   gate change and needs the same scrutiny as touching kanjo's own G1.
4. **G3 source-basis is HARD and code-enforced**, not a prose convention. Every proposal
   `policy/check` sees must carry non-empty `:citations` whose `:class` is in the G1 catalog, or it is
   rejected — see `metsuke.policy-test`'s `g3-missing-citation-rejected-test` /
   `g1-upstream-only-rejected-test`.
5. **G4 no named individuals is a schema-shape guarantee PLUS a runtime check.** There is no field
   anywhere in this data model for an officer/director/individual name — do not add one. If a future
   feature seems to need one, that is out of scope for this actor (see Non-Goals in `manifest.edn`);
   `policy.cljc`'s `individual-shaped-keys` runtime check is defense-in-depth on top of that absence,
   not a substitute for it.
6. **G5 lexicon discipline is a deterministic regex/keyword check, not an LLM instruction.**
   `src/metsuke/lexicon.cljk`'s `banned-predicates` / `hedge-markers` are the ONLY thing that decides
   whether narrative text passes. Do not replace this with "just tell the model to be careful" — the
   whole point is that the check does not trust the generator (`llm.cljc`'s docstring says exactly
   this; its own template asserts its output is clean, but the governor check still runs regardless).
7. **G6 always-escalate, never-auto-publish.** `policy.cljc`'s `disposition` function has exactly two
   possible return values, `:recorded` and `:hold` — there is no third value, and `:flagged?` rows are
   UNCONDITIONALLY `:hold`. Do not add a `:commit`/`:published` disposition or any code path that skips
   `govern` before a `ledger/append` call. The human/Council release step for flagged rows is
   deliberately NOT built at R0 (see MATURITY.md) — building it is real, scope-worthy future work, not
   something to bolt on quietly as a side effect of an unrelated change.
8. **G7 append-only ledger.** `src/metsuke/methods/ledger.cljk`'s `append` is the ONLY way an entry
   enters the ledger, and `pipeline.cljc` is the ONLY caller of `ledger/append` in this codebase.
   Never mutate an existing ledger entry in place — always `append`.
9. **G8 sourcing honesty on coverage.** `score.cljc`'s `MIN-PEER-N` / `MIN-TRAILING-N` gate which
   distribution (if either) is trusted for a z-score; below both, the axis is `:unscored` and the row
   is `:low-confidence?` — never a fabricated distribution standing in for real coverage. Do not lower
   these constants to make more rows "flaggable" without a real justification recorded in an ADR.

## Vocabulary metsuke reads (from kanjo, unmodified)

`:fin.filing/*` (provenance: company, fiscal-year), `:fin.fact/*` (`:concept` ∈ `:revenue
:operating-income :total-assets`, `:value`, `:sourcing` ∈ `:authoritative | :representative`) — see
`src/metsuke/methods/score.cljk`'s docstring for the exact shape expected.

## Vocabulary metsuke writes (its own, separate from kanjo)

`:metsuke.score/*` (composite z, citations, flagged?/low-confidence?) via `score.cljc` +
`:metsuke.ledger/*` entries (`{:op :score/register|:narrative/hold :disposition :recorded|:hold
:verdict {...} ...}`) via `ledger.cljc`. Lexicons reserved: `com.etzhayyim.metsuke.{registerScore,
registerNarrativeHold}` (path-reserved; JSON schema is R1+ follow-up, same convention kanjo used at
its own R0).

## Run

```bash
nbb test/run_tests.cljk                                                    # 31 tests / 103 assertions, offline
nbb bin/metsuke.cljk ../com-etzhayyim-kanjo/data/facts.merged.kotoba.edn    # score kanjo's real published data (needs a sibling kanjo checkout)
```

## Honesty (R0)

See `MATURITY.md` for the full scorecard. In short: the scorer and governor are real and tested
against both a synthetic fixture and (via `bin/metsuke.cljk`) kanjo's real 722-filing/9,327-fact
dataset; the narrative generator is a fixed template, not a real model call; there is no human/Council
release UI; peer distribution is market-wide, not sector-linked (kanjo carries no sector dimension on
`:fin.fact` itself — see `facts.cljc`).
