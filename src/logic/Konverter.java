/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logic;

import boko.FXMLDocumentController;
import dateutil.DateUtilWithTime;
import dateutil.DateUtil;
import dateutil.DateUtilWithDotsTime;
import dateutil.DateUtilDots;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

/**
 *
 * @author Ben
 */
public class Konverter extends TimerTask{
    private final Path inDir,outDir,errorDir,logFile,bak;
    private final String ftpInDir,ftpOutDir, ftpIpAdr, ftpUser, ftpPassword;
    private final boolean ftpOverSSL;
    private final ArrayList<Bordero> borderos = new ArrayList<>();
    private final Pattern onlyNumbers = Pattern.compile("[0-9]+");
    private final Pattern numbers = Pattern.compile("\\d+(\\.\\d+)*");
    private final Pattern onlyzeroes = Pattern.compile("[0]+");
    private final SimpleDateFormat korrekt = new SimpleDateFormat("yyyyMMddHHmm");
    private final SimpleDateFormat korrektOhneUhrzeit = new SimpleDateFormat("yyyyMMdd");
    private final SimpleDateFormat timeInFile = new SimpleDateFormat("HHmm");
    private final ObservableList<Bordero> data;
    private final ProgressIndicator pbBar;
    // static patterns
    private static final String BORDERO_START = "0010IFCSUM";
    private static final String GARANTIE_ZEILE = "0120IDS9";
    private static final String GARANTIE_ZWEINEUN = "0120IDS92";
    private static final String GGVS_ZEILE = "0310F02";
    private final FileOutputStream fout,ferr;
    private final MultiOutputStream multiOut, multiErr;
    private final PrintStream stdout, stderr;
    public Konverter(Path inDir, Path outDir, Path errorDir,Path logFile, Path bak, ObservableList<Bordero> data,String ftpInDir, String ftpOutDir, String ftpIpAdr, boolean ftpOverSSL, String ftpUser, String ftpPassword,ProgressIndicator pbBar) throws FileNotFoundException{
        this.inDir = inDir;
        this.outDir = outDir;
        this.errorDir = errorDir;
        this.logFile = logFile;
        this.data = data;
        this.bak = bak;
        this.ftpInDir = ftpInDir;
        this.ftpOutDir = ftpOutDir;
        this.ftpIpAdr = ftpIpAdr;
        this.ftpOverSSL = ftpOverSSL;
        this.ftpUser = ftpUser;
        this.ftpPassword = ftpPassword;
        this.pbBar = pbBar;
        fout = new FileOutputStream(logFile.toFile(),true);
        ferr = new FileOutputStream(logFile.toFile(),true);
        multiOut= new MultiOutputStream(System.out, fout);
        multiErr= new MultiOutputStream(System.err, ferr);

        stdout= new PrintStream(multiOut);
        stderr= new PrintStream(multiErr);

        System.setOut(stdout);
        System.setErr(stderr);
    }
    @Override
    public void run(){
        try {
            ArrayList <Bordero> localFile = download();
            // Füge die heruntergeladenen Borderos
            // der Liste in der GUI hinzu
            Platform.runLater(() -> {
                pbBar.setProgress(0.00);
                data.addAll(localFile);
            });
            localFile.stream().map((bordero) -> {
                try{
                    konvertiere(bordero);
                    if(bordero.isError())
                    {
                        
                    }
                    else {
                        //Bordero ist konvertiert
                        // Setze Status und aktualsiere die liste
                        bordero.setConverted(true);
                        new Thread(() -> {
                            while(!bordero.isConverted()) {
                                //wait for copy process to complete
                            }
                            refreshList(bordero);
                            
                        }).start();
                    }
                }
                catch (IOException e) {
                    // Ein Fehler ist aufgetreten setze Status
                    // Und aktualisiere die Liste
                    bordero.setErrorMessage("Ein Dateilese Fehler ist aufgetreten bei Bordero "+bordero.getFilename());
                    Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, e);
                    bordero.setError(true,e);
                    refreshList(bordero);

                }
                return bordero;
            }).forEach((bordero) -> {
                try {
                    while(!bordero.isConverted()) {
                        //wait for conversion beeing over
                        Thread.sleep(500);
                    }
                    //than upload file
                    upload(bordero);
                    Platform.runLater(() -> {
                        pbBar.setProgress(1.00*localFile.indexOf(bordero)/localFile.size());
                    });
                } catch (IOException | InterruptedException ex) {
                        Logger.getLogger(Konverter.class.getName()).log(Level.SEVERE, null, ex);
                    }
            });
        } catch (IOException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("b");

        }
        finally {
            try {
                fout.close();
                ferr.close();
                multiOut.close();
                multiErr.close();
            } catch (IOException ex) {
                //Silently donothing
            }
        }
            stdout.close();
            stderr.close();
    }
    private void refreshList(Bordero bordero) {
        Platform.runLater(() -> {
                        int id = data.indexOf(bordero);
                        data.remove(bordero);
                        data.add(id,bordero);
                    });
    }
    /*** Diese Methode wird für jedes Bordero einzeln aufgerufen
     * Idee ist es durch jede Zeile zu laufen und sich die Zeilen anzusehen
     * die eine Garantie oder eine GGVS haben
     ***/
    private void konvertiere(Bordero bordero) throws IOException
    {
        String fileInhalt = "";
        String newInhalt = "";
        // First step is to read the whole file
        InputStream in = new FileInputStream(bordero.getFile());
        BufferedReader buf;
        try {
            buf = new BufferedReader(new InputStreamReader(in,"ISO-8859-1"));
            try {
                char[] buffer = new char[16384];
                StringBuilder fileBuffer = new StringBuilder();
                while(true) {
                    int charCount = buf.read(buffer,0,buffer.length);
                    if(charCount == -1 )break;
                    fileBuffer.append(String.valueOf(buffer,0,charCount));
                }
                fileInhalt = fileBuffer.toString();
                buf.close();
                in.close();
                //System.out.println("Dateiinhalt:\n"+fileInhalt);
                String[] tmp = fileInhalt.split("\n");
                for(int i = 0; i < tmp.length; i++) {
                    //System.out.println("Zeile:"+tmp[i]);
                    if(i==0) {
                        //Enthält die erste Zeile BORDERO_START
                        if(!tmp[i].startsWith(BORDERO_START)){
                           bordero.setErrorMessage("Die Datei ist kein Bordero da "+BORDERO_START+" nicht am Anfang steht.");
                           bordero.setError(true, new Exception());
                           break;
                        }
                    }
                    else {
                        //Ist die Zeile eine Gefahrgut Zeile?
                        if(tmp[i].startsWith(GGVS_ZEILE)) {
                            // Steht eine Gefahrgutanzahl drin?
                            // Wenn keine Anzahl drin steht, kann das Bordero nicht konvertiert werden
                            if(ggvsHandling(tmp[i])) {
                                bordero.setErrorMessage("Die Gefahrgutanzahl war nicht korrekt, da keine Werte eingetragen wurden.\n"
                                        + "Bordero: "+bordero.getFilename()+"\n"
                                        + "Zeile: "+tmp[i]+"\n"
                                        + "Zeilenummer: "+i);
                                bordero.setError(true, new Exception());
                                break;
                            }
                        }
                        // Ist die aktuelle Zeile eine Garantiezeile?
                        if(tmp[i].startsWith(GARANTIE_ZEILE)) {
                            //Es ist eine Garantiezeile
                            // Ist die aktuelle Zeile ein 92er?
                            if(tmp[i].startsWith(GARANTIE_ZWEINEUN)) {
                                // Es ist ein 92er, der braucht kein Datum hier gibt es nichts zu tun
                                System.out.println("Zeile: "+tmp[i]);
                            }
                            else {
                                //Das wären dann die anderen Garantie schlüssel, hier müssen wir prüfen
                                String[] garantieZeile = tmp[i].trim().split(" ");
                                System.out.println("OrigZeile:"+tmp[i]);
                                //Keine Uhrzeit oder Datum zusammen Uhrzeit ( nicht getrennt )
                                if(garantieZeile.length==2) {
                                    if(onlyNumbers.matcher(garantieZeile[1]).matches()) {
                                        //Datum hat keine Punkte
                                        if(garantieZeile[1].length() < 9) {
                                            //Datum hat keine Uhrzeit
                                            tmp[i] =  garantieZeile[0] + " " + korrektOhneUhrzeit.format(DateUtil.convertToDate(garantieZeile[1])) + "0000";
                                        }
                                        else {
                                            //Datum hat Uhrzeit
                                            tmp[i] = garantieZeile[0] + " " + korrekt.format(DateUtilWithTime.convertToDate(garantieZeile[1]));
                                        }
                                    }
                                    else if(numbers.matcher(garantieZeile[1]).matches()) {
                                        //Datum enthält Punkte
                                        if(garantieZeile[1].length() < 11) {
                                            //Datum hat keine Uhrzeit
                                            tmp[i] = garantieZeile[0] + " " + korrektOhneUhrzeit.format(DateUtilDots.convertToDate(garantieZeile[1]));
                                        }
                                        else {
                                            //Datum hat Uhrzeit
                                            tmp[i] = garantieZeile[0] + " " + korrekt.format(DateUtilWithDotsTime.convertToDate(garantieZeile[1]));
                                        }
                                    }
                                    else {
                                        bordero.setErrorMessage("Konnte das Problem mit dem Bordero "+bordero.getFilename()+" nicht beheben.\nFehler in Zeile "+i+"\nZeileninhalt:"+tmp[i]);
                                        bordero.setError(true, new Exception());
                                    }
                                    System.out.println("ConvZeile: "+tmp[i]);
                                }
                                //Datum mit Leerzeichen getrennt von Uhrzeit
                                else if(garantieZeile.length == 3) {
                                    if(onlyNumbers.matcher(garantieZeile[1]).matches()) {
                                        //Datum hat keine Punkte
                                        tmp[i] = garantieZeile[0] + " " + korrektOhneUhrzeit.format(DateUtil.convertToDate(garantieZeile[1])) + garantieZeile[2];
                                    }
                                    else if(numbers.matcher(garantieZeile[1]).matches()) {
                                        //Datum hat Punkte
                                        tmp[i] = garantieZeile[0] + " " + korrektOhneUhrzeit.format(DateUtilDots.convertToDate(garantieZeile[1])) + garantieZeile[2];
                                    }
                                    System.out.println("ConvZeile: "+tmp[i]);
                                }
                                //Kein Datum in der Sendung
                                else if(garantieZeile.length== 1) {
                                    //Setze Datum auf morgen ? !? 
                                    Date now = new Date();
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(now);
                                    cal.add(Calendar.DAY_OF_YEAR, 1);
                                    Date tomorrow = cal.getTime();
                                    tmp[i] = garantieZeile[0] +" " + korrekt.format(tomorrow);
                                    bordero.setWarningMessage("Datum bei '" + garantieZeile[0] + "' nicht vorhanden. Setze auf morgen.\nBordero: '"+bordero.getFilename()+"\nZeile:"+i);
                                }
                                else {
                                    bordero.setErrorMessage("Konnte das Problem mit dem Bordero " +bordero.getFilename()+" nicht beheben.\nDer Fehler ist in Zeile "+i+"\nZeileninhalt:"+tmp[i]);
                                }
                            }
                        }
                        else {
                            //Keine Garantiezeile
                        }
                    }
                }



                /*** 
                 * Setze Datei Inhalt für dieses Bordero fest
                 * Wenn bei dem Bordero ein Fehler aufgetreten ist
                 * Hole orginal inhalt aus variable zurück ( fileInhalt)
                 * Ansonsten setze neuen Dateieninhalt ( newInhalt)
                ***/
                if(bordero.isError()) {
                    bordero.setFileInhalt(fileInhalt);
                }
                else {
                    for(String string : tmp) {
                        newInhalt = newInhalt.concat(string.replaceAll("\r", "").replaceAll("\n", "").concat("\n"));
                    }
                    bordero.setFileInhalt(newInhalt);
                }
            }
            finally {
                buf.close();
            }
        }
        finally {
            in.close();
        }
        //Stream<String> stream
    }
    private ArrayList<Bordero> download() throws IOException
    {
        ArrayList<Bordero> toReturn = new ArrayList<>();
        OutputStream out;
        if(ftpOverSSL) {
            FTPSClient client;
            client = new FTPSClient("SSL");
            client.connect(ftpIpAdr);
            int reply = client.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Verbindungsfehler");
                    alert.setHeaderText("Konnte nicht zum FTP Server verbinden.");
                    alert.setContentText("Server nicht verfügbar und/oder IP Adresse falsch.");

                    alert.showAndWait();
                });
            }
            else {
                if(client.isConnected()) {
                    client.setBufferSize(1000);
                    if(!client.login(ftpUser, ftpPassword)) {
                        client.logout();
                        Platform.runLater(() -> {
                            Alert alert = new Alert(AlertType.ERROR);
                            alert.setTitle("Verbindungsfehler");
                            alert.setHeaderText("Konnte nicht am FTP Server einloggen.");
                            alert.setContentText("Benutzername und/oder Passwort falsch.");

                            alert.showAndWait();
                        });
                    }
                    else {
                        client.enterLocalPassiveMode();
                        client.changeWorkingDirectory(ftpInDir);
                        FTPFile[] files = client.listFiles(ftpInDir);
                        if(files != null && files.length > 0 ) {
                            for(FTPFile file : files) {
                                if(!file.isFile()) {
                                }
                                else {
                                File localFile = new File(inDir.toString()+"/"+file.getName());
                                if(!localFile.exists()) {
                                    boolean createNewFile = localFile.createNewFile();
                                    if(createNewFile) {
                                        Logger.getLogger(Zipper.class.getName()).log(Level.INFO, null, "Konnte Datei "+localFile.getAbsolutePath()+" erzeugen");
                                    }
                                    else {
                                        Logger.getLogger(Zipper.class.getName()).log(Level.WARNING, null, "Konnte Datei "+localFile.getAbsolutePath()+" nicht erzeugen");
                                    }
                                }
                                else {
                                    localFile.delete();
                                    localFile.createNewFile();
                                }
                                out = new FileOutputStream(localFile);
                                Bordero tmp = new Bordero(localFile,this.inDir,this.outDir,this.bak);
                                toReturn.add(tmp);
                                try {
                                    boolean success = client.retrieveFile(file.getName(), out);
                                    if(success) {
                                        client.deleteFile(file.getName());
                                        tmp.setDownloaded(success);
                                        tmp.backupFile();
                                        }
                                    else {
                                        tmp.setError(true, new IOException("Konnte Datei nicht vom FTP Server herunterladen."));
                                    }
                                }
                                finally {
                                    out.close();
                                }
                                }
                            }
                        }
                    }
                }
                client.disconnect();
            }
        }
        else {
            FTPClient client;
            client = new FTPClient();
            try{
            client.connect(ftpIpAdr);
            int reply = client.getReplyCode();
            if(!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Verbindungsfehler");
                    alert.setHeaderText("Konnte nicht zum FTP Server verbinden.");
                    alert.setContentText("Server nicht verfügbar und/oder IP Adresse falsch.");

                    alert.showAndWait();
                });
            }
            else if(!client.login(ftpUser, ftpPassword)) {
                client.logout();
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Verbindungsfehler");
                    alert.setHeaderText("Konnte nicht am FTP Server einloggen.");
                    alert.setContentText("Benutzername und/oder Passwort falsch.");

                    alert.showAndWait();
                });
            }
            else {

                client.enterLocalPassiveMode();
                client.changeWorkingDirectory(ftpInDir);
                FTPFile[] files = client.listFiles();
                if(files != null && files.length > 0) {
                    for(FTPFile file : files)
                    {
                        if(!file.isFile()) {
                            continue;
                        }
                        System.out.println("File: "+file.getName());
                        File localFile = new File(inDir.toString()+"/"+file.getName());
                        if(!localFile.exists())
                        {
                            localFile.createNewFile();
                        }
                        else
                        {
                            localFile.delete();
                            localFile.createNewFile();
                        }

                        out = new FileOutputStream(localFile);
                        boolean success = client.retrieveFile(file.getName(), out);
                        if(success)
                        {
                            client.deleteFile(file.getName());
                            Bordero tmp = new Bordero(localFile,this.inDir,this.outDir,this.bak);
                            tmp.setDownloaded(true);
                            tmp.backupFile();
                            toReturn.add(tmp);
                        }
                        out.close();
                    }
                }
                client.disconnect();
            }
        }
        catch(ConnectException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(AlertType.ERROR);
                alert.setTitle("Verbindungsfehler");
                alert.setHeaderText("Konnte nicht zum FTP Server verbinden.");
                alert.setContentText("Server nicht verfügbar und/oder IP Adresse falsch.");

                alert.showAndWait();
            });
            }
        }
        return toReturn;
    }
    private void upload(Bordero bordero) throws IOException
    {
        FileInputStream in;
        FTPClient client;
        client = new FTPClient();
        client.connect(ftpIpAdr);
        client.login(ftpUser, ftpPassword);
        client.changeWorkingDirectory(ftpOutDir);
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        try{
            in = new FileInputStream(bordero.getFile());
            System.out.println("FileToStore:"+bordero.getFile().getAbsolutePath());
            boolean success = client.storeFile(bordero.getFilename(), in);
            if(!success)
            {
                client.logout();
                in.close();
                client.deleteFile(bordero.getFilename());
                bordero.setErrorMessage("Nach der Konvertierung konnte die Datei nicht nach\n"
                        + ftpIpAdr+" "+ ftpOutDir + "hochgeladen werden bitte manuell dort plazieren.");
                bordero.setError(true,new Exception());
                refreshList(bordero);
            }
            else {
                client.logout();
                in.close();
                bordero.setUploaded(success);
                refreshList(bordero);
                bordero.getFile().delete();

            }
        }
        catch(IOException ex) {
            Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, ex);
        }
        client.disconnect();
    }
    private boolean ggvsHandling(String strLine){
     Pattern pattern = Pattern.compile("[0-9]+");
     strLine = strLine.substring(136).trim();
     if(pattern.matcher(strLine).matches()){
         //Enthält Zahlen
         return Integer.valueOf(strLine) <= 0; //entweder Null oder irgendwas
     }
     else{
         //Keine Zahlen
         return true;
     }
 }
    }