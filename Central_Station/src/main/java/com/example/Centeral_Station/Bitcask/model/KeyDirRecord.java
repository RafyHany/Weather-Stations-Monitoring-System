package com.example.Centeral_Station.Bitcask.model;

public class KeyDirRecord {
    private String filename;
    private int valueSize;
    private long valuePosition;
    private long timeStamp;

    public KeyDirRecord(String filename, int valueSize, long valuePosition, long timeStamp) {
        this.filename = filename;
        this.valueSize = valueSize;
        this.valuePosition = valuePosition;
        this.timeStamp = timeStamp;
    }

    public int getValueSize() {
        return valueSize;
    }

    public void setValueSize(int valueSize) {
        this.valueSize = valueSize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getValuePosition() {
        return valuePosition;
    }

    public void setValuePosition(long valuePosition) {
        this.valuePosition = valuePosition;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
