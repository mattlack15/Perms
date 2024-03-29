package me.gravitinos.perms.core.util;

import io.netty.handler.codec.compression.DecompressionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class GravSerializer {
    private byte[] bytes = new byte[16]; //Default 16 byte capacity
    private int used = 0;
    private int reading = 0;
    private int mark = 0;
    private int writeMark = 0;

    public GravSerializer() {
    }

    public GravSerializer(InputStream is) throws IOException {
        int i;
        ensureCapacity(is.available());
        while ((i = is.read()) != -1) {
            writeByte((byte) i);
        }
        is.close();
    }

    public GravSerializer(String input) {
        this(Base64.getDecoder().decode(input));
    }

    public GravSerializer(byte[] input) {
        bytes = Arrays.copyOf(input, input.length);
        used = bytes.length;
    }

    /**
     * Sets the marker to the current reading byte
     * When you do reset() it will reset to the current reading byte
     */
    public void mark() {
        mark = reading;
    }

    /**
     * Marks this write position for a later writeReset
     */
    public void writeMark() {
        writeMark = used;
    }

    /**
     * Resets serializer's writing position to the last writeMark
     */
    public void writeReset() {
        while (this.used != writeMark) {
            this.used = writeMark;
        }
    }

    public void append(byte[] arr) {
        append(arr, arr.length);
    }

    public void append(byte[] arr, int size) {

        if(size > arr.length)
            throw new IllegalArgumentException("Size cannot be larger than array size!");

        ensureCapacity(used + size);
        System.arraycopy(arr, 0, this.bytes, this.used, size);
        this.used += size;
    }

    private void ensureCapacity(int capacity) {
        if (this.bytes.length >= capacity)
            return;

        int oldCapacity = this.bytes.length;
        int newCapacity = Math.max(oldCapacity + (oldCapacity >> 1), capacity); // max(old + (old/2), cap)
        byte[] b = new byte[newCapacity];
        System.arraycopy(this.bytes, 0, b, 0, this.used);
        this.bytes = b;
    }

    /**
     * Resets to the last marker (mark()), by default it is 0
     */
    public void reset() {
        this.reading = mark;
    }

    public int size() {
        return this.used;
    }

    public int trueSize() {
        return this.bytes.length;
    }

    public void writeString(String str) {
        byte[] bts = str.getBytes();
        ensureCapacity(used + bts.length + 4);
        writeInt(bts.length);
        append(bts);
    }

    public void writeByteArray(byte[] bites) {
        int size = bites.length;
        ensureCapacity(used + size + 4);
        writeInt(size);
        append(bites);
    }

    public void writeShort(short value) {
        this.writeByte((byte) (value & 0xFF));
        this.writeByte((byte) (value & 0xFF00));
    }

    public void writeByte(byte bite) {
        ensureCapacity(used + 1);
        this.bytes[used++] = bite;
    }

    /**
     * Returns the amount of remaining bytes
     */
    public int getRemaining() {
        return this.used - this.reading;
    }

    public void writeLong(long l) {
        byte[] bites = new byte[8];
        for (int i = 0; i < 8; i++) { //8 bytes in a long
            bites[i] = ((byte) (l >>> 8 * i & 255)); //Isolate then add each byte BTW it's >>> because i don't want to preserve the sign, since I'm working with bytes, not their number representations unsure if i need it to be >>> but it's safer to have it
        }
        append(bites);
    }

    public void writeDouble(double d) {
        writeLong(Double.doubleToRawLongBits(d));
    }

    public void writeFloat(float d) {
        writeInt(Float.floatToIntBits(d));
    }

    public void writeInt(int i) {
        byte[] bites = new byte[4];
        for (int i1 = 0; i1 < 4; i1++) //4 bytes in int
            bites[i1] = ((byte) (i >>> 8 * i1 & 255)); //Isolate then add each byte
        append(bites);
    }

    public void writeBoolean(boolean bool) {
        this.writeByte((byte) (bool ? 1 : 0));
    }

    public void writeBooleanArray(boolean... bools) {
        int len = bools.length >> 3;
        byte rem = (byte) (bools.length % 8);

        byte[] ret = new byte[len + (rem > 0 ? 1 : 0)];

        for (int i = 0; i < len; i ++) {
            byte b = 0;
            for (byte j = 0; j < 8; j ++) {
                if (bools[i << 3 | j]) {
                    b |= 1;
                }
                if (j == 7)
                    break;
                b <<= 1;
            }
            ret[i] = b;
        }
        if (rem > 0) {
            byte b = 0;
            for (byte j = 0; j < rem; j ++) {
                if (bools[len << 3 | j]) {
                    b |= 1;
                }
                b <<= 1;
            }
            ret[len] = b;
        }

        writeByte(rem);
        writeByteArray(ret);
    }

    public boolean[] readBooleanArray() {
        byte rem = readByte();
        byte[] dat = readByteArray();
        int len = dat.length - (rem > 0 ? 1 : 0);

        boolean[] ret = new boolean[len << 3 | rem];

        for (int i = 0; i < len; i ++) {
            byte b = dat[i];
            for (int j = 0; j < 8; j ++) {
                ret[i << 3 | j] = ((b >>> 7) & 1) == 1;
                b <<= 1;
            }
        }
        if (rem > 0) {
            byte b = dat[len];
            b <<= (7 - rem);
            for (int j = 0; j < rem; j ++) {
                ret[len << 3 | j] = ((b >>> 7) & 1) == 1;
                b <<= 1;
            }
        }

        return ret;
    }

    public void writeObject(Object o) {
        Serializers.serializeObject(this, o);
    }

    public <T> T readObject(Object... args) {
        return (T) Serializers.deserializeObject(this, args);
    }

    public GravSerializer readSerializer() {
        return new GravSerializer(this.readByteArray());
    }

    public void writeSerializer(GravSerializer serializer) {
        this.writeByteArray(serializer.toByteArray());
    }

    public long readLong() {
        long out = 0L;
        for (int i = 0; i < 8; i++) {
            out |= ((long) readByte() << (i * 8)) & ((long) 255 << (i * 8));
        }
        return out;
    }

    public short readShort() {
        byte b1 = this.readByte();
        byte b2 = this.readByte();
        return (short) (b2 << 8 | b1);
    }

    public boolean readBoolean() {
        return this.readByte() == 1;
    }

    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public byte[] readByteArray() {
        int length = readInt();
        byte[] bites = new byte[length];
        for (int i = 0; i < length; i++) {
            bites[i] = readByte();
        }
        return bites;
    }

    public byte readByte() {
        if (reading >= used)
            throw new IllegalStateException("End of byte array reached (GravSerializer)");
        return bytes[reading++];
    }

    public boolean hasNext() {
        return reading < used;
    }

    public void append(GravSerializer serializer) {
        append(serializer.bytes, serializer.used);
    }

    public void writeUUID(UUID id) {
        ensureCapacity(used + 16);
        this.writeLong(id.getMostSignificantBits());
        this.writeLong(id.getLeastSignificantBits());
    }

    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    public int readInt() {
        int out = 0;
        for (int i = 0; i < 4; i++) {
            out |= ((int) readByte() << (i * 8)) & (255 << (i * 8));
        }
        return out;
    }

    public String readString() {
        int size = readInt();
        byte[] bites = new byte[size];
        for (int i = 0; i < size; i++) {
            bites[i] = readByte();
        }
        return new String(bites);
    }

    public String toString() {
        return Base64.getEncoder().encodeToString(toByteArray());
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, used);
    }

    public void writeToStream(OutputStream stream) throws IOException {
        byte[] bt = toByteArray();

        for (byte b : bt) {
            stream.write(b);
        }
        stream.flush();
        stream.close();
    }
}
