(ns isaac.handbook.chapters
  "Resolves the ordered list of handbook chapters for a live module-index —
   foundation's own chapter leads the table of contents by explicit rule
   (isaac-z90t Decisions), every other module follows sorted by module id.
   Reads each module's :handbook classpath resource through foundation's
   published module coordinate helpers (isaac.foundation.module.coords) — the same
   coordinate isaac.foundation.module.discovery/handbook-resolves? already validates
   at config load — rather than re-walking manifest/classpath discovery."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [isaac.foundation.module.berths :as berths]
    [isaac.foundation.module.coords :as coords]))

(defn- module-deps-paths
  "The classpath-relative roots a module's own deps.edn declares
   (:paths), or the foundation default when it has none."
  [dir fs*]
  (or (some-> (coords/read-text-file fs* (str dir "/deps.edn")) edn/read-string :paths)
      ["resources" "src"]))

(defn- read-under-module-root [coord context handbook]
  (when-let [dir (coords/coord-directory coord context)]
    (let [fs* (coords/runtime-fs)]
      (some #(coords/read-text-file fs* (str dir "/" % "/" handbook))
            (module-deps-paths dir fs*)))))

(defn- read-from-classpath [handbook]
  (some-> (io/resource handbook) slurp))

(defn chapter-text
  "The raw markdown for module `id`'s handbook chapter, or nil when the
   module declares no :handbook, or it does not resolve."
  [module-index context id]
  (let [entry    (get module-index id)
        handbook (:handbook (berths/module-report module-index id))]
    (when (and entry handbook)
      (or (read-under-module-root (:coord entry) context handbook)
          (read-from-classpath handbook)))))

(defn ordered-module-ids
  "Foundation's chapter always leads; every other installed module
   follows sorted by module id."
  [module-index]
  (let [ids         (keys module-index)
        foundation? #(= % coords/foundation-module-id)]
    (concat (filter foundation? ids)
            (sort-by coords/id-str (remove foundation? ids)))))

(defn ordered-chapters
  "[{:module-id <string> :text <markdown>} ...] for every module in
   `module-index` whose handbook chapter resolves, in table-of-contents
   order."
  [module-index context]
  (keep (fn [id]
          (when-let [text (chapter-text module-index context id)]
            {:module-id (coords/id-str id) :text text}))
        (ordered-module-ids module-index)))
