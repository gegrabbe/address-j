package com.glenn.address.domain;

/**
 * Represents personal information for an address book entry.
 * Immutable record containing name, age, and demographic information.
 *
 * @param firstName person's first name
 * @param lastName person's last name
 * @param age person's age in years
 * @param gender person's gender (MALE, FEMALE, OTHER)
 * @param maritalStatus person's marital status (MARRIED, SINGLE, WIDOWED, DIVORCED, OTHER)
 */
public record Person(String firstName, String lastName, Integer age, Gender gender, MaritalStatus maritalStatus) {
}
