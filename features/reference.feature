Feature: handbook__read — generated reference (inventory + config) (isaac-niqx)
  Reference topics are generated from the LIVE instance, not from any
  module's chapter markdown. Inventory entries are one per installed
  module ("module:<id>"), crew ("crew:<id>"), comm ("comm:<id>"), cron
  job ("cron:<id>"), and hail band ("hail-band:<id>"). Config entries are
  one per config path ("config:<dotted.path>"): type, default, required,
  description, options, and the CURRENT EFFECTIVE value — secrets shown
  only as set/not set, never the value. Every entry names its source
  (module id or config path).

  Draft: depends on isaac-dnib (schema defaults, effective config). Not
  baselined until dnib lands; assertions follow its real output format.

  Background:
    Given default Grover setup
    And the isaac EDN file "config/crew/cordelia.edn" exists with:
      | path           | value            |
      | model          | echo             |
      | soul           | You are Cordelia |
      | context-mode   | episodes         |
      | observers      | [:episodes]      |
    And the isaac EDN file "config/crew/oscar.edn" exists with:
      | path  | value        |
      | model | echo         |
      | soul  | You are Oscar |
    And the crew "cordelia" allows tools: handbook/read,recall/search
    And the crew "oscar" allows tools: fs/read
    And the isaac file "/tmp/modules/marigold.bridge/deps.edn" exists with:
      """
      {:paths ["src" "resources"]}
      """
    And the isaac file "/tmp/modules/marigold.bridge/src/marigold/bridge.clj" exists with:
      """
      (ns marigold.bridge)
      (defn create-module [_opts] {})
      """
    And the isaac file "/tmp/modules/marigold.bridge/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.bridge
       :version     "1.0.0"
       :factory     marigold.bridge/create-module
       :description "The ship's bridge: where channels are declared."
       :berths      {:marigold.bridge/comm
                     {:description "Comm channels."
                      :schema      {:type       :map
                                    :key-spec   {:type :keyword}
                                    :value-spec {:type :map :schema {:label {:type :string}}}}}}}
      """
    And the isaac file "/tmp/modules/marigold.longwave/deps.edn" exists with:
      """
      {:paths ["src" "resources"]}
      """
    And the isaac file "/tmp/modules/marigold.longwave/src/marigold/longwave.clj" exists with:
      """
      (ns marigold.longwave)
      (defn create-module [_opts] {})
      """
    And the isaac file "/tmp/modules/marigold.longwave/resources/isaac-manifest.edn" exists with:
      """
      {:id                   :marigold.longwave
       :version              "0.1.0"
       :factory              marigold.longwave/create-module
       :description          "Long-wave radio for the far reaches."
       :marigold.bridge/comm {:longwave {:label "long-wave radio"}}}
      """
    And config file "isaac.edn" containing:
      """
      {:defaults {:frequencies {:crew "cordelia"}}
       :modules  {:marigold.bridge   {:local/root "/tmp/modules/marigold.bridge"}
                  :marigold.longwave {:local/root "/tmp/modules/marigold.longwave"}}}
      """

  @wip
  Scenario: an inventory entry lists a crew's model, granted tools, and context mode
    Given the following model responses are queued:
      | type     | tool_call      | arguments                       | content   | model |
      | toolCall | handbook__read | {"topics":["crew:cordelia"]}    |           | echo  |
      | text     |                |                                  | There.    | echo  |
    When isaac is run with "prompt -m 'What does cordelia have?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                        |
      | message | assistant    | #"(?s)handbook__read"                  |
      | message | toolResult   | #"(?s)crew:cordelia"                   |
      | message | toolResult   | #"(?s)echo"                            |
      | message | toolResult   | #"(?s)handbook__read"                  |
      | message | toolResult   | #"(?s)recall__search"                  |
      | message | toolResult   | #"(?s)episodes"                        |
      | message | assistant    | There.                                  |

  @wip
  Scenario: an inventory entry lists a module's contributions by berth, naming the module
    Given the following model responses are queued:
      | type     | tool_call      | arguments                              | content | model |
      | toolCall | handbook__read | {"topics":["module:marigold.longwave"]} |        | echo  |
      | text     |                |                                         | There.  | echo  |
    When isaac is run with "prompt -m 'What does long-wave radio contribute?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                             |
      | message | assistant    | #"(?s)handbook__read"                       |
      | message | toolResult   | #"(?s)module:marigold\.longwave"            |
      | message | toolResult   | #"(?s)marigold\.bridge/comm.*longwave"      |
      | message | assistant    | There.                                       |

  @wip
  Scenario: a config entry shows its type, default, required flag, description, and current effective value
    Given the isaac config path "logging.level" is "warn"
    And the following model responses are queued:
      | type     | tool_call      | arguments                       | content | model |
      | toolCall | handbook__read | {"topics":["config:logging.level"]} |     | echo  |
      | text     |                |                                  | There.  | echo  |
    When isaac is run with "prompt -m 'What is the log level set to?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                     |
      | message | assistant    | #"(?s)handbook__read"               |
      | message | toolResult   | #"(?s)config:logging\.level"        |
      | message | toolResult   | #"(?s)type.*keyword"                |
      | message | toolResult   | #"(?s)default.*debug"               |
      | message | toolResult   | #"(?s)effective.*warn"              |
      | message | assistant    | There.                               |

  @wip
  Scenario: a secret config entry shows set/not-set, never the value
    Given the env var "GOOGLE_CLIENT_SECRET" is set to "sh-h-h-not-for-the-handbook"
    And the isaac config path "google.tonotop.oauth.client-secret" is "${GOOGLE_CLIENT_SECRET}"
    And the following model responses are queued:
      | type     | tool_call      | arguments                                             | content | model |
      | toolCall | handbook__read | {"topics":["config:google.tonotop.oauth.client-secret"]} |      | echo  |
      | text     |                |                                                        | There.  | echo  |
    When isaac is run with "prompt -m 'Is the Google secret set?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                    |
      | message | assistant    | #"(?s)handbook__read"                              |
      | message | toolResult   | #"(?s)config:google\.tonotop\.oauth\.client-secret" |
      | message | toolResult   | #"(?s)set"                                          |
      | message | assistant    | There.                                              |
    And session "bistro-chat" has transcript not matching:
      | type       | message.content              |
      | toolResult | #"(?s)sh-h-h-not-for-the-handbook" |

  @wip
  Scenario: inventory groups a request for the whole roster into one topic
    Given the following model responses are queued:
      | type     | tool_call      | arguments                | content | model |
      | toolCall | handbook__read | {"topics":["crews"]}     |         | echo  |
      | text     |                |                           | There.  | echo  |
    When isaac is run with "prompt -m 'List every crew' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content         |
      | message | assistant    | #"(?s)handbook__read"   |
      | message | toolResult   | #"(?s)crew:cordelia"    |
      | message | toolResult   | #"(?s)crew:oscar"       |
      | message | assistant    | There.                   |
