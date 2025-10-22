package com.glenn.address.binary;

/**
 * Utility class for obfuscating and clarifying strings using a multi-character cipher.
 * Implements ROT13 for letters (rotate by 13 positions), ROT5 for digits (rotate by 5 positions),
 * and character swapping for '?' and '=' punctuation.
 * All transformations are self-inverse: applying obfuscate() twice returns the original string.
 * Non-obfuscated characters (spaces, other punctuation) pass through unchanged.
 * Note: This provides only casual obfuscation and is not cryptographically secure.
 */
public class Rot13 {

    /**
     * Obfuscates a string using the ROT13 cipher for letters and ROT5 for digits.
     * Letters are rotated by 13 positions, digits by 5 positions.
     * '?' and '=' characters are swapped with each other.
     * Other non-alphanumeric characters pass through unchanged.
     *
     * @param input The string to obfuscate.
     * @return The obfuscated string.
     */
    public static String obfuscate(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                c = (char) ('a' + (c - 'a' + 13) % 26);
            } else if (c >= 'A' && c <= 'Z') {
                c = (char) ('A' + (c - 'A' + 13) % 26);
            } else if (c >= '0' && c <= '9') {
                c = (char) ('0' + (c - '0' + 5) % 10);
            } else if (c == '?') {
                c = '=';
            } else if (c == '=') {
                c = '?';
            }
            result.append(c);
        }
        return result.toString();
    }

    /**
     * Reverses obfuscation applied by obfuscate() method.
     * Since ROT13, ROT5, and character swaps are all self-inverse operations,
     * calling obfuscate() again on obfuscated text returns the original string.
     *
     * @param input The obfuscated string to clarify.
     * @return The clarified (original) string.
     */
    public static String clarify(String input) {
        return obfuscate(input);
    }
}
