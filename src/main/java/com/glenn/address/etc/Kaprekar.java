package com.glenn.address.etc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Utility class implementing Kaprekar's constant calculation for 4-digit numbers.
 * Kaprekar's constant (6174) is reached by repeatedly arranging digits in descending
 * and ascending order, then subtracting. Provides method to calculate steps to reach this constant.
 */
public class Kaprekar {
    private static final Logger log = LoggerFactory.getLogger(Kaprekar.class);
    private static final Integer KAPREKAR = 6174;

    public int calculate(int startingNbr) {
        int nbrSteps = 0;
        int current = startingNbr;

        // Validate input - must be 4 digits and not all same digits
        if (startingNbr < 0 || startingNbr > 9999) {
            log.warn("Starting number must be between 0 and 9999");
            return -1;
        }

        // Check if all digits are the same (will never reach Kaprekar's constant)
        String numStr = String.format("%04d", startingNbr);
        if (numStr.chars().distinct().count() == 1) {
            log.warn("Starting number has all same digits - will not reach Kaprekar's constant");
            return -1;
        }

        // find kaprekar's constant from the starting number
        // return the number of steps taken to find it.
        while (current != KAPREKAR) {
            current = kaprekarStep(current);
            nbrSteps++;

            // Safety check to prevent infinite loops (should never exceed 7 steps)
            if (nbrSteps > 10) {
                log.error("Exceeded maximum steps - possible infinite loop");
                return -1;
            }
        }

        // when you find it log.info "I found it in x steps"
        log.info("I found it in {} steps", nbrSteps);
        return nbrSteps;
    }

    private int kaprekarStep(int number) {
        // Ensure 4 digits by padding with zeros
        String numStr = String.format("%04d", number);

        // Split into array of characters and sort
        char[] digits = numStr.toCharArray();

        // Sort ascending
        Arrays.sort(digits);
        int ascending = Integer.parseInt(new String(digits));

        // Sort descending (reverse the sorted array)
        StringBuilder descBuilder = new StringBuilder(new String(digits));
        int descending = Integer.parseInt(descBuilder.reverse().toString());

        // Subtract smaller from larger
        return descending - ascending;
    }

    public static void main(String[] args) {
        Kaprekar kaprekar = new Kaprekar();

        // Test with various numbers
        int[] testNumbers = {3524, 2005, 1234, 9998, 1};

        for (int num : testNumbers) {
            log.info("Testing with starting number: {}", num);
            int steps = kaprekar.calculate(num);
            if (steps > 0) {
                log.info("Reached Kaprekar's constant in {} steps\n", steps);
            }
        }
    }
}
