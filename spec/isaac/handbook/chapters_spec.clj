(ns isaac.handbook.chapters-spec
  (:require
    [speclj.core :refer :all]
    [isaac.handbook.chapters :as chapters]))

(describe "isaac.handbook.chapters"

  (describe "ordered-module-ids"
    (it "leads with foundation's own chapter, then sorts the rest by module id"
      (let [module-index {:marigold.longwave {} :isaac.foundation {} :marigold.bridge {} :marigold.charts {}}]
        (should= [:isaac.foundation :marigold.bridge :marigold.charts :marigold.longwave]
                 (chapters/ordered-module-ids module-index))))

    (it "still leads with foundation when it is the only module"
      (should= [:isaac.foundation] (chapters/ordered-module-ids {:isaac.foundation {}})))

    (it "sorts alphabetically by module id when foundation is not installed"
      (should= [:marigold.bridge :marigold.charts]
               (chapters/ordered-module-ids {:marigold.charts {} :marigold.bridge {}})))

    (it "returns an empty seq for an empty module-index"
      (should= [] (chapters/ordered-module-ids {})))))
