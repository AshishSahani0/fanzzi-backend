package com.fanzzi.backend.live.agora;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

public class ByteBuf {

    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ByteBuffer buffer;

    public ByteBuf() {}

    public ByteBuf(byte[] data) {
        buffer = ByteBuffer.wrap(data);
    }

    public ByteBuf put(int v) {
        out.write((byte)(v >> 24));
        out.write((byte)(v >> 16));
        out.write((byte)(v >> 8));
        out.write((byte)v);
        return this;
    }

    public ByteBuf put(byte[] bytes) {
        put(bytes.length);
        out.write(bytes, 0, bytes.length);
        return this;
    }

    public ByteBuf put(String str) {
        return put(str.getBytes());
    }

    public ByteBuf putIntMap(TreeMap<Short,Integer> map) {
        put(map.size());
        for(Map.Entry<Short,Integer> e : map.entrySet()){
            put(e.getKey());
            put(e.getValue());
        }
        return this;
    }

    public byte[] asBytes() {
        return out.toByteArray();
    }

    public int readInt() {
        return buffer.getInt();
    }

    public byte[] readBytes() {
        int len = readInt();
        byte[] dst = new byte[len];
        buffer.get(dst);
        return dst;
    }

    public TreeMap<Short,Integer> readIntMap(){
        int size = readInt();
        TreeMap<Short,Integer> map = new TreeMap<>();

        for(int i=0;i<size;i++){
            short key = buffer.getShort();
            int value = readInt();
            map.put(key,value);
        }
        return map;
    }
}