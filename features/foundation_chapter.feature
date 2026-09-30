Feature: Foundation's chapter introduces the handbook (isaac-3z1b)
  isaac-foundation ships the handbook's introduction: its manifest names a
  `handbook.md` chapter covering vocabulary, runtime, files, config, modules
  and berths, the scheduler, logs, and the CLI. Foundation's chapter leads
  the table of contents by an explicit rule, even when another module's id
  sorts before `isaac.foundation`.

  Background:
    Given default Grover setup
    And the isaac file "/tmp/modules/atlas.charts/resources/isaac-manifest.edn" exists with:
      """
      {:id          :atlas.charts
       :version     "0.1.0"
       :description "Marigold's star atlas."
       :handbook    "atlas/charts/handbook.md"}
      """
    And the isaac file "/tmp/modules/atlas.charts/resources/atlas/charts/handbook.md" exists with:
      """
      ## Star atlas
      The atlas lists every beacon Marigold has logged.
      """
    And the isaac EDN file "config/crew/cordelia.edn" exists with:
      | path  | value            |
      | model | echo             |
      | soul  | You are Cordelia |
    And the crew "cordelia" allows tools: handbook/read
    And config file "isaac.edn" containing:
      """
      {:defaults {:frequencies {:crew "cordelia"}}
       :modules  {:atlas.charts {:local/root "/tmp/modules/atlas.charts"}}}
      """

  Scenario: the table of contents leads with foundation's chapter
    Given the following model responses are queued:
      | type     | tool_call      | arguments | content          | model |
      | toolCall | handbook__read | {}        |                  | echo  |
      | text     |                |           | Foundation first. | echo  |
    When isaac is run with "prompt -m 'What is in the handbook?' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                  |
      | message | user         | What is in the handbook?                         |
      | message | toolResult   | #"(?s)isaac\.foundation.*Vocabulary.*atlas\.charts" |
      | message | assistant    | Foundation first.                                |

  Scenario: a foundation section can be read by its topic id
    Given the following model responses are queued:
      | type     | tool_call      | arguments                                  | content    | model |
      | toolCall | handbook__read | {"topics":["isaac.foundation#vocabulary"]} |            | echo  |
      | text     |                |                                            | The words. | echo  |
    When isaac is run with "prompt -m 'Teach me the words' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content          |
      | message | user         | Teach me the words       |
      | message | toolResult   | #"(?s)Vocabulary"        |
      | message | toolResult   | #"(?s)Quarters"          |
      | message | toolResult   | #"(?s)Troubleshooting"   |
      | message | assistant    | The words.               |
    And session "bistro-chat" has transcript not matching:
      | type       | message.content      |
      | toolResult | #"(?s)Star atlas"    |
