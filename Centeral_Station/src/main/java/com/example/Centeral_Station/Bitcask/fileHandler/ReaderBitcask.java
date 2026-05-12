package com.example.Centeral_Station.Bitcask.fileHandler;

import com.example.Centeral_Station.Bitcask.model.BitcaskRecord;
import com.example.Centeral_Station.Bitcask.model.KeyDirRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
}