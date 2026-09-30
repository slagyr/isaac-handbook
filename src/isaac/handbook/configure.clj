(ns isaac.handbook.configure
  "Crew tool :handbook/configure (wire handbook__configure) — several
   path/value set/unset pairs applied atomically through foundation's
   isaac.foundation.config.mutate/set-many! (isaac-cvri), the same write path
   `isaac config set`/`unset` use (validate, write, hot-reload; never
   writes defaults; no --force). Companion prose fields (a cron job's
   :prompt, a crew's soul) and brand-new entities ride the same call
   through set-many!'s existing plan/merge machinery — no tool-specific
   placement logic here; a new entity lands per isaac.foundation.config.mutate's own
   precedent (isaac-c4em). Refuses a literal write over a path whose
   current value is a `${VAR}` reference. Every call is logged as
   :handbook/configure with the calling crew, session, the requested
   pairs (secrets redacted), and the outcome."
  (:require
    [clojure.string :as str]
    [isaac.foundation.config.loader :as loader]
    [isaac.foundation.config.mutate :as mutate]
    [isaac.foundation.config.paths :as paths]
    [isaac.foundation.logger :as log]))

(def ^:private secret-ref-pattern #"\$\{[^}]+\}")

(defn- secret-ref? [v]
  (boolean (and (string? v) (re-matches secret-ref-pattern v))))

(defn- string-key-map [m]
  (into {} (map (fn [[k v]] [(if (keyword? k) (name k) (str k)) v])) (or m {})))

;; region ----- Config access -----

(defn- root [] (loader/root))

(defn- load-result
  ([] (load-result {}))
  ([opts] (loader/load-config-result (merge {:root (root) :skip-cache? true} opts))))

(defn- raw-value-at [path-str]
  (let [raw    (:config (load-result {:substitute-env? false}))
        segs   (mapv keyword (paths/split-path-segments path-str))]
    (get-in raw segs)))

;; endregion ^^^^^ Config access ^^^^^

;; region ----- Value normalization -----

;; JSON tool-call arguments carry string-keyed maps; Isaac config is
;; keyword-keyed (companion-field detection, entity-table lookups, and the
;; schema itself all key off keywords). Recursively keywordize map keys so a
;; whole-entity `set` (crew.boatswain = {"model": "echo", "soul": "..."})
;; lands the way `isaac config set`'s own stdin-map form would. Leaf scalar
;; values are written exactly as given — same as `isaac.foundation.config.mutate` itself,
;; which never coerces a value's type; only the CLI's own single-path `set`
;; does that (isaac.foundation.config.cli.mutate-common), and handbook__configure has no
;; single-path CLI-style entry point to mirror it through.
(defn- keywordize-keys [v]
  (cond
    (map? v)        (into {} (map (fn [[k child]]
                                    [(keyword (if (keyword? k) (name k) (str k)))
                                     (keywordize-keys child)]))
                          v)
    (sequential? v) (mapv keywordize-keys v)
    :else           v))

;; endregion ^^^^^ Value normalization ^^^^^

;; region ----- Request parsing -----

(defn- set-entries [args]
  (string-key-map (get (string-key-map args) "set")))

(defn- unset-entries [args]
  (let [u (get (string-key-map args) "unset")]
    (cond
      (sequential? u) (mapv str u)
      (string? u)     (if (str/blank? u) [] [u])
      :else           [])))

(defn- secret-refusal [path value]
  (when (and (secret-ref? (raw-value-at path)) (not (secret-ref? value)))
    {:path   path
     :reason (str path " already holds a ${VAR} reference — write a ${VAR} reference to "
                  "change it, never a literal value")}))

(defn- build-request
  "Parses the tool call's set/unset pairs into foundation set-many! ops, or
   collects secret-write refusals when any :set target currently holds a
   `${VAR}` reference and the new value is not itself one."
  [args]
  (let [sets      (set-entries args)
        unsets    (unset-entries args)
        refusals  (vec (keep (fn [[path value]] (secret-refusal path value)) sets))]
    (cond
      (seq refusals)
      {:refusals refusals}

      (and (empty? sets) (empty? unsets))
      {:refusals [{:path nil :reason "at least one of set/unset is required"}]}

      :else
      {:ops (into (mapv (fn [[path value]] {:op :set :path path :value (keywordize-keys value)}) sets)
                  (mapv (fn [path] {:op :unset :path path}) unsets))})))

;; endregion ^^^^^ Request parsing ^^^^^

;; region ----- Response formatting -----

(defn- candidate-files [path-str]
  (let [segs (paths/split-path-segments path-str)]
    (cond-> [(str (first segs) ".edn") paths/root-filename]
      (>= (count segs) 2)
      (into [(str (first segs) "/" (second segs) ".edn")
             (str (first segs) "/" (second segs) ".md")]))))

(defn- files-for [path-str touched]
  (let [touched-set (set touched)]
    (filterv touched-set (candidate-files path-str))))

(defn- with-files [files]
  (if (seq files) (str " (" (str/join ", " files) ")") ""))

(defn- describe-set [touched [path value]]
  (str "set " path " = " (pr-str value) (with-files (files-for path touched))))

(defn- describe-unset [touched path]
  (str "unset " path (with-files (files-for path touched))))

(defn- success-message [args touched warnings]
  (let [lines (into (mapv #(describe-set touched %) (set-entries args))
                    (mapv #(describe-unset touched %) (unset-entries args)))]
    (str "handbook__configure: wrote " (count lines) " pair(s).\n"
         (str/join "\n" (map #(str "- " %) lines))
         (when (seq warnings)
           (str "\nWarnings: " (str/join "; " (map :value warnings)))))))

(defn- refusal-message [reasons]
  (str "handbook__configure: refused — nothing written.\n"
       (str/join "\n" (map #(str "- " %) reasons))))

(defn- error-reason [{:keys [key value]}]
  (str key " - " value))

;; endregion ^^^^^ Response formatting ^^^^^

;; region ----- Logging -----

(defn- log-value [path value]
  (if (secret-ref? (raw-value-at path)) "<redacted: ${VAR} reference>" value))

(defn- log-pairs [args]
  (into (mapv (fn [[path value]] {:op :set :path path :value (log-value path value)}) (set-entries args))
        (mapv (fn [path] {:op :unset :path path}) (unset-entries args))))

(defn- log! [level args outcome & kvs]
  (apply log/log* level :handbook/configure "src/isaac/handbook/configure.clj" 0
         :crew (get (string-key-map args) "caller_crew")
         :session (get (string-key-map args) "session_key")
         :pairs (log-pairs args)
         :outcome outcome
         kvs))

;; endregion ^^^^^ Logging ^^^^^

(defn configure
  [arguments]
  (let [args   (string-key-map arguments)
        {:keys [refusals ops]} (build-request args)]
    (if (seq refusals)
      (let [reasons (map :reason refusals)]
        (log! :warn args :refused :reason (str/join "; " reasons))
        {:isError true :error (refusal-message reasons)})
      (let [outcome (mutate/set-many! (root) ops)]
        (case (:status outcome)
          :ok
          (do
            (log! :info args :written)
            {:result (success-message args (:files outcome) (:warnings outcome))})

          (let [reasons (map error-reason (:errors outcome))
                reasons (if (seq reasons) reasons [(name (:status outcome))])]
            (log! :warn args :refused :reason (str/join "; " reasons))
            {:isError true :error (refusal-message reasons)}))))))

(defn configure-tool-factory [_]
  {:builtin?    true
   :description
   (str "Change Isaac config the same way an operator's `isaac config set`/`unset` "
        "would: validate, write, hot-reload. `set` is a map of dotted config path -> "
        "value; `unset` is a list of dotted config paths. At least one of set/unset "
        "is required. Every pair in one call is applied atomically — an invalid pair "
        "refuses the WHOLE call and nothing is written, even the individually-valid "
        "pairs. No force option: the tool never bypasses validation. Companion prose "
        "fields (a cron job's prompt, a crew's soul) and brand-new entities (a new "
        "crew, a new cron job) can be set in the same call, following config set's "
        "own placement precedent. A literal value can never overwrite a path whose "
        "current value is a ${VAR} reference — repoint it at a different ${VAR} "
        "instead.")
   :parameters
   {:type       "object"
    :properties {"set"   {:type                 "object"
                          :description          "Map of dotted config path -> value to write."
                          :additionalProperties true}
                 "unset" {:type        "array"
                          :items       {:type "string"}
                          :description "Dotted config paths to remove."}}}
   :handler #'configure})
