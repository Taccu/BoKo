/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boko;


import logic.Zipper;
import logic.BorderoListCell;
import logic.Bordero;
import logic.Konverter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Nightstalker
 */
public class FXMLDocumentController implements Initializable {
    private static final Logger logger = Logger.getLogger("boko");
    private Path dirIn, dirOut, dirError, log, bak;
    private int repeat;
    private boolean enhanced;
    private Timer konvertJob;
    private Timer zipperJob;
    private String ftpInDir, ftpOutDir, ftpIpAdr, ftpUser, ftpPassword;
    private boolean ftpOverSSL;
    private Path confFile;
    @FXML
    private AnchorPane anPane;
    @FXML
    private Button startButton;
    @FXML
    private Button configButton;
    @FXML
    private Button zipButton;
    @FXML
    private ProgressIndicator pbBar;
    ObservableList<Bordero> data = FXCollections.observableArrayList();
    @FXML
    private ListView<Bordero> jobList;
    @FXML
    private void handleOpenConfig(ActionEvent event) {
        if(Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().edit(confFile.toFile());
            } catch (IOException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                
            }
        }
    }
    @FXML
    private void handleZipStart(ActionEvent event) {
        if(zipperJob != null) {
            zipperJob.cancel();
            zipperJob = null;
            zipButton.setText("Start");
        }
        else {
            zipperJob = new Timer();
            zipperJob.scheduleAtFixedRate(new Zipper(bak.toFile()), 0, 12000L*repeat);
            zipButton.setText("Stop");
        }
    }
    @FXML
    private void handleButtonAction(ActionEvent event) throws FileNotFoundException {
        if(konvertJob != null)
        {
            konvertJob.cancel();
            konvertJob = null;
            startButton.setText("Start");
        }
        else
        {
            konvertJob = new Timer();
            konvertJob.scheduleAtFixedRate(new Konverter(dirIn,dirOut,dirError,log,bak,data,ftpInDir,ftpOutDir,ftpIpAdr,ftpOverSSL,ftpUser,ftpPassword,pbBar), 0, 1000L*repeat);
            startButton.setText("Stop");
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        jobList.setCellFactory((final ListView<Bordero> param) -> new BorderoListCell());
        jobList.setItems(data);
        confFile = Paths.get("Config.xml");
        if(confFile == null)
        {
            logger.log(Level.SEVERE, "Couldn't find Config.xml");
            logger.log(Level.SEVERE, "Exiting....");
            throw new RuntimeException("Couldn't find Config.xml");
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            logger.log(Level.SEVERE, "Couldn't parse Config.xml :-(");
            throw new RuntimeException("Couldn't find Config.xml");
        }
        try {
            Document doc = dBuilder.parse(confFile.toFile());
            dirIn = Files.createDirectories(
                    new File(doc.getElementsByTagName("in").item(0).getTextContent()).toPath());
            
            dirOut = Files.createDirectories(
                    new File(doc.getElementsByTagName("out").item(0).getTextContent()).toPath());
            
            dirError = Files.createDirectories(
                    new File(doc.getElementsByTagName("error").item(0).getTextContent()).toPath());
            repeat = Integer.valueOf(doc.getElementsByTagName("repeat").item(0).getTextContent());
            log = new File(doc.getElementsByTagName("log").item(0).getTextContent()).toPath();
            bak = Files.createDirectories(new File(doc.getElementsByTagName("bak").item(0).getTextContent()).toPath());
            ftpOverSSL = Boolean.valueOf(doc.getElementsByTagName("ssl").item(0).getTextContent());
            ftpInDir = doc.getElementsByTagName("indir").item(0).getTextContent();
            ftpOutDir = doc.getElementsByTagName("outdir").item(0).getTextContent();
            ftpIpAdr = doc.getElementsByTagName("ip").item(0).getTextContent();
            ftpUser = doc.getElementsByTagName("user").item(0).getTextContent();
            ftpPassword = doc.getElementsByTagName("password").item(0).getTextContent();
            
        } catch (SAXException ex) {
            logger.log(Level.SEVERE, "Couldn't parse Config.xml :-(");
            throw new RuntimeException("Couldn't find Config.xml");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Couldn't parse Config.xml :-(\nThere was an IOException");
            Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("Couldn't find Config.xml");
        }
    }    
}
