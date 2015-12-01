/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Ben
 */

public class Bordero{
    private final Path bak,in,out;
    private File file;
    private File backup;
    private String filename,fileInhalt;
    private final double fileSize;
    private final BooleanProperty uploaded;
    private final BooleanProperty converted;
    private final BooleanProperty downloaded;
    private final BooleanProperty error;
    private final BooleanProperty backuped;
    private String errorMessage;
    private String warningMessage;
    private final ObjectProperty<LocalDateTime> date;
    public Bordero (File file,Path in, Path out, Path bak) throws IOException {
        this.file = file;
        filename = file.getName();
        fileSize = Files.size(file.toPath());
        uploaded = new SimpleBooleanProperty();
        downloaded = new SimpleBooleanProperty();
        converted = new SimpleBooleanProperty();
        error = new SimpleBooleanProperty();
        backuped = new SimpleBooleanProperty();
        date = new SimpleObjectProperty();
        date.setValue(LocalDateTime.now());
        errorMessage = "";
        fileInhalt = "";
        warningMessage = "";
        this.bak = bak;
        this.out = out;
        this.in = in;
    }
    public File getFile() {
        return file;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getFileSize() {
        return fileSize;
    }

    public boolean isUploaded() {
        return uploaded.get();
    }

    public boolean isConverted() {
        return converted.get();
    }

    public boolean isDownloaded() {
        return downloaded.get();
    }
    
    public boolean isBackuped() {
        return backuped.getValue();
    }
    
    public void setBackuped(Boolean backuped) {
        this.backuped.setValue(backuped);
    } 
    
    public BooleanProperty uploadedProperty() {
        return uploaded;
    }
    
    public BooleanProperty downloadedProperty() {
        return downloaded;
    }
    
    public BooleanProperty errorProperty() {
        return error;
    }
    
    public BooleanProperty convertedProperty() {
        return converted;
    }
    
    public boolean isError() {
        return error.get();
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
    
    public void setUploaded(boolean uploaded) {
        this.uploaded.setValue(uploaded);
        sendMail(getFile().getAbsolutePath(),"Bordero '"+getFilename() + "' konvertiert","Der Fehler\n"
        + "im Bordero konnte behoben werden\nWarnungen:"+getWarningMessage(),"teni-edv-technik@noerpel.de");
    }

    public File getBackup() {
        return backup;
    }

    public void setBackup(File backup) {
        this.backup = backup;
    }

    public String getFileInhalt() {
        return fileInhalt;
    }

    public void setFileInhalt(String fileInhalt) {
        this.fileInhalt = fileInhalt;
    }

    public Path getBak() {
        return bak;
    }

    public Path getIn() {
        return in;
    }

    public Path getOut() {
        return out;
    }
    
    public void setConverted(boolean converted) {
        (new Thread( () -> {
            try {
                Files.deleteIfExists(Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("out").toString(),"/",getFilename()));
                Files.deleteIfExists(getFile().toPath());
                Path a = Files.createFile(Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("out").toString(),"/",getFilename()));
                try (BufferedWriter outa = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(a.toFile().getAbsolutePath()),"ISO-8859-1"))) {
                    outa.write(getFileInhalt());
                    outa.close();
                    this.file = a.toFile();
                }
                this.converted.setValue(converted);
            } catch (IOException ex) {
                Logger.getLogger(Bordero.class.getName()).log(Level.SEVERE, null, ex);
            }
        })).start();
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded.setValue(downloaded);
    }
    
    public boolean backupFile() {
        try {
            Files.deleteIfExists(Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("backup").toString(),"/",getFilename()));
            Path a = Files.copy(getFile().toPath(), Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("backup").toString(),"/",getFilename()));
            boolean renameTo = file.renameTo(a.toFile());
            if(renameTo) {
                this.backuped.setValue(true);
            }
            else {
                setError(true,new Exception("Konnte Datei nicht in das Backup VZ kopieren :-( "));
            }
        } catch (IOException ex) {
            Logger.getLogger(Bordero.class.getName()).log(Level.SEVERE, null, ex);
        }
        return isBackuped();
    }
    
    public void setError(boolean error,Exception e) {
        this.error.setValue(error);
        try {
            Files.deleteIfExists(Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("error").toString(),"/",getFilename()));
            Path a = Files.move(getFile().toPath(), Paths.get(getFile().getParentFile().getParentFile().toPath().resolve("error").toString(),"/",getFilename()));
            this.file = a.toFile();
        } catch (IOException ex) {
            Logger.getLogger(Bordero.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Bordero: "+getFilename()+"|"+getErrorMessage());
        Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, e);
        sendMail(getFile().getAbsolutePath(),"Konnte Datei nicht konvertieren","Leider konnte der Fehler\n"
        + "im Bordero nicht behoben werden\n"+getErrorMessage()+"\nException:"+e.getMessage(),"Benjamin.Holdermann@noerpel.de");
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = this.errorMessage.concat(errorMessage);
    }

    public ObjectProperty<LocalDateTime> getDate() {
        return date;
    }
    public String getTime() {
        return date.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.filename);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Bordero other = (Bordero) obj;
        return Objects.equals(this.filename, other.filename);
    }
    private boolean sendMail(String attachment, String subject, String fileread, String to) {
        //Erzeuge neue Variable für die Text nachricht
       String text;
       //Solange i kleiner als die Listenlänge ist
       //hänge Listenelement i an die Nachricht an mit einem Zeilenunbruch
      text = fileread;
      // Sender's email ID needs to be mentioned
      String from = "Boko@noerpel.de";

      // Assuming sending email from localhost
      String host = "192.168.0.145";

      // Get system properties
      Properties properties = System.getProperties();

      // Setup mail server
      properties.setProperty("mail.smtp.host", host);
      

      // Get the default Session object.
      Session session = Session.getInstance(properties);

      try{
         // Create a default MimeMessage object.
         MimeMessage message = new MimeMessage(session);

         // Set From: header field of the header.
         message.setFrom(new InternetAddress(from));

         // Set To: header field of the header.
         message.addRecipient(Message.RecipientType.TO,
                                  new InternetAddress(to));

         // Set Subject: header field
         
         message.setSubject("Boko: "+subject);
         Multipart multipart = new MimeMultipart();
         // Now set the actual message
         MimeBodyPart messageBody = new MimeBodyPart();
         messageBody.setText(text);
         multipart.addBodyPart(messageBody);
         //Add attachment
         messageBody = new MimeBodyPart();
         DataSource source = new FileDataSource(attachment);
         messageBody.setDataHandler(new DataHandler(source));
         messageBody.setFileName(attachment);
         
         message.setContent(multipart);
         // Send message
         Transport.send(message);
         System.out.println("Sent message successfully....");
      }catch (MessagingException mex) {
         Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, mex);
         return false;
      }
            return true;
        }
}
