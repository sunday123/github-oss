package com.ij34;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ij34.model.GitUser;
import com.ij34.model.UploadDto;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.coobird.thumbnailator.Thumbnails;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrimaryController {
    Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    UploadDto uploadDto;
    @FXML
    JFXTextField ownerTextField;
    @FXML
    JFXTextField repoTextField;
    @FXML
    JFXTextField pathTextField;
    @FXML
    JFXTextField branchTextField;
    @FXML
    JFXTextField domainTextField;
    @FXML
    JFXPasswordField tokenPasswordField;
    @FXML
    Label tipLabel;
    @FXML
    JFXCheckBox reFileNameBox;


    @FXML
        // This method is called by the FXMLLoader when initialization is complete
    void initialize() throws BackingStoreException {
        byte[] bytes2 = preferences.getByteArray("github-oss-user-key", null);
        try {
            if (bytes2 != null) {
                GitUser u = ObjectUtil.deserialize(bytes2);
                ownerTextField.setText(u.getOwner());
                repoTextField.setText(u.getRepo());
                pathTextField.setText(u.getPath());
                branchTextField.setText(u.getBranch());
                domainTextField.setText(u.getDomain());
                tokenPasswordField.setText(u.getToken());
                if (u.getIsReName() != null) {
                    reFileNameBox.setSelected(u.getIsReName());
                }

            }
        } catch (Exception e) {
            preferences.clear();
        }

    }

    @FXML
    private void saveAction(ActionEvent event) {
        tipLabel.setText("");

        GitUser user = GitUser.builder()
                .owner(ownerTextField.getText())
                .repo(repoTextField.getText())
                .path(pathTextField.getText())
                .branch(branchTextField.getText())
                .domain(domainTextField.getText())
                .token(tokenPasswordField.getText())
                .isReName(reFileNameBox.isSelected())
                .build();
        boolean checkBoolean = checkInput(user);
        if (!checkBoolean) {
            return;
        }
        byte[] bytes = ObjectUtil.serialize(user);
        preferences.putByteArray("github-oss-user-key", bytes);
        tipLabel.setText("save success!");

    }

    private boolean checkInput(GitUser u) {
        if (StrUtil.isBlank(u.getOwner())) {
            tipLabel.setText("owner empty");
            return false;
        }
        if (StrUtil.isBlank(u.getRepo())) {
            tipLabel.setText("repo empty");
            return false;
        }
        if (StrUtil.isNotBlank(u.getPath()) && u.getPath().charAt(0) != '/') {
            tipLabel.setText("path format error");
            return false;
        }
        if (StrUtil.isBlank(u.getBranch())) {
            tipLabel.setText("branch empty");
            return false;
        }
        if (StrUtil.isBlank(u.getDomain())) {
            tipLabel.setText("domain empty");
            return false;
        }
        if (!isUrl(u.getDomain())) {
            tipLabel.setText("domain format error");
            return false;
        }
        if (StrUtil.isBlank(u.getToken()) || u.getToken().length() < 30) {
            tipLabel.setText("token  empty or error");
            return false;
        }
        return true;

    }

    public static boolean isUrl(String url) {
        String regex = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }


    @FXML
    Label uploadTipLabel;
    @FXML
    Label urlLabel;

    @FXML
    private void uploadClipboardAction(ActionEvent event) throws IOException {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasFiles()) {
            List<File> files = clipboard.getFiles();
            if (files != null && files.size() > 0 && files.get(0).exists()) {
                File file = files.get(0);
                doUpload(file);
            } else {
                uploadTipLabel.setText("clipboard is not a file");
            }


        } else {
            uploadTipLabel.setText("clipboard is not a file");
        }

    }

    @FXML
    private void uploadAction(ActionEvent event) {
        String prePath = preferences.get("github-oss-user-prePath-key", null);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("select file");
        if (StrUtil.isNotBlank(prePath)) {
            fileChooser.setInitialDirectory(FileUtil.file(prePath));
        }

        Stage stage = (Stage) urlLabel.getParent().getParent().getParent().getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        preferences.put("github-oss-user-prePath-key", file.getParent());
        doUpload(file);

    }

    @FXML
    private void deleteAction(ActionEvent event) {
        if (uploadDto == null || StrUtil.isBlank(uploadDto.getSha()) || StrUtil.isBlank(uploadDto.getUrl())) {
            uploadTipLabel.setText("no delete");
            return;
        }
        delete(uploadDto);
    }

    @FXML
    private void copyAction(ActionEvent event) {
        if (StrUtil.isNotBlank(urlLabel.getText())) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(DataFormat.PLAIN_TEXT, urlLabel.getText());
            clipboard.setContent(clipboardContent);
        }
    }

    private void doUpload(File file) {
        uploadTipLabel.setText("");
        byte[] bytes2 = preferences.getByteArray("github-oss-user-key", null);
        if (bytes2 == null) {
            uploadTipLabel.setText("setting null,cannot upload file");
            return;
        }
        try {
            if (bytes2 != null) {
                GitUser u = ObjectUtil.deserialize(bytes2);
                UploadDto record = new UploadDto();
                BeanUtil.copyProperties(u, record);
                record.setFileStr(file.getAbsolutePath());
                upload(record);
            }
        } catch (Exception e) {
            uploadTipLabel.setText("upload fail.finish setting .try again");
        }
    }

    private void upload(UploadDto record) {
        String fileStr = record.getFileStr();
        File file = FileUtil.file(fileStr);
        String content = Base64.encode(file);
        String fileName = file.getName();
        String suffix = getSuffixByFileName(fileName);
        String newName = record.getIsReName() ? DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + suffix : fileName;
        String doName = record.getDomain();
        String branch = record.getBranch();
        doName = (doName.charAt(doName.length() - 1) == '/') ? doName : doName + "/";
        String url = "https://api.github.com/repos/" + record.getOwner() + "/" + record.getRepo() + "/contents" + (StrUtil.isBlank(record.getPath()) ? "" : record.getPath()) + "/" + newName;
        String body = HttpRequest.put(url)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "token " + record.getToken())
                .body("{\"message\":\"upload file\",\"branch\": \"" + branch + "\",\"content\": \"" + content + "\"}")
                .timeout(20000).execute().body();
        JSONObject root = JSONUtil.parseObj(body);
        if (root.getJSONObject("content") != null && root.getJSONObject("content").getStr("name") != null
                && StrUtil.equals(newName, root.getJSONObject("content").getStr("name"))) {
            String sha = root.getJSONObject("content").getStr("sha");
            record.setSha(sha);
            record.setUrl(url);
            String download_url = root.getJSONObject("content").getStr("download_url");
            download_url = StrUtil.isBlank(record.getDomain()) ? download_url : doName + root.getJSONObject("content").getStr("path");
            urlLabel.setText(download_url);
            uploadTipLabel.setText(DateUtil.now() + "upload file success:" + fileName + ">" + newName);
            uploadDto = record;
        } else {
            uploadTipLabel.setText(DateUtil.now() + "upload file fail:" + fileName + ">" + newName);
        }
    }



    private void delete(UploadDto record) {
        String url = record.getUrl();
        String fileName = url.substring(url.lastIndexOf('/') + 1, url.length());

        String body = HttpRequest.delete(url)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "token " + record.getToken())
                .body("{\"message\":\"del file\" ,\"sha\": \"" + record.getSha() + "\"}")
                .timeout(20000).execute().body();
        JSONObject root = JSONUtil.parseObj(body);
        if (root.getJSONObject("commit") != null && root.getJSONObject("commit").getStr("sha") != null) {
            urlLabel.setText("");
            uploadTipLabel.setText(DateUtil.now() + "delete file success:" + fileName);
            uploadDto = null;
        } else {
            uploadTipLabel.setText(DateUtil.now() + "delete file fail:" + fileName);
        }
    }


    private static String getPreFixFileName(String fileName) {
        int index = StrUtil.lastIndexOfIgnoreCase(fileName, ".");
        if (index < 0) {
            return fileName;
        }
        return StrUtil.sub(fileName, 0, index);
    }

    private static String getSuffixByFileName(String fileName) {
        int index = StrUtil.lastIndexOfIgnoreCase(fileName, ".");
        if (index < 0) {
            return "";
        }
        return StrUtil.sub(fileName, index, fileName.length());
    }


    //compress

    @FXML
    Label compressLabel;
    @FXML
    JFXSlider scaleSlider,qualitySlider;

    @FXML
    private void compressCopyAction(ActionEvent event)  {
        if (StrUtil.isBlank(compressLabel.getText()) || compressLabel.getText().startsWith("success:")==false){
            return;
        }
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.put(DataFormat.PLAIN_TEXT, compressLabel.getText().substring(8,compressLabel.getText().length()));
        clipboard.setContent(clipboardContent);

    }



    @FXML
    private void compressAction(ActionEvent event)  {
        String prePath = preferences.get("github-oss-user-prePath-key", null);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("select file compress");
        if (StrUtil.isNotBlank(prePath)) {
            fileChooser.setInitialDirectory(FileUtil.file(prePath));
        }

        Stage stage = (Stage) urlLabel.getParent().getParent().getParent().getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        if (isImg(file)==false){
            compressLabel.setText("not jpg jpeg png");
            return;
        }
        preferences.put("github-oss-user-prePath-key", file.getParent());
        double scale = scaleSlider.getValue()/100.00;
        double quality=qualitySlider.getValue()/100.00;
        try {
            String dest=file.getAbsolutePath()+DateUtil.format(DateUtil.date(),"yyMMddHHmmss")+"."+FileTypeUtil.getType(file);
            Thumbnails.of(file).scale(scale).outputQuality(quality).toFile(dest);
            compressLabel.setText("success:"+dest);
        } catch (IOException e) {
            compressLabel.setText("program error");
            e.printStackTrace();
        }

    }

    private boolean isImg(File file) {
        List<String> list = ListUtil.of(ImgUtil.IMAGE_TYPE_JPEG,ImgUtil.IMAGE_TYPE_JPG,ImgUtil.IMAGE_TYPE_PNG);
        String type =  FileTypeUtil.getType(file);
        return list.stream().anyMatch(a->a.equalsIgnoreCase(type));
    }
}
