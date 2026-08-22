package com.hollower.utils;

import net.minecraft.core.BlockPos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class WaypointerV8Codec {
    public static final int MAX_WAYPOINTS = 20_000;

    private static final String ALPHABET =
            "!\"#$%&'()*+-/0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "[\\]^_abcdefghijklmnopqrstuvwxyz{|}~";
    private static final int VERSION_AND_ROUTE_FLAGS = 0x48;
    private static final int CRYSTAL_HOLLOWS_ZONE_REF = 25;
    private static final int BODYLESS_SEQUENCE_FLAGS = 0x05;
    private static final int THIRTEEN_BITS = 1 << 13;
    private static final int FOURTEEN_BIT_THRESHOLD =
            ALPHABET.length() * ALPHABET.length() - THIRTEEN_BITS - 1;

    private WaypointerV8Codec() {
    }

    public static String encode(List<BlockPos> positions) {
        if (positions.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException("Waypointer routes support up to " + MAX_WAYPOINTS + " nodes");
        }
        try {
            byte[] body = createBody(positions);
            byte[] compressed = deflate(appendChecksum(body));
            return "WP:" + escapeHypixelEmotes(encodeBase91(compressed));
        } catch (IOException error) {
            throw new IllegalStateException("Waypointer route export failed", error);
        }
    }

    public static List<BlockPos> decode(String encoded) {
        try {
            String unescaped = unescapeHypixelEmotes(encoded);
            byte[] compressed = decodeBase91(unescaped);
            byte[] framed = inflate(compressed);
            byte[] body = verifyChecksum(framed);
            return parseBody(body);
        } catch (IOException | DataFormatException error) {
            throw new IllegalArgumentException("Could not decode Waypointer route", error);
        }
    }

    private static List<BlockPos> parseBody(byte[] body) throws IOException {
        List<BlockPos> positions = new ArrayList<>();
        try (java.io.DataInputStream in =
                     new java.io.DataInputStream(new ByteArrayInputStream(body))) {
            in.readByte();
            readVarint(in);
            in.readByte();
            int count = readVarint(in);

            int previousX = 0;
            int previousY = 0;
            int previousZ = 0;
            for (int index = 0; index < count; index++) {
                int x = previousX + readZigzag(in);
                int y = previousY + readZigzag(in);
                int z = previousZ + readZigzag(in);
                positions.add(new BlockPos(x, y, z));
                previousX = x;
                previousY = y;
                previousZ = z;
            }
        }
        return positions;
    }

    private static byte[] verifyChecksum(byte[] framed) {
        if (framed.length < Integer.BYTES) {
            throw new IllegalArgumentException("Waypointer route data is truncated");
        }
        int bodyLength = framed.length - Integer.BYTES;
        byte[] body = Arrays.copyOf(framed, bodyLength);
        CRC32 checksum = new CRC32();
        checksum.update(body);
        long expected = checksum.getValue();
        long actual = ((long) (framed[bodyLength] & 0xFF) << 24)
                | ((framed[bodyLength + 1] & 0xFF) << 16)
                | ((framed[bodyLength + 2] & 0xFF) << 8)
                | (framed[bodyLength + 3] & 0xFF);
        if (expected != actual) {
            throw new IllegalArgumentException("Waypointer route checksum mismatch");
        }
        return body;
    }

    private static byte[] inflate(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(input);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) break;
                output.write(buffer, 0, count);
            }
        } finally {
            inflater.end();
        }
        return output.toByteArray();
    }

    private static byte[] decodeBase91(String input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(input.length());
        long bitBuffer = 0;
        int bitCount = 0;
        int value = -1;
        for (int index = 0; index < input.length(); index++) {
            int digit = ALPHABET.indexOf(input.charAt(index));
            if (digit == -1) continue;
            if (value < 0) {
                value = digit;
                continue;
            }
            value += digit * ALPHABET.length();
            bitBuffer |= (long) value << bitCount;
            bitCount += (value & (THIRTEEN_BITS - 1)) > FOURTEEN_BIT_THRESHOLD ? 13 : 14;
            do {
                output.write((int) (bitBuffer & 0xFF));
                bitBuffer >>>= 8;
                bitCount -= 8;
            } while (bitCount > 7);
            value = -1;
        }
        if (value >= 0) {
            output.write((int) ((bitBuffer | (long) value << bitCount) & 0xFF));
        }
        return output.toByteArray();
    }

    private static String unescapeHypixelEmotes(String input) {
        StringBuilder output = new StringBuilder(input.length());
        int index = 0;
        while (index < input.length()) {
            char current = input.charAt(index);
            output.append(current);
            if (current == '~' && index + 1 < input.length() && input.charAt(index + 1) == '~') {
                index += 2;
                continue;
            }
            if ((current == '<' || current == 'o')
                    && index + 2 < input.length()
                    && input.charAt(index + 1) == '~'
                    && ((current == '<' && input.charAt(index + 2) == '3')
                    || (current == 'o' && input.charAt(index + 2) == '/'))) {
                index += 2;
                continue;
            }
            index++;
        }
        return output.toString();
    }

    private static int readVarint(java.io.DataInputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int next = in.readUnsignedByte();
            result |= (next & 0x7F) << shift;
            if ((next & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }

    private static int readZigzag(java.io.DataInputStream in) throws IOException {
        int n = readVarint(in);
        return (n >>> 1) ^ -(n & 1);
    }

    private static byte[] createBody(List<BlockPos> positions) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(VERSION_AND_ROUTE_FLAGS);
            writeVarint(out, CRYSTAL_HOLLOWS_ZONE_REF);
            out.writeByte(BODYLESS_SEQUENCE_FLAGS);
            writeVarint(out, positions.size());

            int previousX = 0;
            int previousY = 0;
            int previousZ = 0;
            for (int index = 0; index < positions.size(); index++) {
                BlockPos pos = positions.get(index);
                int x = pos.getX();
                int y = pos.getY();
                int z = pos.getZ();
                writeZigzag(out, index == 0 ? x : x - previousX);
                writeZigzag(out, index == 0 ? y : y - previousY);
                writeZigzag(out, index == 0 ? z : z - previousZ);
                previousX = x;
                previousY = y;
                previousZ = z;
            }
        }
        return bytes.toByteArray();
    }

    private static byte[] appendChecksum(byte[] body) {
        CRC32 checksum = new CRC32();
        checksum.update(body);
        long value = checksum.getValue();
        byte[] framed = Arrays.copyOf(body, body.length + Integer.BYTES);
        int offset = body.length;
        framed[offset] = (byte) (value >>> 24);
        framed[offset + 1] = (byte) (value >>> 16);
        framed[offset + 2] = (byte) (value >>> 8);
        framed[offset + 3] = (byte) value;
        return framed;
    }

    private static byte[] deflate(byte[] input) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        deflater.setInput(input);
        deflater.finish();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        try {
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                output.write(buffer, 0, count);
            }
        } finally {
            deflater.end();
        }
        return output.toByteArray();
    }

    private static String encodeBase91(byte[] input) {
        StringBuilder output = new StringBuilder((input.length * 123 + 99) / 100);
        long bitBuffer = 0;
        int bitCount = 0;
        for (byte value : input) {
            bitBuffer |= (long) (value & 0xFF) << bitCount;
            bitCount += 8;
            if (bitCount <= 13) continue;

            int encoded = (int) (bitBuffer & (THIRTEEN_BITS - 1));
            if (encoded > FOURTEEN_BIT_THRESHOLD) {
                bitBuffer >>>= 13;
                bitCount -= 13;
            } else {
                encoded = (int) (bitBuffer & ((1 << 14) - 1));
                bitBuffer >>>= 14;
                bitCount -= 14;
            }
            output.append(ALPHABET.charAt(encoded % ALPHABET.length()));
            output.append(ALPHABET.charAt(encoded / ALPHABET.length()));
        }

        if (bitCount > 0) {
            output.append(ALPHABET.charAt((int) (bitBuffer % ALPHABET.length())));
            if (bitCount > 7 || bitBuffer >= ALPHABET.length()) {
                output.append(ALPHABET.charAt((int) (bitBuffer / ALPHABET.length())));
            }
        }
        return output.toString();
    }

    private static String escapeHypixelEmotes(String input) {
        StringBuilder output = new StringBuilder(input.length() + 4);
        for (int index = 0; index < input.length(); index++) {
            char current = input.charAt(index);
            output.append(current);
            if (current == '~'
                    || index + 1 < input.length()
                    && ((current == '<' && input.charAt(index + 1) == '3')
                    || (current == 'o' && input.charAt(index + 1) == '/'))) {
                output.append('~');
            }
        }
        return output.toString();
    }

    private static void writeZigzag(DataOutputStream out, int value) throws IOException {
        writeVarint(out, (value << 1) ^ (value >> 31));
    }

    private static void writeVarint(DataOutputStream out, int value) throws IOException {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte(remaining & 0x7F);
    }
}
