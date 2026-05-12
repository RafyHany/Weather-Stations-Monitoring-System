package com.example.Centeral_Station.Bitcask.fileHandler;

import com.example.Centeral_Station.Bitcask.clock.SystemClock;
import com.example.Centeral_Station.Bitcask.model.BitcaskRecord;
import com.example.Centeral_Station.Bitcask.model.KeyDirRecord;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

@Component
public class WriterBitcask {
    private final String directoryPathData = "Data/data/";
    private final String directoryPathHint = "Data/hint/";
    private String fileName;
    private FileChannel fileChannelData;
    private FileChannel fileChannelHint ;
    private RandomAccessFile randomAccessData;
    private RandomAccessFile randomAccessHint ;
    private final int MAX_FILE_SIZE = 1024  ; // 1 MB
    public WriterBitcask() throws IOException {
        createNewFile();
    }

    public synchronized KeyDirRecord writeBitcask(BitcaskRecord bitcaskRecord) throws IOException {
        if (bitcaskRecord == null)
            return null;
        int headerSize =   8 // long timeStamp
                + 4 // int keySize
                + 4 // int valueSize
                + 8 ; //long key

        int totalSize =  headerSize  + bitcaskRecord.getValueSize(); //byte[] value

        if (this.fileChannelData.size() + totalSize >  MAX_FILE_SIZE) {
            this.fileChannelData.close();
            this.randomAccessData.close();
            this.fileChannelHint.close();
            this.randomAccessHint.close();
            createNewFile();
        }


        ByteBuffer byteBuffer = ByteBuffer.allocate(totalSize);

        byteBuffer.putLong(bitcaskRecord.getTimeStamp());
        byteBuffer.putInt(bitcaskRecord.getKeySize());
        byteBuffer.putInt(bitcaskRecord.getValueSize());
        byteBuffer.putLong(bitcaskRecord.getKey());
        byteBuffer.put(bitcaskRecord.getValue());
        byteBuffer.flip(); // to change mode from write to read
        long currentPosition = this.fileChannelData.size() + headerSize ;
        this.fileChannelData.position(fileChannelData.size());
        this.fileChannelData.write(byteBuffer);

        System.out.println(fileChannelData.size());
        KeyDirRecord res = new KeyDirRecord(this.fileName, bitcaskRecord.getValueSize(), currentPosition, bitcaskRecord.getTimeStamp());
        writeHint(res, bitcaskRecord.getKey());
        return res ;
    }

    private  void writeHint(KeyDirRecord record , long key) throws IOException {
        if(record == null)
            return;

        long totalSize = 8 // long timeStamp
                + 4 // int keySize
                + 4 // int valueSize
                + 8 // long valuePosition
                + 8 ; //long key

        ByteBuffer byteBuffer = ByteBuffer.allocate((int) totalSize);
        byteBuffer.putLong(record.getTimeStamp());
        byteBuffer.putInt(8); // keySize is always 8 bytes for long
        byteBuffer.putInt(record.getValueSize());
        byteBuffer.putLong(record.getValuePosition());
        byteBuffer.putLong(key);
        byteBuffer.flip();

        this.fileChannelHint.position(fileChannelHint.size());
        this.fileChannelHint .write(byteBuffer);
    }

    private void createNewFile() throws IOException {
        long currentTime = SystemClock.getCurrentTime();
        String filenameData = this.directoryPathData + String.valueOf(currentTime) + ".data";
        String filenameHint = this.directoryPathHint + String.valueOf(currentTime) + ".hint";
        this.fileName = filenameData;
        try {
            // DO NOT use try-with-resources here. We need it to stay open!
            this.randomAccessData = new RandomAccessFile(filenameData, "rw");
            this.fileChannelData = this.randomAccessData.getChannel();

            this.randomAccessHint = new RandomAccessFile(filenameHint, "rw");
            this.fileChannelHint = this.randomAccessHint.getChannel();
        } catch (FileNotFoundException e) {
            Logger.getLogger(WriterBitcask.class.getName()).severe("File not found: " + e.getMessage());
            throw e; // Rethrow so the application knows it failed to start
        }

    }
}
