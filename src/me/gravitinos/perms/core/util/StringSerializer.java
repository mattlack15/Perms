package me.gravitinos.perms.core.util;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class StringSerializer {
    private List<Byte> bytes = new ArrayList<>();
    private int reading = 0;
    private int mark = 0;

    public StringSerializer(){

    }

    public StringSerializer(String input){
        for(byte b : Base64.getDecoder().decode(input))
            bytes.add(b);
    }

    public StringSerializer(byte[] input){
        for(byte b : input)
            bytes.add(b);
    }

    /**
     * Sets the marker to the current reading byte
     * When you do reset() it will reset to the current reading byte
     */
    public void mark() {
        mark = reading;
    }

    /**
     * Resets to the last marker (mark()) marker is by default 0
     */
    public void reset() {
        this.reading = mark;
    }

    public void writeString(String str){
        int size = str.length();
        writeInt(size);
        for(byte bite : str.getBytes()){
            bytes.add(bite);
        }
    }

    public void writeLong(long l){
        List<Byte> bites = new ArrayList<>();
        for(int i = 0; i < 8; i++) { //8 bytes in a long
            bites.add((byte) (l >>> 8 * i & 255)); //Isolate then add each byte BTW it's >>> because i don't want to preserve the sign, since I'm working with bytes, not their number representations unsure if i need it to be >>> but it's safer to have it
        }
        bytes.addAll(bites);
    }

    public void writeInt(int i){
        List<Byte> bites = new ArrayList<>();
        for(int i1 = 0; i1 < 4; i1++) //4 bytes in int
            bites.add((byte) (i >>> 8 * i1 & 255)); //Isolate then add each byte
        bytes.addAll(bites);
    }

    public long readLong(){
        long out = 0L;
        for(int i = 0; i < 8; i++) {
            out |= ((long)readByte() << (i * 8)) & ((long)255 << (i * 8));
        }
        return out;
    }
    
    public byte readByte() {
        if(reading >= bytes.size())
            throw new UnsupportedOperationException("End of byte array reached (StringSerializer)");
        return bytes.get(reading++);
    }

    public int readInt(){
        int out = 0;
        for(int i = 0; i < 4; i++) {
            out |= ((int)readByte() << (i * 8)) & (255 << (i * 8));
        }
        return out;
    }

    public String readString(){

        int size = readInt();
        byte[] bites = new byte[size];
        for(int i = 0; i < size; i++){
            bites[i] = readByte();
        }
        return new String(bites);
    }

    public String toString() {
        byte[] bites = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); i++){
            bites[i] = bytes.get(i);
        }
        return Base64.getEncoder().encodeToString(bites);
    }

    public byte[] toByteArray(){
        byte[] bites = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); i++)
            bites[i] = bytes.get(i);
        return bites;
    }
}
