(ns isaac.handbook.tools
  "Crew tool :handbook/read (wire handbook__read)."
  (:require
    [clojure.string :as str]
    [isaac.cli.host :as host]
    [isaac.config.loader :as loader]
    [isaac.handbook.chapters :as chapters]
    [isaac.handbook.render :as render]))

(defn- string-key-map [m]
  (into {} (map (fn [[k v]] [(if (keyword? k) (name k) (str k)) v])) (or m {})))

(defn- requested-topics [args]
  (let [topics (get (string-key-map args) "topics")]
    (cond
      (sequential? topics) (mapv str topics)
      (string? topics)     (if (str/blank? topics) [] [topics])
      :else                [])))

(defn read-topic
  [args]
  (let [cfg          (or (loader/snapshot "handbook__read tool call") {})
        module-index (:module-index cfg)
        context      {:cwd (host/cwd)}
        max-chars    (get-in cfg [:handbook :max-chars])
        chapters     (chapters/ordered-chapters module-index context)]
    {:result (render/read-topics chapters (requested-topics args) max-chars)}))

(defn read-tool-factory [_]
  {:description
   (str "Isaac's operating handbook: read the table of contents (no topics), a whole "
        "module chapter (topic = module id), or one chapter section (topic = "
        "<module-id>#<slug>). Multiple topics come back in the order asked; an "
        "unknown topic is listed as unknown, never fails the call; a large response "
        "is capped, with the rest listed as omitted.")
   :parameters
   {:type       "object"
    :properties {"topics" {:type        "array"
                           :items       {:type "string"}
                           :description "Topic ids to read: a module id for a whole chapter, or <module-id>#<slug> for one section. Omit for the table of contents."}}}
   :handler #'read-topic})
