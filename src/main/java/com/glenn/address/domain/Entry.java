package com.glenn.address.domain;

public record Entry(String entryId, Person person, Address address, String notes) {
}
