(ns isaac.handbook.cli
  "Operator CLI for the same module chapters and topic index as handbook__read."
  (:require
    [isaac.foundation.cli.api :as api]
    [isaac.foundation.cli.host :as host]
    [isaac.foundation.config.loader :as loader]
    [isaac.handbook.chapters :as chapters]
    [isaac.handbook.render :as render]))

(defn run-command! [cfg topics]
  (let [cfg      (or cfg (loader/snapshot "embedded handbook CLI invocation") {})
        chapters (chapters/ordered-chapters (:module-index cfg) {:cwd (host/cwd)})
        known?   (render/known-topics chapters)
        output   (render/read-topics chapters topics Long/MAX_VALUE)]
    (println output)
    (if (every? known? topics) 0 1)))

(defmethod api/run :handbook [_id {:keys [_raw-args config]}]
  (run-command! config _raw-args))
