/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boko;

import insidefx.undecorator.Undecorator;
import insidefx.undecorator.UndecoratorScene;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Nightstalker
 */
public class BoKo extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));
        Region roota = (Region) root;
        final UndecoratorScene undecoratorScene = new UndecoratorScene(stage,roota);
        
        undecoratorScene.setFadeInTransition();
        stage.setOnCloseRequest((WindowEvent we) -> {
            we.consume();
            undecoratorScene.setFadeOutTransition();
        });
        Image image = new Image(getClass().getResource("bokologo.png").toURI().toString());
        stage.getIcons().addAll(image);
        stage.setScene(undecoratorScene);
        stage.sizeToScene();
        
        Undecorator undecorator = undecoratorScene.getUndecorator();
        stage.setMinWidth(undecorator.getMinWidth());
        stage.setMinHeight(undecorator.getMinHeight());
        stage.setTitle("Bordero Konverter");
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
