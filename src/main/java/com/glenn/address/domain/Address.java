package com.glenn.address.domain;

/**
 * Represents address information for an address book entry.
 * Immutable record containing physical address, contact, and communication details.
 *
 * @param street street address
 * @param city city name
 * @param state state or province abbreviation
 * @param zip postal code or ZIP code
 * @param email email address for contact
 * @param phone phone number for contact
 */
public record Address(String street, String city, String state, String zip, String email, String phone) {
}
