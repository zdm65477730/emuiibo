package com.xortroll.emuiibo.emuiigen.ui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import java.util.Locale
import java.util.ResourceBundle
import java.io.File
import java.io.InputStream
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javafx.scene.image.Image
import java.util.Properties
import javafx.scene.layout.Pane

class MainApplication : Application() {
    private lateinit var controller: MainController;

    companion object {
        lateinit var ResourceBundle: java.util.ResourceBundle

        lateinit var Description: String
        
        lateinit var Version: String

        lateinit var HostServices: javafx.application.HostServices

        @JvmStatic
        fun main(args: Array<String>) {
            val langEnv = System.getenv("LANG")
            val languageEnv = System.getenv("LANGUAGE")
            var locale: Locale? = null

            if (langEnv != null) {
                when {
                    langEnv.startsWith("zh_TW") || langEnv.startsWith("zh-HK") || langEnv.startsWith("zh_HK") -> {
                        println("Detected Traditional Chinese locale from LANG")
                        locale = Locale.TRADITIONAL_CHINESE
                    }
                    langEnv.startsWith("zh_CN") || langEnv.startsWith("zh") -> {
                        println("Detected Simplified Chinese locale from LANG")
                        locale = Locale.SIMPLIFIED_CHINESE
                    }
                    langEnv.startsWith("ja") -> {
                        println("Detected Japanese locale from LANG")
                        locale = Locale.JAPANESE
                    }
                    langEnv.startsWith("ko") -> {
                        println("Detected Korean locale from LANG")
                        locale = Locale.KOREAN
                    }
                }
            }

            if (locale == null && languageEnv != null) {
                when {
                    languageEnv.startsWith("zh_TW") || languageEnv.startsWith("zh-HK") || languageEnv.startsWith("zh_HK") -> {
                        println("Detected Traditional Chinese locale from LANGUAGE")
                        locale = Locale.TRADITIONAL_CHINESE
                    }
                    languageEnv.startsWith("zh_CN") || languageEnv.startsWith("zh") -> {
                        println("Detected Simplified Chinese locale from LANGUAGE")
                        locale = Locale.SIMPLIFIED_CHINESE
                    }
                    languageEnv.startsWith("ja") -> {
                        println("Detected Japanese locale from LANGUAGE")
                        locale = Locale.JAPANESE
                    }
                    languageEnv.startsWith("ko") -> {
                        println("Detected Korean locale from LANGUAGE")
                        locale = Locale.KOREAN
                    }
                }
            }

            if (locale == null) {
                println("Using system default locale: ${Locale.getDefault()}")
                locale = Locale.getDefault()
            }
            
            println("Selected locale: $locale")
            
            try {
                ResourceBundle = java.util.ResourceBundle.getBundle("messages", locale)
                println("Loaded bundle: ${ResourceBundle.locale}")
            } catch (e: Exception) {
                println("Exception loading bundle: $e")
                ResourceBundle = java.util.ResourceBundle.getBundle("messages", Locale.ENGLISH)
                println("Loaded default bundle: ${ResourceBundle.locale}")
            }
            
            Application.launch(MainApplication::class.java, *args);
        }
    }

    override fun start(primaryStage: Stage) {
        println("FXML resource bundle locale: ${ResourceBundle.locale}")
        HostServices = this.getHostServices()

        val this_loader = this::class.java.classLoader;
        val fxml_loader = FXMLLoader(this_loader.getResource("main.fxml"));
        fxml_loader.resources = ResourceBundle;
        val base = fxml_loader.load<Pane>();
        this.controller = fxml_loader.getController() as MainController;

        val main_props = Properties();
        main_props.load(this_loader.getResourceAsStream("main.properties"));
        Version = main_props.getProperty("version");
        
        val width = base.prefWidth;
        val height = base.prefHeight;
        Description = try {
            ResourceBundle.getString("app.description")
        } catch (e: Exception) {
            "emuiibo's virtual amiibo PC utility"
        }
        primaryStage.title = "emuiigen v" + Version + " - " + Description;
        primaryStage.icons.add(Image(this_loader.getResource("icon.png").toExternalForm()));
        primaryStage.scene = Scene(base, width, height);
        primaryStage.scene.stylesheets.add(this_loader.getResource("main.css").toExternalForm());
        primaryStage.minWidth = width;
        primaryStage.minHeight = height;
        primaryStage.isResizable = false;

        this.controller.prepare(primaryStage);
        primaryStage.show();
    }
}