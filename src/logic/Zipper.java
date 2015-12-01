/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Ben
 */
public class Zipper extends TimerTask{
    private final File backupDir;
    private final SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
    public Zipper(File backupDir) {
        this.backupDir = backupDir;
    }
    @Override
    public void run () {
        System.out.println("Start zipping...");
        File[] files = backupDir.listFiles((File dir, String name) -> {
            String lowercaseName = name.toLowerCase();
            return (!lowercaseName.endsWith(".zip")&& dir.lastModified()>1 * 24 * 60 * 60 * 1000);
        });
        if(null != files && files.length >0) {
            if(Files.exists(backupDir.toPath().resolve("Borderos_"+sdf.format(new Date())+".zip"))) {
                //fos = new FileOutputStream(backupDir.getAbsolutePath()+"/Borderos_"+sdf.format(new Date())+"1.zip");
                addFilesToZip(new File(backupDir.getAbsolutePath()+"/Borderos_"+sdf.format(new Date())+".zip"),files);
            }
            else {
                try {
                    File zipFile = new File(backupDir.getAbsolutePath()+"/Borderos_"+sdf.format(new Date())+".zip");
                    zipFile.createNewFile();
                    addFilesToZip(zipFile,files);
                } catch (IOException ex) {
                    Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
        }
    System.out.println("Zipping ended...");

    }
    private void addFilesToZip(File source, File[] files)
{
    try
    {

        File tmpZip = File.createTempFile(source.getName(), null);
        tmpZip.delete();
        if(!source.renameTo(tmpZip))
        {
            throw new Exception("Could not make temp file (" + source.getName() + ")");
        }
        byte[] buffer = new byte[1024];
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(source));
        try (
            ZipInputStream zin = new ZipInputStream(new FileInputStream(tmpZip))) {
            for (File file : files) {
                try (InputStream in = new FileInputStream(file)) {
                    out.putNextEntry(new ZipEntry(file.getName()));
                    for(int read = in.read(buffer); read > -1; read = in.read(buffer))
                    {
                        out.write(buffer, 0, read);
                    }
                    out.closeEntry();
                }
                file.delete();
            }   for(ZipEntry ze = zin.getNextEntry(); ze != null; ze = zin.getNextEntry())
            {
                out.putNextEntry(ze);
                for(int read = zin.read(buffer); read > -1; read = zin.read(buffer))
                {
                    out.write(buffer, 0, read);
                }
            out.closeEntry();
        }
        }
        finally {
            out.close();
            tmpZip.delete();
        }
    }
    catch(Exception e)
    {
        Logger.getLogger(Zipper.class.getName()).log(Level.SEVERE, null, e);
    }
}
}
