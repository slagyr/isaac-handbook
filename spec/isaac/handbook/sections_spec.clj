(ns isaac.handbook.sections-spec
  (:require
    [speclj.core :refer :all]
    [isaac.handbook.sections :as sections]))

(def chart-room-md
  "## Plotting a course
Marigold's nav computer resolves a course from the chart room's last
confirmed fix. Set the destination beacon before requesting a plot.

### Troubleshooting
No confirmed fix on record — take a fresh star sighting before plotting.

## Reading the sensors
The long-range sensor sweep reports drift, debris, and beacon strength
for the plotted course.

### Troubleshooting
Sensor sweep returns empty — the sweep interval hasn't elapsed yet;
wait one cycle.")

(describe "isaac.handbook.sections"

  (describe "slugify"
    (it "lowercases, hyphenates spaces, and strips punctuation"
      (should= "plotting-a-course" (sections/slugify "Plotting a course")))
    (it "handles nil"
      (should= "" (sections/slugify nil))))

  (describe "sections"
    (it "splits on top-level ## headings, one entry per section"
      (should= 2 (count (sections/sections chart-room-md))))

    (it "captures the title and slug of each section"
      (let [[s1 s2] (sections/sections chart-room-md)]
        (should= "Plotting a course" (:title s1))
        (should= "plotting-a-course" (:slug s1))
        (should= "Reading the sensors" (:title s2))
        (should= "reading-the-sensors" (:slug s2))))

    (it "keeps a nested ### Troubleshooting subsection attached to its section, not the next one"
      (let [[s1 s2] (sections/sections chart-room-md)]
        (should-contain "No confirmed fix on record" (:body s1))
        (should-not-contain "Sensor sweep returns empty" (:body s1))
        (should-contain "Sensor sweep returns empty" (:body s2))
        (should-not-contain "No confirmed fix on record" (:body s2))))

    (it "excludes the next section's heading and body from the previous section"
      (let [[s1 _] (sections/sections chart-room-md)]
        (should-not-contain "Reading the sensors" (:body s1))))

    (it "is valid for a chapter with a single section (no Troubleshooting required)"
      (let [secs (sections/sections "## Purpose\nTalk to ships beyond the horizon.")]
        (should= 1 (count secs))
        (should= "Purpose" (:title (first secs)))
        (should-contain "Talk to ships beyond the horizon." (:body (first secs)))))

    (it "returns an empty vector for a chapter with no ## headings"
      (should= [] (sections/sections "just some prose, no headings")))

    (it "returns an empty vector for blank/nil input"
      (should= [] (sections/sections nil))
      (should= [] (sections/sections "")))))
