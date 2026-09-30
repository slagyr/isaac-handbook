(ns isaac.handbook.configure-spec
  (:require
    [speclj.core :refer :all]
    [isaac.config.loader :as loader]
    [isaac.config.mutate :as mutate]
    [isaac.handbook.configure :as configure]
    [isaac.logger :as log]))

(defn- stub-load [config]
  (fn [_] {:config config :errors [] :warnings []}))

(describe "isaac.handbook.configure"

  (describe "configure-tool-factory"
    (it "describes handbook__configure and wires the handler + set/unset params"
      (let [spec (configure/configure-tool-factory {})]
        (should-contain "atomically" (:description spec))
        (should= true (:builtin? spec))
        (should= #'configure/configure (:handler spec))
        (should= "object" (get-in spec [:parameters :properties "set" :type]))
        (should= "array" (get-in spec [:parameters :properties "unset" :type]))
        (should= "string" (get-in spec [:parameters :properties "unset" :items :type])))))

  (describe "configure"
    (around [it] (with-redefs [log/log* (fn [& _] nil)] (it)))

    (it "refuses when neither set nor unset is given"
      (with-redefs [loader/load-config-result (stub-load {})]
        (let [result (configure/configure {})]
          (should= true (:isError result))
          (should-contain "refused" (:error result)))))

    (it "applies a successful atomic batch and names the touched file per pair"
      (with-redefs [loader/load-config-result (stub-load {})
                    loader/root               (fn [] "/tmp/root")
                    mutate/set-many!          (fn [root ops]
                                                (should= "/tmp/root" root)
                                                (should= [{:op :set :path "crew.marvin.model" :value "echo"}]
                                                        ops)
                                                {:status :ok :files ["crew/marvin.edn"] :errors [] :warnings []})]
        (let [result (configure/configure {"set" {"crew.marvin.model" "echo"}})]
          (should-not (:isError result))
          (should-contain "set crew.marvin.model = \"echo\"" (:result result))
          (should-contain "crew/marvin.edn" (:result result)))))

    (it "refuses the whole call and reports every failing pair when set-many! is invalid"
      (with-redefs [loader/load-config-result (stub-load {})
                    loader/root               (fn [] "/tmp/root")
                    mutate/set-many!          (fn [_ _ops]
                                                {:status :invalid :files [] :warnings []
                                                 :errors [{:key "crew.marvin.effort" :value "can't coerce \"a lot\" to int"}]})]
        (let [result (configure/configure {"set" {"crew.marvin.model"  "echo"
                                                    "crew.marvin.effort" "a lot"}})]
          (should= true (:isError result))
          (should-contain "refused" (:error result))
          (should-contain "crew.marvin.effort" (:error result)))))

    (it "refuses a literal write over a path whose current value is a ${VAR} reference"
      (with-redefs [loader/load-config-result (fn [opts]
                                                {:config (if (false? (:substitute-env? opts))
                                                           {:google {:oauth {:client-secret "${GOOGLE_CLIENT_SECRET}"}}}
                                                           {})
                                                 :errors [] :warnings []})
                    mutate/set-many!          (fn [& _] (throw (ex-info "should not write" {})))]
        (let [result (configure/configure {"set" {"google.oauth.client-secret" "leaked-value-123"}})]
          (should= true (:isError result))
          (should-contain "refused" (:error result))
          (should-contain "google.oauth.client-secret" (:error result))
          (should-not-contain "leaked-value-123" (:error result)))))

    (it "allows repointing a ${VAR} secret at a different ${VAR} reference"
      (with-redefs [loader/load-config-result (fn [opts]
                                                {:config (if (false? (:substitute-env? opts))
                                                           {:google {:oauth {:client-secret "${OLD_VAR}"}}}
                                                           {})
                                                 :errors [] :warnings []})
                    loader/root               (fn [] "/tmp/root")
                    mutate/set-many!          (fn [_ ops]
                                                (should= [{:op :set :path "google.oauth.client-secret" :value "${NEW_VAR}"}]
                                                        ops)
                                                {:status :ok :files ["isaac.edn"] :errors [] :warnings []})]
        (let [result (configure/configure {"set" {"google.oauth.client-secret" "${NEW_VAR}"}})]
          (should-not (:isError result)))))

    (it "keywordizes a whole-entity map's keys before handing it to set-many!"
      (with-redefs [loader/load-config-result (stub-load {})
                    loader/root               (fn [] "/tmp/root")
                    mutate/set-many!          (fn [_ ops]
                                                (should= [{:op :set :path "crew.boatswain"
                                                          :value {:model "echo" :soul "Keeps the deck crew in line."}}]
                                                        ops)
                                                {:status :ok :files ["crew/boatswain.edn" "crew/boatswain.md"] :errors [] :warnings []})]
        (let [result (configure/configure {"set" {"crew.boatswain" {"model" "echo" "soul" "Keeps the deck crew in line."}}})]
          (should-not (:isError result))
          (should-contain "crew/boatswain.edn" (:result result))
          (should-contain "crew/boatswain.md" (:result result)))))

    (it "builds an unset op for every requested path"
      (with-redefs [loader/load-config-result (stub-load {})
                    loader/root               (fn [] "/tmp/root")
                    mutate/set-many!          (fn [_ ops]
                                                (should= [{:op :unset :path "crew.marvin.soul"}] ops)
                                                {:status :ok :files ["crew/marvin.edn"] :errors [] :warnings []})]
        (let [result (configure/configure {"unset" ["crew.marvin.soul"]})]
          (should-not (:isError result))
          (should-contain "unset crew.marvin.soul" (:result result)))))))
