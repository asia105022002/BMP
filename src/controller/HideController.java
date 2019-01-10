package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import core.Core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;


public class HideController {
    @FXML
    private
    Parent root;
    @FXML
    BorderPane pane;
    @FXML
    ScrollPane paneLeft;
    @FXML
    ScrollPane paneRight;
    @FXML
    TextField imgInputPath;
    @FXML
    TextField fileInputPath;
    @FXML
    TextArea textArea;
    @FXML
    TextField scale;
    @FXML
    TextField ceiling;
    @FXML
    Label capacity;
    @FXML
    Label size;
    @FXML
    ImageView imageIn;
    @FXML
    ImageView imageOut;

    private Node leftNode;
    private Node RightNode;
    private long c;
    private boolean enableEmbed = false;
    private Core core = new Core();

    @FXML
    public void initialize() {
        leftNode = pane.getLeft();
        RightNode = pane.getRight();
        pane.setLeft(null);
        pane.setRight(null);
    }

    private void setParameter() {
        double scale = Double.valueOf(this.scale.getText());
        core.setScale(scale);
        int ceiling = Integer.valueOf(this.ceiling.getText());
        core.setCeiling((byte) ceiling);
        File input = new File(imgInputPath.getText());
        core.setInputPath(input);
    }

    public void toReveal() throws IOException {
        root = FXMLLoader.load(getClass().getResource("../res/reveal.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) pane.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    public void getCapacity() throws IOException {
        setParameter();
        core.loadPixelsO();
        core.lerpAll();
        c = core.getCapacity();
        capacity.setText("可嵌入量:" + binaryFormat((int) c) + " (" + c / 8 + " 位元組)");
        textArea.appendText("可嵌入訊息量計算完成,約" + binaryFormat((int) c) + "\n");
        enableEmbed = true;
    }

    public void embed() throws IOException {
        if (!enableEmbed) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("請先計算可嵌入量");
            alert.showAndWait();
        } else {
            File file = new File(fileInputPath.getText());
            if (!file.isFile()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("檔案不存在");
                alert.showAndWait();
            } else if (file.length() > c / 8) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("檔案大小超過可嵌入量，無法嵌入");
                alert.showAndWait();
            } else {
                BufferedImage img = ImageIO.read(new File(imgInputPath.getText()));
                if (img.getType() != BufferedImage.TYPE_BYTE_GRAY) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setHeaderText(null);
                    alert.setContentText("非灰階影像!");
                    alert.showAndWait();
                } else {
                    setParameter();
                    core.setFilePath(file);
                    File path = new File(imgInputPath.getText());
                    path = new File(path.getParent() + "\\hide.bmp");
                    core.embed();
                    core.setOutputPath(path);
                    core.writePixels();
                    textArea.appendText("檔案嵌入完成\n");
                    textArea.appendText("圖片生成:" + path + "\n");
                    imageOut.setImage(new Image(path.toURI().toURL().toString()));
                    pane.setRight(RightNode);

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("圖片生成:" + path + "\n是否開啟資料夾?");
                    alert.showAndWait();
                    if (alert.getResult() == ButtonType.OK)
                        Desktop.getDesktop().open(new File(path.getParent()));
                }
            }
        }
    }

    public void extract() throws IOException {
        setParameter();
        core.setOutputPath(new File("D:\\作業\\助教\\hide.bmp"));
        core.setExtractPath(new File("D:\\作業\\助教\\還原.bmp"));
        File output = new File(imgInputPath.getText());
        core.loadPixels();
        core.lerpAll();
        core.getCapacity();

        ArrayList<Byte> list = core.extract();
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i);
        }

//
        File file = new File(output.getParent() + "\\exx." + core.getFileExtension().substring(0, 3));

        System.out.println(file);

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println(file + "!");
        }
        fileOutputStream.write(bytes);
        fileOutputStream.close();

        textArea.appendText("訊息摘出完成\n");
        textArea.appendText("摘出訊息於:" + output.getParent() + "\\摘出.txt\n");
        core.writePixelsO();
        textArea.appendText("還原圖片於:" + fileInputPath.getText() + "\n");
    }


    public void selectImg() throws MalformedURLException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("bmp", "*.bmp"));
        Stage stage = (Stage) pane.getScene().getWindow();
        File selectFile = fileChooser.showOpenDialog(stage);
        if (selectFile != null) {
            long fileSize = selectFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("檔案過大，請小於2GB");
                alert.showAndWait();
            } else {
                imgInputPath.setText(selectFile.toString());
                loadImg();
            }
        }
    }

    public void loadImg() throws MalformedURLException {
        File file = new File(imgInputPath.getText());
        if (!file.isFile()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案不存在");
            alert.showAndWait();
        } else {
            Image image = new Image(file.toURI().toURL().toString());
            imageIn.setImage(image);
            pane.setLeft(leftNode);
        }

    }

    public void selectFile() {
        FileChooser fileChooser = new FileChooser();
        Stage stage = (Stage) pane.getScene().getWindow();
        File selectFile = fileChooser.showOpenDialog(stage);
        if (selectFile != null) {
            long fileSize = selectFile.length();
            if (fileSize > Integer.MAX_VALUE) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setContentText("檔案過大，請小於2GB");
                alert.showAndWait();
            } else {
                fileInputPath.setText(selectFile.toString());
                loadFile();
            }
        }
    }

    public void loadFile() {
        File file = new File(fileInputPath.getText());
        if (!file.isFile()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案不存在");
            alert.showAndWait();
        } else if (file.length() > Integer.MAX_VALUE) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案過大，請小於2GB");
            alert.showAndWait();
        } else {
            size.setText("檔案大小:" + ByteFormat((int) file.length()) + " (" + file.length() + " 位元組)");
            core.setFilePath(file);
        }
    }

    private String binaryFormat(int i) {
        return ByteFormat(i / 8);

    }

    private String ByteFormat(int i) {
        if ((double) i < 1024)
            return String.format("%.0f 位元組", (double) i);
        else if ((double) i / 1024 < 1024)
            return String.format("%.2f KB", (double) i / 1024);
        else if ((double) i / 1024 / 1024 < 1024)
            return String.format("%.2f MB", (double) i / 1024 / 1024);
        else
            return String.format("%.2f GB", (double) i / 1024 / 1024 / 1024);

    }

}



