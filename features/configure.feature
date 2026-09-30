Feature: handbook__configure — atomic multi-set config writes, prose fields (isaac-lshz)
  handbook__configure is granted to a crew separately from handbook__read
  (crew allows tools: handbook/configure). It writes Isaac config through
  foundation's one write path (the same validate/hot-reload machinery
  `isaac config set`/`unset` use) — never a second writer. A call may carry
  several path/value pairs; they are applied as ONE atomic batch: any
  invalid pair refuses the WHOLE call and nothing is written, even the
  individually-valid pairs. No `--force`. Companion prose fields (a cron
  job's :prompt, a crew's soul) and brand-new entities (a new crew, a new
  cron job) are in scope for v1. The tool never touches `.env` and never
  writes a literal value over a `${VAR}`-referenced secret. Every call is
  logged.

  Where a new entity lands is `config set`'s own placement precedent
  (isaac-c4em): an existing entry is written where it already lives; a new
  entry becomes its own entity file when `:prefer-entity-files` is true,
  otherwise it goes inline in isaac.edn. The Background sets the preference,
  as the live instances do. Batches go through foundation's `set-many!`
  (isaac-cvri).

  Background:
    Given default Grover setup
    And the isaac EDN file "config/crew/cordelia.edn" exists with:
      | path  | value            |
      | model | echo             |
      | soul  | You are Cordelia |
    And the crew "cordelia" allows tools: handbook/configure
    And the isaac EDN file "config/crew/oscar.edn" exists with:
      | path  | value         |
      | model | echo          |
      | soul  | You are Oscar |
    And the crew "oscar" allows tools: handbook/read
    And the isaac EDN file "config/cron/evening-plan.edn" exists with:
      | path   | value                |
      | crew   | cordelia             |
      | expr   | 0 21 * * *           |
      | prompt | Wrap the day's log.  |
    And config file "isaac.edn" containing:
      """
      {:defaults            {:frequencies {:crew "cordelia"}}
       :prefer-entity-files true}
      """

  @wip
  Scenario: a single field set lands in the crew's own entity file
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call            | arguments                             | content        | model |
      | toolCall | handbook__configure  | {"set":{"crew.marvin.model":"echo"}}  |                | echo  |
      | text     |                      |                                        | Done, skipper. | echo  |
    When isaac is run with "prompt -m 'Switch Marvin to the echo model' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                          |
      | message | user         | Switch Marvin to the echo model          |
      | message | assistant    | #"(?s)handbook__configure"               |
      | message | toolResult   | #"(?s)crew\.marvin\.model.*echo"         |
      | message | toolResult   | #"(?s)crew/marvin\.edn"                  |
      | message | assistant    | Done, skipper.                             |
    And the isaac file "config/crew/marvin.edn" EDN contains:
      | path  | value |
      | model | echo  |

  @wip
  Scenario: atomic multi-set writes two fields that are only valid together
    Given the following model responses are queued:
      | type     | tool_call           | arguments                                                                      | content       | model |
      | toolCall | handbook__configure | {"set":{"models.riptide.model":"echo-v1","models.riptide.provider":"grover"}}  |               | echo  |
      | text     |                     |                                                                                 | Riptide's up. | echo  |
    When isaac is run with "prompt -m 'Stand up a model called riptide' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                          |
      | message | assistant    | #"(?s)handbook__configure"               |
      | message | toolResult   | #"(?s)models\.riptide\.model.*echo-v1"   |
      | message | toolResult   | #"(?s)models\.riptide\.provider.*grover" |
      | message | assistant    | Riptide's up.                              |
    And the isaac file "isaac.edn" EDN contains:
      | path                     | value   |
      | models.riptide.model     | echo-v1 |
      | models.riptide.provider  | grover  |

  @wip
  Scenario: an invalid pair in the batch refuses the whole call; nothing is written
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call           | arguments                                                          | content  | model |
      | toolCall | handbook__configure | {"set":{"crew.marvin.model":"echo","crew.marvin.effort":"a lot"}}  |          | echo  |
      | text     |                     |                                                                     | Refused. | echo  |
    When isaac is run with "prompt -m 'Bump Marvin to echo and max effort' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content              |
      | message | assistant    | #"(?s)handbook__configure"   |
      | message | toolResult   | #"(?s)refused"               |
      | message | toolResult   | #"(?s)crew\.marvin\.effort"  |
      | message | assistant    | Refused.                       |
    And the isaac file "config/crew/marvin.edn" EDN contains:
      | path  | value  |
      | model | grover |

  @wip
  Scenario: unset removes a field through the same tool
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value   |
      | model | grover  |
      | soul  | Worry.  |
    And the following model responses are queued:
      | type     | tool_call           | arguments                        | content  | model |
      | toolCall | handbook__configure | {"unset":["crew.marvin.soul"]}   |          | echo  |
      | text     |                     |                                   | Cleared. | echo  |
    When isaac is run with "prompt -m 'Marvin does not need a soul override anymore' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                    |
      | message | assistant    | #"(?s)handbook__configure"         |
      | message | toolResult   | #"(?s)unset.*crew\.marvin\.soul"   |
      | message | assistant    | Cleared.                             |
    And the isaac file "config/crew/marvin.edn" EDN contains:
      | path  | value  |
      | model | grover |
    And the config file "crew/marvin.edn" does not contain "Worry"

  @wip
  Scenario: creating a new cron job follows the sibling-file precedent and splits the prompt into a companion file
    Given the following model responses are queued:
      | type     | tool_call           | arguments                                                                                                                                     | content       | model |
      | toolCall | handbook__configure | {"set":{"cron.hull-watch":{"crew":"cordelia","expr":"0 6 * * *","prompt":"Log the hull integrity reading and flag anything that rattles."}}} |               | echo  |
      | text     |                     |                                                                                                                                                | Watch is set. | echo  |
    When isaac is run with "prompt -m 'Add a dawn hull-check cron job' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content               |
      | message | assistant    | #"(?s)handbook__configure"    |
      | message | toolResult   | #"(?s)cron\.hull-watch"       |
      | message | toolResult   | #"(?s)cron/hull-watch\.edn"   |
      | message | toolResult   | #"(?s)cron/hull-watch\.md"    |
      | message | assistant    | Watch is set.                   |
    And the isaac file "config/cron/hull-watch.edn" EDN contains:
      | path | value      |
      | crew | cordelia   |
      | expr | 0 6 * * *  |
    And the isaac file "config/cron/hull-watch.md" exists
    And the config file "isaac.edn" does not contain "hull-watch"

  @wip
  Scenario: creating a new crew follows the sibling-file precedent, landing with a companion soul
    Given the following model responses are queued:
      | type     | tool_call           | arguments                                                                                                 | content   | model |
      | toolCall | handbook__configure | {"set":{"crew.boatswain":{"model":"echo","soul":"You keep the deck crew in line and the log current."}}} |           | echo  |
      | text     |                     |                                                                                                            | New hire. | echo  |
    When isaac is run with "prompt -m 'Sign on a new crew member, the boatswain' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content              |
      | message | assistant    | #"(?s)handbook__configure"   |
      | message | toolResult   | #"(?s)crew\.boatswain"       |
      | message | toolResult   | #"(?s)crew/boatswain\.edn"   |
      | message | toolResult   | #"(?s)crew/boatswain\.md"    |
      | message | assistant    | New hire.                      |
    And the isaac file "config/crew/boatswain.edn" EDN contains:
      | path  | value |
      | model | echo  |
    And the isaac file "config/crew/boatswain.md" exists

  @wip
  Scenario: handbook__configure is unavailable unless the crew is granted it
    Given the crew "oscar" allows tools: handbook/read
    When the user sends "hello" on session "bistro-chat" as crew "oscar"
    Then the prompt does not have tools:
      | name                |
      | handbook__configure |

  @wip
  Scenario: the response names every file the batch touched, not just the last one
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call           | arguments                                                            | content       | model |
      | toolCall | handbook__configure | {"set":{"crew.marvin.model":"echo","defaults.tools.max-lines":500}} |               | echo  |
      | text     |                     |                                                                       | Logged both.  | echo  |
    When isaac is run with "prompt -m 'Switch Marvin to echo and raise the tool output cap' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                                |
      | message | assistant    | #"(?s)handbook__configure"                     |
      | message | toolResult   | #"(?s)crew\.marvin\.model.*crew/marvin\.edn"   |
      | message | toolResult   | #"(?s)defaults\.tools\.max-lines.*isaac\.edn"  |
      | message | assistant    | Logged both.                                     |

  @wip
  Scenario: every configure call is logged with who, what, and the outcome
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call           | arguments                             | content | model |
      | toolCall | handbook__configure | {"set":{"crew.marvin.model":"echo"}}  |         | echo  |
      | text     |                     |                                        | Done.   | echo  |
    When isaac is run with "prompt -m 'Switch Marvin to echo' --session bistro-chat --crew cordelia"
    Then the log has entries matching:
      | level | event               | crew     | session     | outcome |
      | :info | :handbook/configure | cordelia | bistro-chat | written |

  @wip
  Scenario: writing a literal value over a secret reference is refused
    Given the env var "GOOGLE_CLIENT_SECRET" is set to "sh-h-h-not-for-the-handbook"
    And the isaac config path "google.oauth.client-secret" is "${GOOGLE_CLIENT_SECRET}"
    And the following model responses are queued:
      | type     | tool_call           | arguments                                                  | content  | model |
      | toolCall | handbook__configure | {"set":{"google.oauth.client-secret":"leaked-value-123"}}  |          | echo  |
      | text     |                     |                                                             | Refused. | echo  |
    When isaac is run with "prompt -m 'Set the Google client secret directly' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content                     |
      | message | assistant    | #"(?s)handbook__configure"          |
      | message | toolResult   | #"(?s)refused"                      |
      | message | toolResult   | #"(?s)google\.oauth\.client-secret" |
      | message | assistant    | Refused.                              |
    And session "bistro-chat" has transcript not matching:
      | type       | message.content         |
      | toolResult | #"(?s)leaked-value-123" |
    And the config file "isaac.edn" does not contain "leaked-value-123"

  @wip
  Scenario: an unrecognized config path refuses the whole call
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call           | arguments                                                      | content  | model |
      | toolCall | handbook__configure | {"set":{"crew.marvin.model":"echo","crew.marvin.bogus":"x"}}  |          | echo  |
      | text     |                     |                                                                 | Refused. | echo  |
    When isaac is run with "prompt -m 'Bump Marvin to echo, also set bogus' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content              |
      | message | assistant    | #"(?s)handbook__configure"   |
      | message | toolResult   | #"(?s)refused"               |
      | message | toolResult   | #"(?s)bogus"                 |
      | message | assistant    | Refused.                       |
    And the isaac file "config/crew/marvin.edn" EDN contains:
      | path  | value  |
      | model | grover |

  @wip
  Scenario: unsetting a whole entity path removes its file
    Given the isaac EDN file "config/crew/marvin.edn" exists with:
      | path  | value  |
      | model | grover |
    And the following model responses are queued:
      | type     | tool_call           | arguments                  | content       | model |
      | toolCall | handbook__configure | {"unset":["crew.marvin"]}  |               | echo  |
      | text     |                     |                             | Mustered out. | echo  |
    When isaac is run with "prompt -m 'Muster out Marvin entirely' --session bistro-chat --crew cordelia"
    Then session "bistro-chat" has transcript matching:
      | type    | message.role | message.content              |
      | message | assistant    | #"(?s)handbook__configure"   |
      | message | toolResult   | #"(?s)crew\.marvin"          |
      | message | assistant    | Mustered out.                  |
    And the isaac file "config/crew/marvin.edn" does not exist
