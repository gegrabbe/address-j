package com.glenn.address.web;

/**
 * Represents an error response returned by the REST API.
 * Contains error type and human-readable error message.
 *
 * @param error error type or category
 * @param message detailed error message
 */
public record ErrorResponse(String error, String message) {
}
