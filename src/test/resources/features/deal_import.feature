Feature: Deal import validation
  BDD checks for normalized deals and Topla import readiness (actorKey is chosen at runtime via env, not in the deal body).

  Scenario: Valid deal passes basic validation
    Given a normalized deal with required fields
    When validation runs
    Then the result is valid

  Scenario: Missing title fails validation
    Given a normalized deal without a title
    When validation runs
    Then the result is invalid

  Scenario Outline: Dynamic source and end semantics
    Given a normalized deal for source "<source>" with actor "<actorKey>" and end mode "<end_mode>"
    When validation runs
    Then the result is valid

    Examples:
      | source   | actorKey             | end_mode            |
      | Amazon   | topla_amazon_bot     | fixed               |
      | Trendyol | topla_trendyol_bot   | unknown_far_future  |

  Scenario Outline: Batch count with unique keys (no duplicate imports in a run)
    Given <count> distinct normalized deals for source "<source>" with actor "<actorKey>" and end mode "<end_mode>"
    When validation runs on each prepared deal
    Then all prepared deals are valid
    And prepared deals have unique import keys

    Examples:
      | count | source   | actorKey           | end_mode           |
      | 2     | Amazon   | topla_amazon_bot   | fixed              |
      | 5     | Amazon   | topla_amazon_bot   | fixed              |
      | 3     | Trendyol | topla_trendyol_bot | unknown_far_future |
