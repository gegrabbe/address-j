package com.glenn.address.binary;

public record Parameters(BinaryService tester, String name, String extension) {
    private static final String OUTPUT_FILE = "test-binary-output";
    private static final String INPUT_FILE = "test-binary-input";

    public String outputFile() {
        return OUTPUT_FILE + "." + extension;
    }

    public String inputFile() {
        return INPUT_FILE + "." + extension;
    }
}
