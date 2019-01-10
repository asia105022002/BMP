package core;

import java.io.*;

public class ByteArrayToBinary {

    private byte[] bytes;
    private StringBuilder current = new StringBuilder();
    private int index = 0;

    public static void main(String[] args) throws IOException {
//        File file = new File("C:\\Users\\Slia\\Desktop\\新增資料夾 (4)\\BXhUjmzCIAAQLDz.jpg");
//        ByteArrayToBinary B2b = new ByteArrayToBinary(file);
    }



    public ByteArrayToBinary(byte[] bytes) {
        this.bytes=bytes;
        toCurrent();
    }

    public String pop(byte l) {
        if (l > current.length())
            if (!toCurrent())
                return current.toString();
        String temp = current.substring(0, l);
        current = new StringBuilder(current.substring(l));
        return temp;
    }

    private boolean toCurrent() {
        if (index == bytes.length)
            return false;
        String temp = Integer.toBinaryString(bytes[index++] & 0xFF);
        current.append(String.format("%8s", temp).replace(' ', '0'));
        return true;
        //System.out.println((byte)Integer.parseInt(current.toString(),2));
    }
}
