Feature: Change action

  Scenario: change lines till forwards
    Given file with text
    """
    if (condition) {
    }
    """
    When I type "ct(for"
    Then text should be
    """
    for(condition) {
    }
    """
    And caret should be placed after "for" in insert mode

  Scenario: change lines till backwards
    Given file with text
    """
    if (condition) {<caret>
    }
    """
    When I type "cT("
    Then text should be
    """
    if (
    }
    """
    And caret should be placed after "if (" in insert mode


# sample for outline

  Scenario Outline: change something
    Given file with text
    """
    <text>
    """
    When I type "<keys>"
    Then text should be
    """
    <expected_text>
    """
    And editor should be in <mode> mode
  Examples:
    | text                         | keys | expected_text    | mode   |
    | if (condition) {<caret>\n}\n | cFc  | if (<caret>\n}\n | insert |

