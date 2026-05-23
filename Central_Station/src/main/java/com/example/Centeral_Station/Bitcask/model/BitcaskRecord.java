package com.example.Centeral_Station.Bitcask.model;

public class BitcaskRecord {
    private long timeStamp;
    private int keySize;
    private int valueSize;
    private long key;
    private byte[] value;

    public BitcaskRecord(long timeStamp, long key, byte[] value) {
        this.timeStamp = timeStamp;
        this.key = key;
        this.value = value;
        this.keySize = 8;
        this.valueSize = value.length;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public int getValueSize() {
        return valueSize;
    }

    public void setValueSize(int valueSize) {
        this.valueSize = valueSize;
    }

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

}
