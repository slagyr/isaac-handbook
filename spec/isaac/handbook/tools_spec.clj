(ns isaac.handbook.tools-spec
  (:require
    [speclj.core :refer :all]
    [isaac.config.loader :as loader]
    [isaac.handbook.tools :as tools]))

(describe "isaac.handbook.tools"

  (describe "read-tool-factory"
    (it "describes handbook__read and wires the handler + topics param"
      (let [spec (tools/read-tool-factory {})]
        (should-contain "handbook" (:description spec))
        (should= #'tools/read-topic (:handler spec))
        (should= "array" (get-in spec [:parameters :properties "topics" :type]))
        (should= "string" (get-in spec [:parameters :properties "topics" :items :type])))))

  (describe "read-topic"
    (it "returns the table of contents when the config has no installed modules"
      (with-redefs [loader/snapshot (fn [_] {:module-index {}})]
        (let [result (tools/read-topic {})]
          (should-contain "Isaac Handbook" (:result result)))))

    (it "respects handbook.max-chars from the live config snapshot"
      (with-redefs [loader/snapshot (fn [_] {:module-index {} :handbook {:max-chars 5}})]
        (let [result (tools/read-topic {})]
          ;; With no chapters, the TOC's fixed header alone is kept (first
          ;; chunk always kept) even though it exceeds a 5-char cap.
          (should-contain "Isaac Handbook" (:result result)))))

    (it "reads a string-keyed topics arg (as arguments arrive off the wire)"
      (with-redefs [loader/snapshot (fn [_] {:module-index {}})]
        (let [result (tools/read-topic {"topics" ["space-whales"]})]
          (should-contain "space-whales" (:result result))
          (should-contain "unknown" (:result result)))))

    (it "reads a keyword-keyed topics arg"
      (with-redefs [loader/snapshot (fn [_] {:module-index {}})]
        (let [result (tools/read-topic {:topics ["space-whales"]})]
          (should-contain "space-whales" (:result result))
          (should-contain "unknown" (:result result)))))

    (it "falls back to an empty config when no snapshot has been set"
      (with-redefs [loader/snapshot (fn [_] nil)]
        (let [result (tools/read-topic {})]
          (should-contain "Isaac Handbook" (:result result)))))))
