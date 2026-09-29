Feature: handbook__read — Isaac's operating handbook (isaac-z90t)
  handbook__read is granted to a crew like any other tool (crew allows
  tools: handbook/read). Called with no topics, it returns a table of
  contents: a short instance overview plus every available topic — one
  per installed module's handbook chapter, plus one per concept section
  inside a chapter (addressed as "<module-id>#<section-slug>"). Called
  with topics, it returns those items, in the order asked, as markdown,
  each naming its source module. An unknown topic never fails the call —
  it's listed as unknown alongside where to find what IS available. A
  total response size cap returns what fits and lists the rest as
  omitted. v1 is chapters only (config/inventory reference topics land
  in a later bean).

  Background:
    Given default Grover setup
    And the isaac file "/tmp/modules/marigold.charts/deps.edn" exists with:
      """
      {:paths ["src" "resources"]}
      """
    And the isaac file "/tmp/modules/marigold.charts/src/marigold/charts.clj" exists with:
      """
      (ns marigold.charts)
      (defn create-module [_opts] {})
      """
    And the isaac file "/tmp/modules/marigold.charts/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.charts
       :version     "0.1.0"
       :factory     marigold.charts/create-module
       :description "The chart room: navigation references for the reaches Marigold has already mapped."
       :handbook    "marigold/charts/handbook.md"}
      """
    And the isaac file "/tmp/modules/marigold.charts/resources/marigold/charts/handbook.md" exists with:
      """
      ## Plotting a course
      Marigold's nav computer resolves a course from the chart room's last
      confirmed fix. Set the destination beacon before requesting a plot.

      ### Troubleshooting
      No confirmed fix on record — take a fresh star sighting before plotting.

      ## Reading the sensors
      The long-range sensor sweep reports drift, debris, and beacon strength
      for the plotted course.

      ### Troubleshooting
      Sensor sweep returns empty — the sweep interval hasn't elapsed yet;
      wait one cycle.
      """
    And the isaac file "/tmp/modules/marigold.bridge/deps.edn" exists with:
      """
      {:paths ["resources"]}
      """
    And the isaac file "/tmp/modules/marigold.bridge/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.bridge
       :version     "1.0.0"
       :factory     marigold.bridge/create-module
       :description "The ship's bridge: where channels are declared."}
      """
    And the isaac EDN file "config/crew/cordelia.edn" exists with:
      | path  | value            |
      | model | echo             |
      | soul  | You are Cordelia |
    And the crew "cordelia" allows tools: handbook/read
    And config file "isaac.edn" containing:
      """
      {:defaults {:frequencies {:crew "cordelia"}}
       :modules  {:marigold.charts {:local/root "/tmp/modules/marigold.charts"}
                  :marigold.bridge {:local/root "/tmp/modules/marigold.bridge"}}}
      """

  @wip
  Scenario: no topics returns a table of contents; a module with no handbook contributes nothing
    Given the following model responses are queued:
      | type     | tool_call     | arguments | content                    | model |
      | toolCall | handbook__read | {}       |                            | echo  |
      | text     |               |           | Here's what's aboard.      | echo  |
    When isaac is run with "prompt -m 'What can the handbook tell me?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                             |
      | message | user         | What can the handbook tell me?                              |
      | message | assistant    | #"(?s)handbook__read"                                       |
      | message | toolResult   | #"(?s)marigold\.charts.*Plotting a course"                  |
      | message | toolResult   | #"(?s)marigold\.charts.*Reading the sensors"                |
      | message | assistant    | Here's what's aboard.                                       |
    And session "bistro-chat" has transcript not matching:
      | type       | message.content        |
      | toolResult | #"(?s)marigold\.bridge" |

  @wip
  Scenario: a whole-chapter topic returns its markdown, naming its module
    Given the following model responses are queued:
      | type     | tool_call      | arguments                          | content       | model |
      | toolCall | handbook__read | {"topics":["marigold.charts"]}     |               | echo  |
      | text     |                |                                     | Got the chart room. | echo |
    When isaac is run with "prompt -m 'Read the chart room chapter' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                             |
      | message | user         | Read the chart room chapter                 |
      | message | assistant    | #"(?s)handbook__read"                       |
      | message | toolResult   | #"(?s)marigold\.charts"                     |
      | message | toolResult   | #"(?s)Plotting a course"                    |
      | message | toolResult   | #"(?s)Reading the sensors"                  |
      | message | toolResult   | #"(?s)Sensor sweep returns empty"           |
      | message | assistant    | Got the chart room.                         |

  @wip
  Scenario: a chapter-section topic returns just that section, with its Troubleshooting
    Given the following model responses are queued:
      | type     | tool_call      | arguments                                                  | content    | model |
      | toolCall | handbook__read | {"topics":["marigold.charts#plotting-a-course"]}           |            | echo  |
      | text     |                |                                                             | Plotted.   | echo  |
    When isaac is run with "prompt -m 'Just the plotting section' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                        |
      | message | user         | Just the plotting section              |
      | message | assistant    | #"(?s)handbook__read"                  |
      | message | toolResult   | #"(?s)marigold\.charts"                |
      | message | toolResult   | #"(?s)Plotting a course"               |
      | message | toolResult   | #"(?s)No confirmed fix on record"      |
      | message | assistant    | Plotted.                                |
    And session "bistro-chat" has transcript not matching:
      | type       | message.content            |
      | toolResult | #"(?s)Reading the sensors" |

  @wip
  Scenario: multiple topics come back in the order asked
    Given the isaac file "/tmp/modules/marigold.longwave/deps.edn" exists with:
      """
      {:paths ["resources"]}
      """
    And the isaac file "/tmp/modules/marigold.longwave/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.longwave
       :version     "0.1.0"
       :factory     marigold.longwave/create-module
       :description "Long-wave radio for the far reaches."
       :handbook    "marigold/longwave/handbook.md"}
      """
    And the isaac file "/tmp/modules/marigold.longwave/resources/marigold/longwave/handbook.md" exists with:
      """
      ## Purpose
      Talk to ships beyond the horizon.
      """
    When the isaac EDN file "isaac.edn" changes to:
      """
      {:defaults {:frequencies {:crew "cordelia"}}
       :modules  {:marigold.charts   {:local/root "/tmp/modules/marigold.charts"}
                  :marigold.bridge   {:local/root "/tmp/modules/marigold.bridge"}
                  :marigold.longwave {:local/root "/tmp/modules/marigold.longwave"}}}
      """
    And the following model responses are queued:
      | type     | tool_call      | arguments                                                           | content  | model |
      | toolCall | handbook__read | {"topics":["marigold.longwave","marigold.charts#reading-the-sensors"]} |       | echo  |
      | text     |                |                                                                      | Both.    | echo  |
    When isaac is run with "prompt -m 'Longwave first, then the sensors' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                                                          |
      | message | user         | Longwave first, then the sensors                                                         |
      | message | assistant    | #"(?s)handbook__read"                                                                    |
      | message | toolResult   | #"(?s)marigold\.longwave.*Talk to ships beyond the horizon.*marigold\.charts.*Reading the sensors" |
      | message | assistant    | Both.                                                                                     |

  @wip
  Scenario: an unknown topic is listed as unknown; the call still succeeds
    Given the following model responses are queued:
      | type     | tool_call      | arguments                                                        | content        | model |
      | toolCall | handbook__read | {"topics":["marigold.charts#nonexistent","space-whales"]}        |                | echo  |
      | text     |                |                                                                   | No such thing. | echo  |
    When isaac is run with "prompt -m 'Tell me about space whales' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                          |
      | message | user         | Tell me about space whales                                |
      | message | assistant    | #"(?s)handbook__read"                                     |
      | message | toolResult   | #"(?s)marigold\.charts#nonexistent.*unknown"              |
      | message | toolResult   | #"(?s)space-whales.*unknown"                              |
      | message | toolResult   | #"(?s)no topics"                                           |
      | message | assistant    | No such thing.                                             |

  @wip
  Scenario: the response is capped; omitted topics are listed by name
    Given the isaac config path "handbook.max-chars" is "60"
    And the following model responses are queued:
      | type     | tool_call      | arguments                          | content        | model |
      | toolCall | handbook__read | {}                                  |                | echo  |
      | text     |                |                                     | Trimmed a lot. | echo  |
    When isaac is run with "prompt -m 'Give me everything' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content            |
      | message | user         | Give me everything         |
      | message | assistant    | #"(?s)handbook__read"       |
      | message | toolResult   | #"(?s)omitted"              |
      | message | assistant    | Trimmed a lot.              |

  @wip
  Scenario: handbook__read is unavailable unless the crew is granted it
    Given the crew "cordelia" allows tools: fs/read
    When the user sends "hello" on session "bistro-chat" as crew "cordelia"
    Then the prompt does not have tools:
      | name           |
      | handbook__read |
