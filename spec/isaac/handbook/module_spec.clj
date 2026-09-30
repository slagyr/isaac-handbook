(ns isaac.handbook.module-spec
  (:require
    [speclj.core :refer :all]
    [isaac.module.protocol :as module]
    [isaac.handbook.module :as handbook-module]))

(describe "isaac.handbook.module"
  (it "create-module returns a valid Module instance"
    (should (module/module? (handbook-module/create-module)))))
