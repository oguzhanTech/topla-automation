Feature: Amazon hub batch size (specification)
  Jar does not read this file; keep Examples in sync with AMAZON_DEALS_TARGET_COUNT in .env when using AMAZON_MODE=hub.

  Scenario Outline: Hub target count examples
    Given a hub batch size of <count>
    Then the hub batch size is accepted

    Examples:
      | count |
      | 3     |
      | 5     |
      | 10    |
