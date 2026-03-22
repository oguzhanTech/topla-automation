Feature: Deal import validation
  Placeholder BDD scenarios for future end-to-end or contract-style checks.

  Scenario: Valid deal passes basic validation
    Given a normalized deal with required fields
    When validation runs
    Then the result is valid

  Scenario: Missing title fails validation
    Given a normalized deal without a title
    When validation runs
    Then the result is invalid
