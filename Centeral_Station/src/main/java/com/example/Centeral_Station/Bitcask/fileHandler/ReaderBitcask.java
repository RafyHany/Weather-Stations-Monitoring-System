package com.example.Centeral_Station.Bitcask.fileHandler;

import com.example.Centeral_Station.Bitcask.model.BitcaskRecord;
import com.example.Centeral_Station.Bitcask.model.KeyDirRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReaderBitcask {

    public ReaderBitcask() {
    }

    public BitcaskRecord readBitcask(KeyDirRecord record) throws IOException {
        if (record == null) return null;

        try (RandomAccessFile raf = new RandomAccessFile(record.getFilename(), "r");
             FileChannel channel = raf.getChannel()) {
            int headerSize = 24;
            long recordStartPosition = record.getValuePosition() - headerSize;
            ByteBuffer headerBuffer = ByteBuffer.allocate(headerSize);
            channel.read(headerBuffer, recordStartPosition);
            headerBuffer.flip();

            long timeStamp = headerBuffer.getLong();
            int keySize = headerBuffer.getInt();
            int valueSize = headerBuffer.getInt();
            long key = headerBuffer.getLong();

            ByteBuffer valueBuffer = ByteBuffer.allocate(valueSize);
            channel.read(valueBuffer, record.getValuePosition());
            valueBuffer.flip();

            byte[] valueBytes = new byte[valueSize];
            valueBuffer.get(valueBytes);

            return new BitcaskRecord(timeStamp, key, valueBytes);
        }
    }

    public Map<Long, KeyDirRecord> readHintFiles(Path hintFile) {
        Map<Long, KeyDirRecord> recoveredPointers = new HashMap<>();
        try (RandomAccessFile raf = new RandomAccessFile(hintFile.toFile(), "r");
             FileChannel channel = raf.getChannel()) {
            String dataFile = hintFile.toString().replace("hint", "data");
            ByteBuffer buffer = ByteBuffer.allocate(32);
            while (channel.read(buffer) > 0) {
                buffer.flip();
                if (buffer.remaining() < 32) {
                    break;
                }

                long timeStamp = buffer.getLong();
                int keySize = buffer.getInt();
                int valueSize = buffer.getInt();
                long valuePosition = buffer.getLong();
                long key = buffer.getLong();

                buffer.clear();

                KeyDirRecord record = new KeyDirRecord(dataFile, valueSize, valuePosition, timeStamp);
                recoveredPointers.put(key, record);
            }
            return recoveredPointers;

        } catch (Exception e) {
            System.err.println("Failed to read hint file: " + hintFile.getFileName());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

}