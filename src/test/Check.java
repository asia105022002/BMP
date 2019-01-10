package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Check {
    public static void main(String[] args) throws IOException {
        File file1=new File("");
        File file2=new File("");

        byte[] bytes1 = new byte[(int) file1.length()];
        FileInputStream fileInputStream = new FileInputStream(file1);
        fileInputStream.read(bytes1);
        fileInputStream.close();
        System.out.println(bytes1.length);
        System.out.println(bytes1[183073]);
        byte[] bytes2 = new byte[(int) file2.length()];
        fileInputStream = new FileInputStream(file2);
        fileInputStream.read(bytes2);

        fileInputStream.close();
        System.out.println(bytes2.length);

        for(int c=0;c<bytes1.length;c++)
            if(bytes1[c]!=bytes2[c])
                System.out.println(c+","+bytes1[c]+" "+bytes2[c]);


    }
}
