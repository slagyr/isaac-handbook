Feature: isaac handbook — operator man pages for the POH (isaac-y2dn)
  isaac help stays command usage (flags, subcommands). isaac handbook
  prints the same topic index as handbook__read: table of contents with
  no args, a chapter by module id, a section by <module-id>#<slug>.
  Markdown to stdout. No size cap. Unknown topic: print the rest, exit 1.
  A module with no :handbook contributes nothing.

  Background:
    Given default Grover setup
    And the isaac file "/tmp/modules/marigold.charts/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.charts
       :version     "0.1.0"
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
    And the isaac file "/tmp/modules/marigold.bridge/resources/isaac-manifest.edn" exists with:
      """
      {:id          :marigold.bridge
       :version     "1.0.0"
       :description "The ship's bridge: where channels are declared."}
      """
    And config file "isaac.edn" containing:
      """
      {:modules {:marigold.charts {:local/root "/tmp/modules/marigold.charts"}
                 :marigold.bridge {:local/root "/tmp/modules/marigold.bridge"}}}
      """

  Scenario: no args prints the TOC of installed chapters; a module with no handbook is absent (isaac-y2dn)
    When isaac is run with "handbook"
    Then the stdout contains "marigold.charts"
    And the stdout contains "Plotting a course"
    And the stdout contains "Reading the sensors"
    And the stdout does not contain "marigold.bridge"
    And the exit code is 0

  Scenario: a module-id argument prints that chapter (isaac-y2dn)
    When isaac is run with "handbook marigold.charts"
    Then the stdout contains "# marigold.charts"
    And the stdout contains "Set the destination beacon before requesting a plot"
    And the stdout contains "Sensor sweep returns empty"
    And the stdout does not contain "marigold.bridge"
    And the exit code is 0

  Scenario: a module#slug argument prints that section (isaac-y2dn)
    When isaac is run with "handbook marigold.charts#plotting-a-course"
    Then the stdout contains "marigold.charts#plotting-a-course"
    And the stdout contains "Set the destination beacon before requesting a plot"
    And the stdout contains "No confirmed fix on record"
    And the stdout does not contain "Reading the sensors"
    And the exit code is 0

  Scenario: several topic ids print in order; an unknown one is listed unknown, known ones still print, exit 1 (isaac-y2dn)
    When isaac is run with "handbook marigold.charts#reading-the-sensors space-whales marigold.charts#plotting-a-course"
    Then the stdout lines contain in order:
      | Sensor sweep returns empty                     |
      | space-whales                                   |
      | Set the destination beacon before requesting a plot |
    And the stdout contains "unknown"
    And the exit code is 1

  Scenario: a chapter larger than handbook.max-chars still prints in full (isaac-y2dn)
    Given the isaac config path "handbook.max-chars" is "60"
    When isaac is run with "handbook marigold.charts"
    Then the stdout contains "Set the destination beacon before requesting a plot"
    And the stdout contains "Sensor sweep returns empty"
    And the stdout does not contain "omitted"
    And the exit code is 0
