package com.glenn.address.domain;

/**
 * Represents a complete address book entry containing personal and address information.
 * Immutable record with fields for entry identification, person details, address, and notes.
 *
 * @param entryId unique identifier for this entry
 * @param person personal information (name, age, gender, marital status)
 * @param address address details (street, city, state, zip, email, phone)
 * @param notes optional notes or comments about this entry
 */
public record Entry(Integer entryId, Person person, Address address, String notes) {
}
