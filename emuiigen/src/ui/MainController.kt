package com.xortroll.emuiibo.emuiigen.ui

import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.concurrent.Callable
import java.io.File
import java.io.FileInputStream
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ListView
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.image.ImageView
import javafx.scene.image.Image
import javafx.event.EventHandler
import javafx.event.ActionEvent
import javafx.stage.Stage
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.beans.value.ChangeListener
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.application.HostServices
import org.apache.commons.io.FileUtils
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPReply
import com.xortroll.emuiibo.emuiigen.Amiibo
import com.xortroll.emuiibo.emuiigen.AmiiboStatus
import com.xortroll.emuiibo.emuiigen.AmiiboStatusKind
import com.xortroll.emuiibo.emuiigen.AmiiboAPI
import com.xortroll.emuiibo.emuiigen.AmiiboAPIEntry
import com.xortroll.emuiibo.emuiigen.Utils
import com.xortroll.emuiibo.emuiigen.AmiiboDate
import com.xortroll.emuiibo.emuiigen.ui.MainApplication
import com.xortroll.emuiibo.emuiigen.AmiiboAreaInfo
import kotlin.ExperimentalUnsignedTypes

@OptIn(ExperimentalUnsignedTypes::class)
class MainController {
    companion object {

        val TemporaryFtpDirectory = MainApplication.ResourceBundle.getString("ftp.tempDir");

    }

    @FXML lateinit var AmiiboOpenButton: Button;
    @FXML lateinit var AmiiboSaveButton: Button;
    @FXML lateinit var OpenedAmiiboNameText: TextField;
    @FXML lateinit var OpenedAmiiboUseRandomUuidCheck: CheckBox;
    @FXML lateinit var OpenedAmiiboAreaList: ListView<String>;

    @FXML lateinit var AboutButton: Button;
    
    @FXML lateinit var GenerateOneAmiiboSeriesBox: ComboBox<String>;
    @FXML lateinit var AmiiboBox: ComboBox<String>;
    @FXML lateinit var AmiiboImage: ImageView;
    @FXML lateinit var StatusLabel: Label;

    @FXML lateinit var AmiiboNameText: TextField;
    @FXML lateinit var AmiiboDirectoryText: TextField;
    @FXML lateinit var NameAsDirectoryNameCheck: CheckBox;
    @FXML lateinit var GenerateOneUseRandomUuidCheck: CheckBox;
    @FXML lateinit var GenerateOneImageSaveCheck: CheckBox;
    @FXML lateinit var GenerateOneFtpCheck: CheckBox;
    @FXML lateinit var GenerateOneFtpAddress: TextField;
    @FXML lateinit var GenerateOneFtpPort: TextField;
    @FXML lateinit var GenerateOneButton: Button;

    @FXML lateinit var GenerateAllUseRandomUuidCheck: CheckBox;
    @FXML lateinit var GenerateAllImageSaveCheck: CheckBox;
    @FXML lateinit var GenerateAllFtpCheck: CheckBox;
    @FXML lateinit var GenerateAllFtpAddress: TextField;
    @FXML lateinit var GenerateAllFtpPort: TextField;
    @FXML lateinit var GenerateAllButton: Button;

    @FXML lateinit var GenerateSeriesUseRandomUuidCheck: CheckBox;
    @FXML lateinit var GenerateSeriesImageSaveCheck: CheckBox;
    @FXML lateinit var GenerateSeriesAmiiboSeriesBox: ComboBox<String>;
    @FXML lateinit var GenerateSeriesFtpCheck: CheckBox;
    @FXML lateinit var GenerateSeriesFtpAddress: TextField;
    @FXML lateinit var GenerateSeriesFtpPort: TextField;
    @FXML lateinit var GenerateSeriesButton: Button;

    lateinit var MainStage: Stage;
    lateinit var OpenedAmiiboPath: String;
    var OpenedAmiibo: Amiibo? = null;
    lateinit var Amiibos: Map<String, List<AmiiboAPIEntry>>;

    fun updateSelectedAmiiboSeries() {
        val series = this.getSelectedAmiiboSeriesName();
        if(series != null) {
            val amiibos = this.Amiibos.get(series);
            amiibos?.let {
                val amiibo_names = mutableListOf<String>();
                for(amiibo in amiibos) {
                    amiibo_names.add(amiibo.amiibo_name);
                }
                amiibo_names.sort();
                this.AmiiboBox.items.setAll(amiibo_names);
            }
            ?: let {
                System.out.println("Internal unexpected error");
            }
        }
    }

    fun getSelectedAmiiboSeriesName() : String? {
        return this.GenerateOneAmiiboSeriesBox.selectionModel.selectedItem;
    }

    fun getSelectedAmiiboIndex() : Int {
        return this.AmiiboBox.selectionModel.selectedIndex;
    }

    fun getSelectedAmiibo() : AmiiboAPIEntry? {
        val series_name = this.getSelectedAmiiboSeriesName();
        if(series_name != null) {
            return this.Amiibos.get(series_name)?.get(this.getSelectedAmiiboIndex());
        }
        else {
            return null;
        }
    }

    fun updateSelectedAmiibo() {
        val series = this.getSelectedAmiiboSeriesName();
        if(series != null) {
            val amiibos = this.Amiibos.get(series);
            amiibos?.let {
                val amiibo_idx = this.getSelectedAmiiboIndex();

                if(amiibo_idx >= 0) {
                    val amiibo = amiibos.get(amiibo_idx);

                    val img = Image(amiibo.image_url, true);
                    this@MainController.AmiiboImage.setImage(img);
                    this@MainController.AmiiboNameText.setText(amiibo.amiibo_name);
                }
            }
            ?: let {
                this@MainController.showError(MainApplication.ResourceBundle.getString("error.internalUnexpectedError"));
            }
        }
        else {
            this@MainController.AmiiboImage.setImage(null);
        }
    }

    fun showYesNo(msg: String) : Boolean {
        val alert = Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.CANCEL);
        val res = alert.showAndWait();
        return res.isPresent() && (res.get() == ButtonType.YES);
    }
    
    fun showError(text: String) {
        val alert = Alert(Alert.AlertType.ERROR);
        alert.setTitle(MainApplication.ResourceBundle.getString("error.title"));
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    fun showInfo(text: String) {
        val alert = Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(MainApplication.ResourceBundle.getString("info.title"));
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    fun showWarn(text: String) {
        val alert = Alert(Alert.AlertType.WARNING);
        alert.setTitle(MainApplication.ResourceBundle.getString("warn.title"));
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    fun cleanTemporaryFtp() {
        FileUtils.deleteDirectory(Paths.get(TemporaryFtpDirectory).toAbsolutePath().toFile());
    }

    fun processFtpPort(is_ftp: Boolean, port_str: String) : Int? {
        if(is_ftp) {
            try {
                return port_str.toInt();
            }
            catch(ex: Exception) {
                this.showError(MainApplication.ResourceBundle.getString("error.invalidFtpPort").format(ex.toString()));
                return null;
            }
        }
        else {
            return 0;
        }
    }

    fun chooseBaseAmiiboPath(is_ftp: Boolean) : Pair<String, String>? {
        val path = if(is_ftp) {
            Paths.get(TemporaryFtpDirectory).toAbsolutePath()
        }
        else {
            val chooser = DirectoryChooser();
            val dir = chooser.showDialog(this@MainController.MainStage);
            if(dir != null) {
                Paths.get(dir.toString()).toAbsolutePath()
            }
            else {
                null
            }
        };

        val pres_path = if(is_ftp) {
            "ftp:/emuiibo/amiibo"
        }
        else {
            path.toString()
        };

        return if(path != null) {
            Pair(path.toString(), pres_path)
        }
        else {
            null
        };
    }

    fun generateAmiibo(path: String, base_path: String, amiibo: AmiiboAPIEntry, amiibo_name: String, use_random_uuid: Boolean, save_image: Boolean, is_ftp: Boolean, ftp_addr: String, ftp_port: Int) : Boolean {
        val local_date = LocalDateTime.now();
        val cur_date = AmiiboDate(local_date.year.toUShort(), local_date.monthValue.toUByte(), local_date.dayOfMonth.toUByte());

        val write_counter: UShort = 0u;
        val version: UInt = 0u;
        val mii_charinfo_file = "mii-charinfo.bin";
        val uuid = Amiibo.randomUuid();

        val amiibo_v = Amiibo(cur_date, amiibo.id, cur_date, mii_charinfo_file, amiibo_name, uuid, use_random_uuid, version, write_counter, null);
        if(amiibo_v.save(path)) {
            if(save_image) {
                try {
                    val image_path = Paths.get(path, "amiibo.png").toAbsolutePath().toString();
                    Utils.netDownloadFile(amiibo.image_url, image_path);
                }
                catch(ex: Exception) {
                    System.out.println("Exception saving amiibo image: " + ex.toString());
                    return false;
                }
            }

            if(is_ftp) {
                val ftp_base_dir = "/emuiibo/amiibo/" + base_path;
                val client = FTPClient();

                try {
                    val config = FTPClientConfig();
                    client.configure(config);
                    client.connect(ftp_addr, ftp_port);

                    val reply = client.replyCode;
                    if(!FTPReply.isPositiveCompletion(reply)) {
                        this@MainController.showError(MainApplication.ResourceBundle.getString("error.ftpServerRefusedConnection"));
                        client.disconnect();
                        return false;
                    }

                    client.makeDirectory(ftp_base_dir);
                    if(!Utils.ensureFtpDirectory(client, ftp_base_dir)) {
                        this@MainController.showError(MainApplication.ResourceBundle.getString("error.unableToEnsureFTPDir").format(ftp_base_dir, client.replyCode.toString()));
                        client.disconnect();
                        return false;
                    }

                    client.enterLocalPassiveMode();
                    for(file in File(path).listFiles()) {
                        val strm = FileInputStream(file);
                        val ftp_path = ftp_base_dir + "/" + file.name;
                        client.storeFile(ftp_path, strm);
                        strm.close();
                        if(!FTPReply.isPositiveCompletion(client.replyCode)) {
                            this@MainController.showError(MainApplication.ResourceBundle.getString("error.unableToStoreFTPFile").format(ftp_path, client.replyCode.toString()));
                            client.disconnect();
                            return false;
                        }
                    }

                    client.disconnect();
                    return true;
                }
                catch(ex: Exception) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.exceptionOnFTPConnection").format(ex.toString()));
                    client.disconnect();
                    return false;
                }
            }

            return true;
        }
        else {
            return false;
        }
    }

    fun prepare(stage: Stage) {
        this.MainStage = stage;
        println("Resource bundle in controller: ${MainApplication.ResourceBundle.locale}")

        this.OpenedAmiiboNameText.textProperty().addListener(object : ChangeListener<String> {
            override fun changed(a: ObservableValue<out String>, old: String, new: String) {
                if(new.length > 0x14) {
                    this@MainController.OpenedAmiiboNameText.textProperty().set(old);
                    this@MainController.showWarn(MainApplication.ResourceBundle.getString("warn.nameTooLong"));
                }
            }
        });

        this.OpenedAmiiboNameText.setText(MainApplication.ResourceBundle.getString("status.openedAmiiboName"));

        this.StatusLabel.setText(MainApplication.ResourceBundle.getString("status.ready"));

        this.GenerateOneAmiiboSeriesBox.promptText = MainApplication.ResourceBundle.getString("combobox.prompt.selectSeries");
        this.AmiiboBox.promptText = MainApplication.ResourceBundle.getString("combobox.prompt.selectAmiibo");

        val task = object : Task<Map<String, List<AmiiboAPIEntry>>>() {
            public override fun call() : Map<String, List<AmiiboAPIEntry>> {
                this.updateMessage(MainApplication.ResourceBundle.getString("status.loadingAmiiboAPI"));
                return AmiiboAPI.query();
            }

            override fun succeeded() {
                super.succeeded();

                this@MainController.Amiibos = this.getValue();
                val series_names = this@MainController.Amiibos.keys.toMutableList();
                series_names.sort();
                this@MainController.GenerateOneAmiiboSeriesBox.items.setAll(series_names);
                this@MainController.GenerateSeriesAmiiboSeriesBox.items.setAll(series_names);

                this@MainController.StatusLabel.textProperty().unbind();
                this@MainController.StatusLabel.setText(MainApplication.ResourceBundle.getString("status.ready"));
            }

            override fun failed() {
                super.failed();

                this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboApiFailed"));
                this@MainController.StatusLabel.textProperty().unbind();
                this@MainController.StatusLabel.setText(MainApplication.ResourceBundle.getString("status.ready"));
            }

            override fun cancelled() {
                super.cancelled();

                this@MainController.StatusLabel.textProperty().unbind();
                this@MainController.StatusLabel.setText(MainApplication.ResourceBundle.getString("status.ready"));
            }
        };

        this.StatusLabel.textProperty().bind(task.messageProperty());

        val api_thread = Thread(task);
        api_thread.start();

        this.AmiiboOpenButton.setOnAction(object : EventHandler<ActionEvent> {
            override fun handle(event: ActionEvent) {
                val file_chooser = FileChooser();
                file_chooser.setTitle("Open virtual amiibo directory");
                
                val path = file_chooser.showOpenDialog(this@MainController.MainStage);
                if(path == null) {
                    return;
                }

                val status = Amiibo.tryParse(path.absolutePath);
                if(status.first.contains(AmiiboStatusKind.Ok)) {
                    this@MainController.OpenedAmiiboPath = path.absolutePath;
                    this@MainController.OpenedAmiibo = status.second!!;

                    this@MainController.OpenedAmiiboNameText.setDisable(false);
                    this@MainController.OpenedAmiiboUseRandomUuidCheck.setDisable(false);
                    this@MainController.OpenedAmiiboAreaList.setDisable(false);
                    this@MainController.AmiiboSaveButton.setDisable(false);

                    this@MainController.OpenedAmiiboNameText.setText(this@MainController.OpenedAmiibo!!.name);
                    this@MainController.OpenedAmiiboUseRandomUuidCheck.setSelected(this@MainController.OpenedAmiibo!!.use_random_uuid);

                    if(this@MainController.OpenedAmiibo!!.hasAreas()) {
                        val entries = this@MainController.OpenedAmiibo!!.areas!!.areas;
                        val entry_names = mutableListOf<String>();
                        
                        for(entry in entries) {
                            val active_str = if(entry.is_active) {
                                MainApplication.ResourceBundle.getString("checkbox.activeArea");
                            }
                            else "";


                            entry_names.add(entry.program_id.toString(16) + " (" + entry.access_id.toString(16) + ") " + active_str);
                        }

                        this@MainController.OpenedAmiiboAreaList.setItems(javafx.collections.FXCollections.observableArrayList(entry_names));
                    }
                    else {
                        this@MainController.OpenedAmiiboAreaList.setItems(javafx.collections.FXCollections.emptyObservableList<String>());
                    }

                    if(status.first.contains(AmiiboStatusKind.MiiCharInfoNotFound)) {
                        this@MainController.showWarn(MainApplication.ResourceBundle.getString("warn.noMiiCharinfo"));
                    }
                    if(status.first.contains(AmiiboStatusKind.InvalidNameLength)) {
                        this@MainController.showWarn(MainApplication.ResourceBundle.getString("warn.nameTooLong"));
                    }
                }
                else {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.unableToLoadAmiibo").format(status.first.toString()));
                }
            }
        });

        this.AmiiboSaveButton.setOnAction(object : EventHandler<ActionEvent> {
            override fun handle(event: ActionEvent) {
                val path = this@MainController.OpenedAmiiboPath;
                val amiibo = this@MainController.OpenedAmiibo!!;

                val status = amiibo.save(path);
                if(!status) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.unableToSaveAmiibo").format(status.toString()));
                }
                else {
                    this@MainController.showInfo(MainApplication.ResourceBundle.getString("info.successfullySaved"));
                }
            }
        });

        this.OpenedAmiiboNameText.textProperty().addListener(object : ChangeListener<String?> {
            override fun changed(a: ObservableValue<out String?>, old: String?, new: String?) {
                if(new != null && new.length > Amiibo.NameMaxLength) {
                    val str = new.substring(0, Amiibo.NameMaxLength);
                    this@MainController.OpenedAmiiboNameText.setText(str);
                }
            }
        });

        AboutButton.setOnAction {
            try {
                MainApplication.HostServices.showDocument("https://github.com/XorTroll/emuiibo")
            } catch (e: Exception) {
                // Fallback in case HostServices is not available
                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = MainApplication.ResourceBundle.getString("info.title")
                alert.headerText = null
                alert.contentText = "GitHub: https://github.com/XorTroll/emuiibo"
                alert.showAndWait()
            }
        }

        this.GenerateOneAmiiboSeriesBox.selectionModel.selectedItemProperty().addListener(object : ChangeListener<String?> {
            override fun changed(a: ObservableValue<out String?>, old: String?, new: String?) {
                this@MainController.updateSelectedAmiiboSeries();
            }
        });

        this.AmiiboBox.selectionModel.selectedItemProperty().addListener(object : ChangeListener<String?> {
            override fun changed(a: ObservableValue<out String?>, old: String?, new: String?) {
                this@MainController.updateSelectedAmiibo();
            }
        });

        this.AmiiboNameText.textProperty().addListener(object : ChangeListener<String?> {
            override fun changed(a: ObservableValue<out String?>, old: String?, new: String?) {
                if(new!!.length > Amiibo.NameMaxLength) {
                    val str = new.substring(0, Amiibo.NameMaxLength);
                    this@MainController.AmiiboNameText.setText(str);
                }
            }
        });

        this.AmiiboDirectoryText.disableProperty().bind(this.NameAsDirectoryNameCheck.selectedProperty());

        this.GenerateOneFtpAddress.disableProperty().bind(this.GenerateOneFtpCheck.selectedProperty().not());
        this.GenerateOneFtpPort.disableProperty().bind(this.GenerateOneFtpCheck.selectedProperty().not());

        this.GenerateOneButton.setOnAction(object : EventHandler<ActionEvent> {
            override fun handle(event: ActionEvent) {
                this@MainController.cleanTemporaryFtp();

                val amiibo_name = this@MainController.AmiiboNameText.getText() as String;
                if(amiibo_name.isNullOrEmpty()) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboNameNull"));
                    return;
                }

                val amiibo_dir = if(this@MainController.NameAsDirectoryNameCheck.isSelected()) {
                    amiibo_name
                }
                else {
                    this@MainController.AmiiboDirectoryText.getText()
                };
                if(amiibo_dir.isNullOrEmpty()) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboDirNull"));
                    return;
                }
                else if(amiibo_dir.contains("/") || amiibo_dir.contains("\\")) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboDirInvalidChars"));
                    return;
                }

                val is_ftp = this@MainController.GenerateOneFtpCheck.isSelected();
                val ftp_addr = this@MainController.GenerateOneFtpAddress.getText();
                val ftp_port = this@MainController.processFtpPort(is_ftp, this@MainController.GenerateOneFtpPort.getText());
                if(ftp_port == null) {
                    return;
                }

                val use_random_uuid = this@MainController.GenerateOneUseRandomUuidCheck.isSelected();
                val save_image = this@MainController.GenerateOneImageSaveCheck.isSelected();

                val paths = this@MainController.chooseBaseAmiiboPath(is_ftp);
                if(paths != null) {
                    val (path, pres_path) = paths;
                    val base_path = Utils.ensureValidFileDirectoryName(amiibo_dir);

                    val amiibo_path = Paths.get(path, base_path).toString();
                    val pres_amiibo_path = Paths.get(pres_path, base_path).toString();
                    if(this@MainController.showYesNo(MainApplication.ResourceBundle.getString("confirm.generateOne").format(pres_amiibo_path))) {
                        val selected_amiibo = this@MainController.getSelectedAmiibo();
                        if(selected_amiibo != null) {
                            if(this@MainController.generateAmiibo(amiibo_path, base_path, selected_amiibo, amiibo_name, use_random_uuid, save_image, is_ftp, ftp_addr, ftp_port)) {
                                this@MainController.showInfo(MainApplication.ResourceBundle.getString("info.amiiboGenerated"));
                            }
                            else {
                                this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboGenerateFailed"));
                            }
                        }
                        else {
                            this@MainController.showError(MainApplication.ResourceBundle.getString("error.amiiboUpdateFailed"));
                        }
                    }
                }
            }
        });

        this.GenerateAllFtpAddress.disableProperty().bind(this.GenerateAllFtpCheck.selectedProperty().not());
        this.GenerateAllFtpPort.disableProperty().bind(this.GenerateAllFtpCheck.selectedProperty().not());

        this.GenerateAllButton.setOnAction(object : EventHandler<ActionEvent> {
            override fun handle(event: ActionEvent) {
                this@MainController.cleanTemporaryFtp();

                val is_ftp = this@MainController.GenerateAllFtpCheck.isSelected();
                val ftp_addr = this@MainController.GenerateAllFtpAddress.getText();
                val ftp_port = this@MainController.processFtpPort(is_ftp, this@MainController.GenerateAllFtpPort.getText());
                if(ftp_port == null) {
                    return;
                }

                val use_random_uuid = this@MainController.GenerateAllUseRandomUuidCheck.isSelected();
                val save_images = this@MainController.GenerateAllImageSaveCheck.isSelected();

                val paths = this@MainController.chooseBaseAmiiboPath(is_ftp);
                if(paths != null) {
                    val (path, pres_path) = paths;
                    if(this@MainController.showYesNo(MainApplication.ResourceBundle.getString("confirm.generateAll").format(pres_path))) {
                        var success = true;
                        for(series in this@MainController.Amiibos) {
                            val series_name = series.key;
                            val series_amiibos = series.value;
                            
                            for(amiibo in series_amiibos) {
                                val base_path = Utils.ensureValidFileDirectoryName(series_name) + "/" + Utils.ensureValidFileDirectoryName(amiibo.amiibo_name);
                                val amiibo_path = Paths.get(path, base_path).toString();
                                
                                if(!this@MainController.generateAmiibo(amiibo_path, base_path, amiibo, amiibo.amiibo_name, use_random_uuid, save_images, is_ftp, ftp_addr, ftp_port)) {
                                    success = false;
                                    break;
                                }
                            }
                            
                            if(!success) {
                                break;
                            }
                        }
                        
                        if(success) {
                            this@MainController.showInfo(MainApplication.ResourceBundle.getString("info.allAmiibosGenerated"));
                        }
                        else {
                            this@MainController.showError(MainApplication.ResourceBundle.getString("error.couldNotGenerateVirtualAmiibo"));
                        }
                    }
                }
            }
        });

        this.GenerateSeriesAmiiboSeriesBox.selectionModel.selectedItemProperty().addListener(object : ChangeListener<String?> {
            override fun changed(a: ObservableValue<out String?>, old: String?, new: String?) {
                // Just update the selection
            }
        });

        this.GenerateSeriesFtpAddress.disableProperty().bind(this.GenerateSeriesFtpCheck.selectedProperty().not());
        this.GenerateSeriesFtpPort.disableProperty().bind(this.GenerateSeriesFtpCheck.selectedProperty().not());

        this.GenerateSeriesButton.setOnAction(object : EventHandler<ActionEvent> {
            override fun handle(event: ActionEvent) {
                this@MainController.cleanTemporaryFtp();

                val is_ftp = this@MainController.GenerateSeriesFtpCheck.isSelected();
                val ftp_addr = this@MainController.GenerateSeriesFtpAddress.getText();
                val ftp_port = this@MainController.processFtpPort(is_ftp, this@MainController.GenerateSeriesFtpPort.getText());
                if(ftp_port == null) {
                    return;
                }

                val series_name = this@MainController.GenerateSeriesAmiiboSeriesBox.selectionModel.getSelectedItem();
                if(series_name == null) {
                    this@MainController.showError(MainApplication.ResourceBundle.getString("error.noSeriesWasSelected"));
                    return;
                }

                val use_random_uuid = this@MainController.GenerateSeriesUseRandomUuidCheck.isSelected();
                val save_images = this@MainController.GenerateSeriesImageSaveCheck.isSelected();

                val paths = this@MainController.chooseBaseAmiiboPath(is_ftp);
                if(paths != null) {
                    val (path, pres_path) = paths;
                    if(this@MainController.showYesNo(MainApplication.ResourceBundle.getString("confirm.generateSeries").format(pres_path))) {
                        val series_amiibos = this@MainController.Amiibos.get(series_name);
                        if(series_amiibos != null) {
                            var success = true;
                            for(amiibo in series_amiibos) {
                                val base_path = Utils.ensureValidFileDirectoryName(amiibo.amiibo_name);
                                val amiibo_path = Paths.get(path, base_path).toString();
                                
                                if(!this@MainController.generateAmiibo(amiibo_path, base_path, amiibo, amiibo.amiibo_name, use_random_uuid, save_images, is_ftp, ftp_addr, ftp_port)) {
                                    success = false;
                                    break;
                                }
                            }
                            
                            if(success) {
                                this@MainController.showInfo(MainApplication.ResourceBundle.getString("info.seriesAmiibosGenerated"));
                            }
                            else {
                                this@MainController.showError(MainApplication.ResourceBundle.getString("error.couldNotGenerateVirtualAmiibo"));
                            }
                        }
                        else {
                            this@MainController.showError(MainApplication.ResourceBundle.getString("error.couldNotFindSelectedSeries"));
                        }
                    }
                }
            }
        });
    }
}