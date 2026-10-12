(ns isaac.handbook.cli-spec
  (:require
    [isaac.foundation.cli.api :as api]
    [isaac.foundation.config.loader :as loader]
    [isaac.handbook.chapters :as chapters]
    [isaac.handbook.cli]
    [speclj.core :refer :all]))

(def charts [{:module-id "marigold.charts"
              :text "## Plotting a course\nSet the destination beacon.\n\n## Reading the sensors\nSensor sweep returns empty.\n"}])

(describe "isaac handbook command"
  (around [example]
    (with-redefs [loader/snapshot (fn [_] {:module-index {} :handbook {:max-chars 1}})
                  chapters/ordered-chapters (fn [_ _] charts)]
      (example)))

  (it "prints the full table of contents despite the tool's cap"
    (let [exit (atom nil)
          out  (with-out-str (reset! exit (api/run :handbook {:_raw-args []})))]
      (should= 0 @exit)
      (should-contain "Reading the sensors" out)
      (should-not-contain "omitted" out)))

  (it "prints requested topics in order and returns failure for unknown topics"
    (let [exit (atom nil)
          out  (with-out-str (reset! exit (api/run :handbook
                                                 {:_raw-args ["marigold.charts#reading-the-sensors"
                                                              "space-whales" "marigold.charts#plotting-a-course"]})))]
      (should= 1 @exit)
      (should (< (.indexOf out "Sensor sweep returns empty")
                 (.indexOf out "space-whales: unknown")
                 (.indexOf out "Set the destination beacon")))
      (should-not-contain "omitted" out)))

  (it "renders every requested topic when the total exceeds the tool cap"
    (let [large (str "## Plotting a course\n" (apply str (repeat 41000 "x"))
                     "\n\n## Reading the sensors\nSensor sweep returns empty.\n")]
      (with-redefs [chapters/ordered-chapters (fn [_ _] [{:module-id "marigold.charts" :text large}])]
        (let [exit (atom nil)
              out  (with-out-str (reset! exit (api/run :handbook
                                                     {:_raw-args ["marigold.charts#plotting-a-course"
                                                                  "marigold.charts#reading-the-sensors"]})))]
          (should= 0 @exit)
          (should-contain "Sensor sweep returns empty" out)
          (should-not-contain "omitted" out)))))

  (it "returns success for a known chapter and section"
    (doseq [topic ["marigold.charts" "marigold.charts#plotting-a-course"]]
      (let [exit (atom nil)
            out  (with-out-str (reset! exit (api/run :handbook {:_raw-args [topic]})))]
        (should= 0 @exit)
        (should-contain "Set the destination beacon" out)))))
