package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import core.Core;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RevealController {
    @FXML
    private
    Parent root;
    @FXML
    BorderPane pane;
    @FXML
    TextField imgInputPath;
    @FXML
    TextArea textArea;
    @FXML
    TextField scale;
    @FXML
    TextField ceiling;

    private Core core = new Core();

    public void toHide() throws IOException {
        root = FXMLLoader.load(getClass().getResource("../res/hide.fxml"));
        Scene scene = new Scene(root);
        Stage stage = (Stage) pane.getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    public void selectImg() {
        FileChooser fileChooser = new FileChooser();

        File file = new File(imgInputPath.getText());
        if (file.exists()) {
            file = new File(file.getParent());
            fileChooser.setInitialDirectory(file);
        }

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
            } else
                imgInputPath.setText(selectFile.toString());
        }
    }

    public void loadImg() {
        File file = new File(imgInputPath.getText());
        if (!file.isFile()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案不存在");
            alert.showAndWait();
        }
    }

    public void extract() {
        try {
            double scale = Double.valueOf(this.scale.getText());
            core.setScale(scale);
            int ceiling = Integer.valueOf(this.ceiling.getText());
            core.setCeiling((byte) ceiling);
            File inputFile = new File(imgInputPath.getText());
            core.setOutputPath(inputFile);
            core.loadPixels();
            core.lerpAll();
            core.getCapacity();

            ArrayList<Byte> list = core.extract();
            byte[] bytes = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                bytes[i] = list.get(i);
            }//返還list 轉[]輸出檔案

            File outputFile = new File(inputFile.getParent() + "\\取出." + core.getFileExtension());

            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            fileOutputStream.write(bytes);
            fileOutputStream.close();//輸出檔案

            textArea.appendText("檔案已取出\n");
            textArea.appendText("檔案生成:" + outputFile + "\n");

            core.setExtractPath(new File(inputFile.getParent() + "\\還原.bmp"));
            core.writePixelsO();//還原圖片
            textArea.appendText("還原圖片:" + inputFile.getParent() + "\\還原.bmp\n");

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText(null);
            alert.setContentText("檔案已取出:" + outputFile + "\n是否開啟資料夾?");
            alert.showAndWait();
            if (alert.getResult() == ButtonType.OK)
                Desktop.getDesktop().open(new File(outputFile.getParent()));

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.toString());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案取出失敗");
            alert.showAndWait();
        } catch (FileNotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("找不到檔案");
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("檔案取出失敗");
            alert.showAndWait();
        }
    }

}
