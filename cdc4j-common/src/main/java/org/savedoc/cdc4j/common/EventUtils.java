package org.savedoc.cdc4j.common;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class EventUtils {
    public static Pair<String, Integer> readString(ByteBuffer byteBuffer, int startFrom) {
        final var sb = new StringBuilder();
        while ((char) byteBuffer.get(startFrom) != '\u0000') {
            sb.append((char) byteBuffer.get(startFrom));
            ++startFrom;
        }
        return new Pair<>(sb.toString(), ++startFrom);
    }

    public static String readFixedBytes(ByteBuffer byteBuffer, int offset, int countToRead) {
        final var sb = new StringBuilder();
        for (int i = 0; i < countToRead; i++) {
            sb.append((char) byteBuffer.get(offset));
            ++offset;
        }
        return sb.toString();
    }

    public static List<TupleData> readTuple(ByteBuffer byteBuffer, int offset) {
        final var tuples = new ArrayList<TupleData>();
        final var numberOfColumns = byteBuffer.getShort(offset);
        offset += Short.BYTES;
        for (short i = 0; i < numberOfColumns; i++) {
            final var tuple = createTuple(byteBuffer, offset);
            tuples.add(tuple.getLeft());
            offset = tuple.getRight();
        }
        return tuples;
    }

    public static Pair<TupleData, Integer> createTuple(ByteBuffer byteBuffer, int offset) {
        final var dataType = (char) byteBuffer.get(offset);
        ++offset;
        final var columnLength = byteBuffer.getInt(offset);
        offset += Integer.BYTES;
        final var columnValue = readFixedBytes(byteBuffer, offset, columnLength);
        offset += columnLength;
        return new Pair<>(new TupleData((short) 0, dataType, columnLength, columnValue), offset);
    }


}
