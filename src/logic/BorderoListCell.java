/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package logic;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Ben
 */
public class BorderoListCell extends ListCell<Bordero>{
    @Override
    public void updateItem(final Bordero bordero, boolean empty){
        super.updateItem(bordero, empty);
        if(empty){
            setText(null);
            setGraphic(null);
        }
        else{
            setText(null);
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(4);
            grid.setPadding(new Insets(0,5,0,5));
            //create labels
            Label dlLabel = new Label("downloaded");
            Label upLabel = new Label("uploaded");
            Label cvLabel = new Label("converted");
            Label baLabel = new Label("backuped");
            Label erLabel = new Label("error");
            //Set tooltips
            dlLabel.setTooltip(new Tooltip("Server: 172.16.2.76\nVerzeichnis:/Teningen/Senden/IDS/Borderofehler"));
            upLabel.setTooltip(new Tooltip("Server: 172.16.2.76\nVerzeichnis:/Teningen/Senden/IDS"));
            cvLabel.setTooltip(new Tooltip("Verzeichnis: "+bordero.getOut().toString()));
            baLabel.setTooltip(new Tooltip("Verzeichnis: "+bordero.getBak().toString()));
            
            Label name = new Label(bordero.getFilename());
            grid.add(name, 2, 0);
            CheckBox downloaded,uploaded,converted,error,backuped;
            downloaded = new CheckBox("");
            downloaded.setSelected(bordero.isDownloaded());
            uploaded = new CheckBox("");
            uploaded.setSelected(bordero.isUploaded());
            converted = new CheckBox("");
            converted.setSelected(bordero.isConverted());
            error = new CheckBox("");
            error.setSelected(bordero.errorProperty().getValue());
            backuped = new CheckBox("");
            backuped.setSelected(bordero.isBackuped());
            
            //Disable all Checkboxes
            error.setDisable(true);
            converted.setDisable(true);
            uploaded.setDisable(true);
            downloaded.setDisable(true);
            backuped.setDisable(true);
            
            Label date = new Label(bordero.getTime());
            grid.add(date,3,0);
            
            //order the shit
            grid.add(downloaded, 2, 2);
            grid.add(dlLabel,2,1);
            grid.add(converted,3,2);
            grid.add(cvLabel,3,1);
            grid.add(uploaded,4,2);
            grid.add(upLabel,4,1);
            grid.add(erLabel,5,1);
            grid.add(error,5,2);
            grid.add(backuped,6,2);
            grid.add(baLabel,6,1);
            setGraphic(grid);
        }
    }
}
