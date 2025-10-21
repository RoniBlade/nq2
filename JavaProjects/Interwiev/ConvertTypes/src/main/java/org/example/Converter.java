package org.example;

public class Converter {

    private int toInt;
    private byte toByte;
    private long toLong;

    public int convertFromDoubleToInt(double value) {
        return (int) value;
    }

    public long convertFromDoubleToLong(double value) {
        return (long) value;
    }

    public byte convertFromDoubleToByte(double value) {
        return (byte) value;
    }



}
