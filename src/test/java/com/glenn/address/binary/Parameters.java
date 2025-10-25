package com.glenn.address.binary;

public record Parameters(BinaryService tester, String name, String extension) {

    public String outputFile(String filePrefix) {
        return filePrefix + "." + extension;
    }

    public String inputFile(String filePrefix) {
        return filePrefix + "." + extension;
    }
}
