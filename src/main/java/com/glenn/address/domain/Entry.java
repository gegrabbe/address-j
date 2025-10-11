package com.glenn.address.domain;

public record Entry(Integer entryId, Person person, Address address, String notes) {
}
