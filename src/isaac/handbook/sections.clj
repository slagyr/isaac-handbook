(ns isaac.handbook.sections
  "Splits a handbook chapter's raw markdown into ordered, top-level (##)
   concept sections. Chapters are free-form — there is no fixed heading
   set, and a nested `### Troubleshooting` subsection is a convention, not
   a requirement; whatever nested subsections a section has ride along
   with it as part of its :body."
  (:require
    [clojure.string :as str]))

(defn slugify
  "lowercase, spaces -> hyphens, punctuation stripped."
  [title]
  (-> (or title "")
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      str/trim
      (str/replace #"\s+" "-")))

(defn- heading-title
  "The heading text of a top-level (exactly `## `) markdown heading line,
   or nil. A third `#` (`### Troubleshooting`) is a nested subsection, not
   a top-level one, and does not match."
  [line]
  (second (re-matches #"^##\s+(.+?)\s*$" (or line ""))))

(defn sections
  "Ordered [{:title :slug :body} ...] — one per top-level (##) heading in
   `markdown`. :body is the section's full raw markdown, heading line
   through (but not including) the next top-level heading, so nested
   subsections stay attached to their section."
  [markdown]
  (let [lines (str/split-lines (or markdown ""))
        idxs  (vec (keep-indexed (fn [i line] (when (heading-title line) i)) lines))]
    (vec
      (map-indexed
        (fn [n start]
          (let [end   (get idxs (inc n) (count lines))
                chunk (subvec (vec lines) start end)]
            {:title (heading-title (first chunk))
             :slug  (slugify (heading-title (first chunk)))
             :body  (str/join "\n" chunk)}))
        idxs))))
