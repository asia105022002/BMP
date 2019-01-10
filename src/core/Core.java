package core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Core {
    private File inputFile, outputFile, extractFile;
    private Pixel[][] pixels;
    private Pixel[][] pixelsB;
    private Pixel[][] pixelsO;
    private int width, height;
    private int width1, height1;
    private double scale;
    private byte ceiling;

    private File fileToHide;
    private String fileExtension;

    public Core() {

    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFilePath(File file) {
        fileToHide = file;
    }

    public void setInputPath(File input) {
        inputFile = input;
    }

    public void setOutputPath(File output) {
        outputFile = output;
    }

    public void setExtractPath(File extract) {
        extractFile = extract;
    }

    public void setCeiling(byte ceiling) {
        this.ceiling = ceiling;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void loadPixelsO() throws IOException {
        BufferedImage img = ImageIO.read(inputFile);
        Raster raster = img.getData();
        width = img.getWidth();
        height = img.getHeight();
        pixelsO = new Pixel[height][width];

        width1 = (int) Math.floor(width * scale);
        height1 = (int) Math.floor(height * scale);

        pixels = new Pixel[height1][width1];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                pixelsO[i][j] = new Pixel((short) raster.getSample(j, i, 0));
                int i1 = map(i, height);
                int j1 = map(j, width);
                pixels[i1][j1] = new Pixel((short) raster.getSample(j, i, 0));
                pixels[i1][j1].Mark();
            }
        }
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                if (pixels[i][j] == null)
                    pixels[i][j] = new Pixel();
            }
        }
        long l = 0;
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                if (pixels[i][j].isMark())
                    l++;
            }
        }
        System.out.println(l);
    }

    public void loadPixels() throws IOException {
        BufferedImage img = ImageIO.read(outputFile);
        Raster raster = img.getData();
        width1 = img.getWidth();
        height1 = img.getHeight();
        pixels = new Pixel[height1][width1];
        pixelsB = new Pixel[height1][width1];

        width = (int) Math.ceil(width1 / scale);
        height = (int) Math.ceil(height1 / scale);

        pixelsO = new Pixel[height][width];

        for (int i1 = 0; i1 < height1; i1++) {
            for (int j1 = 0; j1 < width1; j1++) {
                pixels[i1][j1] = new Pixel((short) raster.getSample(j1, i1, 0));
                pixelsB[i1][j1] = new Pixel((short) raster.getSample(j1, i1, 0));
            }
        }
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int i1 = map(i, height);
                int j1 = map(j, width);
                pixelsO[i][j] = new Pixel((short) raster.getSample(j1, i1, 0));
                pixels[i1][j1].Mark();
            }
        }
    }

    public void lerpAll() {
        for (int i = 0; i < width1; i++) {//做直的
            if (pixels[0][i].isMark()) {
                for (int j = 0; j < height1; j++) {
                    if (!pixels[j][i].isMark()) {
                        int up = --j, down = ++j;
                        while (!pixels[up][i].isMark())
                            --up;
                        while (!pixels[down][i].isMark())
                            ++down;
                        int t = lerp(pixels[up][i].getGrayScale(), pixels[down][i].getGrayScale(), j - up, down - j);
                        pixels[j][i].setGrayScale((short) t);
                        pixels[j][i].Done();
                    }
                }
            }
        }
        for (int i = 0; i < height1; i++) {//做橫的
            for (int j = 0; j < width1; j++) {
                if (!pixels[i][j].isDone()) {
                    int left = --j, right = ++j;
                    while (!pixels[i][left].isDone())
                        --left;
                    while (!pixels[i][right].isDone())
                        ++right;
                    int t = lerp(pixels[i][left].getGrayScale(), pixels[i][right].getGrayScale(), j - left, right - j);
                    pixels[i][j].setGrayScale((short) t);
                    pixels[i][j].Done();
                }
            }
        }
    }

    public long getCapacity() {
        long max = 0;
        for (int a = 0; a < height - 1; a++) {
            for (int b = 0; b < width - 1; b++) {
                int i = map(a, height), j = map(b, width);
                int i1 = map(a + 1, height), j1 = map(b + 1, width);
                short[] values = {
                        pixels[i][j].getGrayScale(),//左上
                        pixels[i][j1].getGrayScale(),//右上
                        pixels[i1][j].getGrayScale(),//左下
                        pixels[i1][j1].getGrayScale()//右下
                };
                Arrays.sort(values);
                short minValue = values[0];
                short maxValue = values[3];
                //區塊計算嵌入量
                if (i1 == height - 1) {
                    i1++;
                }
                if (j1 == width - 1) {
                    j1++;
                }
                for (int y = i; y < i1; y++) {
                    for (int x = j; x < j1; x++) {
                        if (!pixels[y][x].isMark()) {
                            int n = Math.max(
                                    maxValue - pixels[y][x].getGrayScale(),
                                    pixels[y][x].getGrayScale() - minValue
                            );
                            byte c = (byte) (Math.log(n) / Math.log(2));
                            if (c > ceiling)
                                c = ceiling;
                            pixels[y][x].setCapacity(c);
                            max += c;
                        }
                    }
                }
            }
        }
        return max;
    }

    public String embed() throws IOException {
        boolean flag = true;
        String fileName = fileToHide.getName();
        int iIndex = 0, jIndex = 0, lsbIndex = 0;
        byte[] fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).getBytes();

//        System.out.println(fileExtension.length);
//        System.out.println(fileExtension[0]);
//        System.out.println(fileExtension[1]);
//        System.out.println(fileExtension[2]);

        byte[] header = new byte[13];
        System.arraycopy(fileExtension, 0, header, 9, fileExtension.length);

        byte[] bytes = new byte[(int) fileToHide.length()];
        FileInputStream fileInputStream = new FileInputStream(fileToHide);
        fileInputStream.read(bytes);
        fileInputStream.close();

        byte[] array = new byte[header.length + bytes.length];
        System.arraycopy(header, 0, array, 0, header.length);
        System.arraycopy(bytes, 0, array, header.length, bytes.length);

        ByteArrayToBinary B2b = new ByteArrayToBinary(array);
        Random random = new Random();
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                if (!pixels[i][j].isMark()) {
                    byte c = pixels[i][j].getCapacity();
                    if (c > 0) {
                        int msg;
                        if (flag) {
                            StringBuilder t = new StringBuilder(B2b.pop(c));
                            if (t.length() != c) {
                                flag = false;
                                iIndex = i;
                                jIndex = j;
                                lsbIndex = t.length();
                                System.out.println(iIndex);
                                System.out.println(jIndex);
                                System.out.println(lsbIndex);
                                while (t.length() < c)
                                    t.append("0");
                            }
                            msg = Integer.parseInt(t.toString(), 2);
                        } else {
                            msg = random.nextInt((int) Math.pow(2, c));
                        }
                        int p1 = pixels[i][j].getGrayScale();
                        int v = p1 >> (c + 1) << (c + 1) | msg;
                        int v1 = (p1 >> c | 1) << c | msg;
                        p1 = Math.abs(v1 - p1) > Math.abs(v - p1) ? v : v1;
                        pixels[i][j].setGrayScale((short) p1);
                        s.append(msg);
                    }
                }
            }
        }
        byte[] iBytes = intToByteArray(iIndex);
        byte[] jBytes = intToByteArray(jIndex);
        System.arraycopy(iBytes, 0, header, 0, iBytes.length);
        System.arraycopy(jBytes, 0, header, 4, jBytes.length);
        header[8] = (byte) lsbIndex;
        System.out.println("inLsb"+header[12]);
        B2b = new ByteArrayToBinary(header);
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                if (!pixels[i][j].isMark()) {
                    byte c = pixels[i][j].getCapacity();
                    if (c > 0) {
                        StringBuilder t = new StringBuilder(B2b.pop(c));
                        if (t.length() != c) {
                            String pixel = Integer.toBinaryString(pixels[i][j].getGrayScale() & 0xFF);
                            pixel = String.format("%8s", pixel).replace(' ', '0');
                            int subIndex = 8 - (c - t.length());
                            t.append(pixel.substring(subIndex));
                            flag = true;
                        }
                        int msg = Integer.parseInt(t.toString(), 2);
                        int p1 = pixels[i][j].getGrayScale();
                        int v = p1 >> (c + 1) << (c + 1) | msg;
                        int v1 = (p1 >> c | 1) << c | msg;
                        p1 = Math.abs(v1 - p1) > Math.abs(v - p1) ? v : v1;
                        pixels[i][j].setGrayScale((short) p1);
                        if (flag) {
                            return s.toString();
                        }
                    }
                }
            }
        }
        System.out.println("error");
        return s.toString();
    }

    public ArrayList<Byte> extract() {
        ArrayList<Byte> bytes = new ArrayList<>();
        int iIndex = -1, jIndex = -1, lsbIndex = -1;
        boolean b = false;
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                if (!pixels[i][j].isMark()) {
                    int c = pixels[i][j].getCapacity();
                    if (c > 0) {
                        int p1 = pixelsB[i][j].getGrayScale();
                        int integer = p1 % (int) (Math.pow(2, c));
                        String temp = Integer.toBinaryString(integer & ((int) Math.pow(2, c) - 1));
                        if (i == iIndex && j == jIndex) {
                            String s1=String.format("%" + c + "s", temp).replace(' ', '0');
                            s.append(s1, 0, lsbIndex);
                        } else {
                            s.append(String.format("%" + c + "s", temp).replace(' ', '0'));
                        }
                        if (s.length() >= 104) {
                            b = true;
                            byte[] t = new byte[4];
                            t[0] = (byte) Integer.parseInt(s.substring(0, 8), 2);
                            t[1] = (byte) Integer.parseInt(s.substring(8, 16), 2);
                            t[2] = (byte) Integer.parseInt(s.substring(16, 24), 2);
                            t[3] = (byte) Integer.parseInt(s.substring(24, 32), 2);
                            iIndex = byteArrayToInt(t);
                            s = new StringBuilder(s.substring(32));

                            System.out.println(t[0]);
                            System.out.println(t[1]);
                            System.out.println(t[2]);
                            System.out.println(t[3]);
                            System.out.println("i:"+iIndex);

                            t[0] = (byte) Integer.parseInt(s.substring(0, 8), 2);
                            t[1] = (byte) Integer.parseInt(s.substring(8, 16), 2);
                            t[2] = (byte) Integer.parseInt(s.substring(16, 24), 2);
                            t[3] = (byte) Integer.parseInt(s.substring(24, 32), 2);
                            jIndex = byteArrayToInt(t);
                            s = new StringBuilder(s.substring(32));

                            System.out.println(t[0]);
                            System.out.println(t[1]);
                            System.out.println(t[2]);
                            System.out.println(t[3]);
                            System.out.println("j:"+jIndex);

                            lsbIndex = Integer.parseInt(s.substring(0, 8), 2);
                            System.out.println(s.substring(0, 8));
                            s = new StringBuilder(s.substring(8));
                            System.out.println("lsb:"+lsbIndex);

                            t[0] = (byte) Integer.parseInt(s.substring(0, 8), 2);
                            t[1] = (byte) Integer.parseInt(s.substring(8, 16), 2);
                            t[2] = (byte) Integer.parseInt(s.substring(16, 24), 2);
                            t[3] = (byte) Integer.parseInt(s.substring(24, 32), 2);

                            System.out.println(t[3]);
                            System.out.println(t[3]==0);

                            int extensionIndex=0;
                            while(extensionIndex<t.length&&t[extensionIndex]!=0)
                                ++extensionIndex;
                            byte[] extensionByte=new byte[extensionIndex];
                            System.arraycopy(t,0,extensionByte,0,extensionByte.length);
                            fileExtension = new String(extensionByte);
                            s = new StringBuilder(s.substring(32));
                        }
                        if (b) {
                            if (s.length() >= 8) {
                                byte byteForAdding = (byte) Integer.parseInt(s.substring(0, 8), 2);
                                bytes.add(byteForAdding);
                                s = new StringBuilder(s.substring(8));
                            }
                            if (i == iIndex && j == jIndex) {
                                System.out.println(s);
                                return bytes;
                            }
                        }

                    }
                }
            }
        }
        return bytes;
    }


    public void writePixels() throws IOException {
        BufferedImage imageOut = new BufferedImage(width1, height1, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster writableRaster = imageOut.getRaster();
        for (int i = 0; i < height1; i++) {
            for (int j = 0; j < width1; j++) {
                writableRaster.setSample(j, i, 0, pixels[i][j].getGrayScale());
            }
        }
        imageOut.setData(writableRaster);
        ImageIO.write(imageOut, "bmp", outputFile);
    }

    public void writePixelsO() throws IOException {
        BufferedImage imageOut = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster writableRaster = imageOut.getRaster();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                writableRaster.setSample(j, i, 0, pixelsO[i][j].getGrayScale());
            }
        }
        imageOut.setData(writableRaster);
        ImageIO.write(imageOut, "bmp", extractFile);
    }

    //Linear Interpolation
    private static int lerp(int valuef, int values, int f, int s) {
        return (values - valuef) / (f + s) * f + valuef;
    }

    private int map(int i, int r) {
        return (int) Math.floor((Math.floor(r * scale) - 1) * i / (r - 1));
    }

    private int byteArrayToInt(byte[] b) {
        return b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    private byte[] intToByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }
}

class Pixel {
    private short grayScale = 255;
    private byte capacity = 0;
    private boolean mark = false;
    private boolean done = false;

    Pixel() {

    }

    Pixel(short grayScale) {
        this.grayScale = grayScale;
    }

//    public Pixel(short grayScale, boolean mark) {
//        this.grayScale = grayScale;
//        this.mark = mark;
//    }

    void Mark() {
        mark = true;
        done = true;
    }

    void Done() {
        done = true;
    }

    boolean isDone() {
        return done || mark;
    }

    boolean isMark() {
        return mark;
    }

    void setGrayScale(short v) {
        grayScale = v;
    }

    short getGrayScale() {
        return grayScale;
    }

    void setCapacity(byte v) {
        capacity = v;
    }

    byte getCapacity() {
        return capacity;
    }
}


