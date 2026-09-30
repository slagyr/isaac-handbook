(ns isaac.handbook.render-spec
  (:require
    [speclj.core :refer :all]
    [isaac.handbook.render :as render]))

(def charts-chapter
  {:module-id "marigold.charts"
   :text      "## Plotting a course\nMarigold's nav computer resolves a course.\n\n### Troubleshooting\nNo confirmed fix on record.\n\n## Reading the sensors\nThe long-range sensor sweep reports drift.\n\n### Troubleshooting\nSensor sweep returns empty.\n"})

(def longwave-chapter
  {:module-id "marigold.longwave"
   :text      "## Purpose\nTalk to ships beyond the horizon.\n"})

(def commented-chapter
  {:module-id "marigold.bridge"
   :text      "<!--\nLint convention: internal authoring note, not for a crew.\n-->\n\n## Hailing\nOpen a channel to another ship.\n"})

(describe "isaac.handbook.render"

  (describe "table of contents (no topics)"
    (it "lists every chapter's module id and its section titles"
      (let [toc (render/read-topics [charts-chapter] [] nil)]
        (should-contain "marigold.charts" toc)
        (should-contain "Plotting a course" toc)
        (should-contain "Reading the sensors" toc)))

    (it "contributes nothing for a module with no handbook (never in the chapters list)"
      (let [toc (render/read-topics [charts-chapter] [] nil)]
        (should-not-contain "marigold.bridge" toc))))

  (describe "a whole-chapter topic"
    (it "returns the chapter's whole markdown, naming its module"
      (let [out (render/read-topics [charts-chapter] ["marigold.charts"] nil)]
        (should-contain "marigold.charts" out)
        (should-contain "Plotting a course" out)
        (should-contain "Reading the sensors" out)
        (should-contain "Sensor sweep returns empty" out))))

  (describe "a chapter-section topic"
    (it "returns just that section, with its Troubleshooting subsection"
      (let [out (render/read-topics [charts-chapter] ["marigold.charts#plotting-a-course"] nil)]
        (should-contain "marigold.charts" out)
        (should-contain "Plotting a course" out)
        (should-contain "No confirmed fix on record" out)
        (should-not-contain "Reading the sensors" out))))

  (describe "multiple topics"
    (it "come back concatenated in the order requested"
      (let [out (render/read-topics [charts-chapter longwave-chapter]
                                    ["marigold.longwave" "marigold.charts#reading-the-sensors"]
                                    nil)
            i1  (.indexOf out "marigold.longwave")
            i2  (.indexOf out "Talk to ships beyond the horizon.")
            i3  (.indexOf out "marigold.charts")
            i4  (.indexOf out "Reading the sensors")]
        (should (< i1 i2 i3 i4)))))

  (describe "unknown topics"
    (it "are listed as unknown; the call still succeeds"
      (let [out (render/read-topics [charts-chapter]
                                    ["marigold.charts#nonexistent" "space-whales"]
                                    nil)]
        (should-contain "marigold.charts#nonexistent" out)
        (should-contain "space-whales" out)
        (should-contain "unknown" out)
        (should-contain "no topics" out))))

  (describe "the size cap"
    (it "returns what fits and lists the rest as omitted, without failing"
      (let [out (render/read-topics [charts-chapter longwave-chapter] [] 60)]
        (should-contain "omitted" out)))

    (it "never cuts a kept topic mid-body — explicit topics either come back whole or are omitted"
      (let [out (render/read-topics [charts-chapter longwave-chapter]
                                    ["marigold.longwave" "marigold.charts"]
                                    50)]
        (should-contain "Talk to ships beyond the horizon." out)
        (should-not-contain "Plotting a course" out)
        (should-contain "omitted" out)
        (should-contain "marigold.charts" out)))

    (it "always keeps at least the first item, even alone over cap"
      (let [out (render/read-topics [charts-chapter] ["marigold.charts"] 1)]
        (should-contain "Plotting a course" out))))

  (describe "HTML comments in chapter source"
    (it "are stripped from a whole-chapter topic before a crew ever sees them"
      (let [out (render/read-topics [commented-chapter] ["marigold.bridge"] nil)]
        (should-contain "Hailing" out)
        (should-not-contain "Lint convention" out)
        (should-not-contain "<!--" out)))

    (it "are stripped from the table of contents"
      (let [toc (render/read-topics [commented-chapter] [] nil)]
        (should-not-contain "Lint convention" toc)
        (should-not-contain "<!--" toc)))))
