(ns metsuke.methods.ledger
  "G7 append-only audit ledger. Every score computed and every narrative
  HOLD/decision is written here — never mutated in place. Same commit-DAG
  shape as kanjo's own Datom log / kosatsu's identity journal: each entry
  chains to the previous entry's content hash, so tampering with an entry
  after the fact breaks the chain (`validate-chain` below detects it).

  This is a pure, in-memory, portable `.cljc` structure — `append` takes and
  returns a ledger value (a vector of entries); there is no hidden mutable
  state and no I/O here. A caller that wants durability spits the returned
  vector to an EDN file (see README.md's nbb CLI note) — that's a deliberate
  separation, the same one score.cljc/policy.cljc/llm.cljc keep from any
  I/O concern.")

(def ^:private hex-digits "0123456789abcdef")

(defn- to-hex
  "Portable unsigned-int -> lowercase-hex-string (no Integer/toHexString,
  which is JVM-only — `unsigned-bit-shift-right` is available on both the
  JVM and cljs, so this needs no reader-conditional)."
  [n]
  (loop [n (bit-and n 0xffffffff) acc ()]
    (if (zero? n)
      (apply str (or (seq acc) [\0]))
      (recur (unsigned-bit-shift-right n 4)
             (cons (nth hex-digits (bit-and n 0xf)) acc)))))

(defn- char-code [s i]
  #?(:clj (int (.charAt ^String s i))
     :cljs (.charCodeAt s i)))

(defn- stable-hash
  "A small, portable, deterministic string hash (djb2 over `pr-str`) — good
  enough for local tamper-evidence in R0, NOT a cryptographic content
  address. Swapping in a real content-addressed hash (matching kanjo's own
  CID scheme) is a documented R1+ follow-up, not silently pretended here."
  [v]
  (let [s (pr-str v)]
    (loop [h 5381 i 0]
      (if (= i (count s))
        (str "djb2:" (to-hex h))
        (recur (bit-and (+ (* h 33) (char-code s i)) 0xffffffff) (inc i))))))

(defn append
  "Appends `entry` (a plain map — :op/:subject/:citations/:disposition/etc,
  whatever the caller passes) to `ledger` (a vector, empty at the start).
  Returns the NEW ledger — `ledger` itself is never mutated (it's an
  immutable persistent vector already; this fn just makes the append-only
  discipline the documented contract, not an accident of the data structure)."
  [ledger entry]
  (let [prev-hash (:entry/hash (peek ledger))
        seq-no (count ledger)
        stamped (assoc entry :entry/seq seq-no :entry/prev prev-hash)
        h (stable-hash stamped)]
    (conj (vec ledger) (assoc stamped :entry/hash h))))

(defn validate-chain
  "Returns {:ok? bool :broken-at (seq-no|nil)} — walks the ledger checking
  each entry's :entry/prev against the actual previous entry's :entry/hash,
  and that the entry's own :entry/hash matches a fresh stable-hash of its
  content (minus the hash field itself). Any mismatch means the ledger was
  mutated out-of-band after the fact — this is the append-only invariant
  made checkable, not just promised in a docstring."
  [ledger]
  (loop [i 0 prev-hash nil]
    (if (= i (count ledger))
      {:ok? true :broken-at nil}
      (let [entry (nth ledger i)
            unhashed (dissoc entry :entry/hash)
            recomputed (stable-hash unhashed)]
        (if (and (= prev-hash (:entry/prev entry))
                 (= recomputed (:entry/hash entry)))
          (recur (inc i) (:entry/hash entry))
          {:ok? false :broken-at i})))))
