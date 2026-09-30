(ns isaac.handbook.render
  "Pure handbook__read logic: given already-resolved, already-ordered
   chapters (module id + raw markdown), renders the table of contents or
   the requested topics, applying the response size cap. No module
   discovery, no filesystem — isaac.handbook.chapters does that and hands
   this namespace plain data."
  (:require
    [clojure.string :as str]
    [isaac.handbook.sections :as sections]))

(def default-max-chars 40000)

;; region ----- Topic index -----

(defn- chapter-topic [module-id text]
  {:id      module-id
   :content (str "# " module-id "\n\n" text)})

(defn- section-topic [module-id {:keys [slug title body]}]
  {:id      (str module-id "#" slug)
   :content (str "## " module-id "#" slug " — " title "\n\n" body)})

(defn- strip-html-comments
  "Drops every `<!-- ... -->` block (including multiline ones, such as a
   lint-convention authoring note left at the top of a chapter) before the
   text is ever assembled into a table-of-contents entry or topic response
   — a stray authoring comment must never reach a crew."
  [text]
  (-> text
      (str/replace #"(?s)<!--.*?-->\n*" "")
      str/triml))

(defn- chapter-entry [{:keys [module-id text]}]
  (let [text (strip-html-comments text)]
    {:module-id module-id
     :text      text
     :sections  (sections/sections text)}))

(defn- topic-entries
  "Every chapter's own entries, sections attached."
  [chapters]
  (mapv chapter-entry chapters))

(defn- topics-by-id [entries]
  (into {}
        (mapcat (fn [{:keys [module-id text sections]}]
                  (cons [module-id (chapter-topic module-id text)]
                        (map #(let [t (section-topic module-id %)] [(:id t) t]) sections))))
        entries))

;; endregion ^^^^^ Topic index ^^^^^

;; region ----- Size cap -----

(defn- apply-cap
  "Keeps whole chunks (never cuts mid-topic) up to max-chars, always
   keeping at least the first chunk so the response is never empty.
   Returns {:kept [chunk...] :omitted [name...]}."
  [chunks max-chars]
  (loop [remaining chunks kept [] total 0]
    (if (empty? remaining)
      {:kept kept :omitted []}
      (let [{:keys [text] :as chunk} (first remaining)
            new-total (+ total (count text))]
        (if (and (seq kept) (> new-total max-chars))
          {:kept kept :omitted (mapv :name remaining)}
          (recur (rest remaining) (conj kept chunk) new-total))))))

(defn- render-omission [omitted]
  (if (seq omitted)
    (str "\n\n(" (count omitted) " topic" (when (> (count omitted) 1) "s")
         " omitted — too large for the response cap: " (str/join ", " omitted) ")")
    ""))

;; endregion ^^^^^ Size cap ^^^^^

;; region ----- Table of contents -----

(defn- toc-chunks [entries]
  (mapcat (fn [{:keys [module-id sections]}]
            (cons {:name module-id :text (str "## " module-id "\n- " module-id ": (whole chapter)\n")}
                  (map (fn [{:keys [slug title]}]
                         {:name (str module-id "#" slug) :text (str "- " module-id "#" slug ": " title "\n")})
                       sections)))
          entries))

(defn- render-toc [entries max-chars]
  (let [{:keys [kept omitted]} (apply-cap (toc-chunks entries) max-chars)]
    (str "# Isaac Handbook\n\n"
         "Ask for a chapter by module id, or a section with <module-id>#<slug>.\n\n"
         (str/join "" (map :text kept))
         (render-omission omitted))))

;; endregion ^^^^^ Table of contents ^^^^^

;; region ----- Topics -----

(defn- unknown-topic-chunk [topic]
  {:name topic
   :text (str topic ": unknown topic. Call handbook__read with no topics to see what's "
              "available.\n")})

(defn- render-requested [by-id requested max-chars]
  (let [chunks (mapv (fn [topic]
                       (if-let [{:keys [content]} (get by-id topic)]
                         {:name topic :text content}
                         (unknown-topic-chunk topic)))
                     requested)
        {:keys [kept omitted]} (apply-cap chunks max-chars)]
    (str (str/join "\n\n" (map :text kept)) (render-omission omitted))))

;; endregion ^^^^^ Topics ^^^^^

(defn read-topics
  "chapters: [{:module-id <string> :text <chapter markdown>} ...] already
   ordered (foundation's chapter first, then sorted by module id) and
   already filtered to modules whose handbook resolves. requested: a seq
   of topic-id strings, or empty/nil for the table of contents. max-chars:
   the response size cap, or nil for the default."
  [chapters requested max-chars]
  (let [entries   (topic-entries chapters)
        by-id     (topics-by-id entries)
        max-chars (or max-chars default-max-chars)]
    (if (seq requested)
      (render-requested by-id requested max-chars)
      (render-toc entries max-chars))))
