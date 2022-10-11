package modmanager.ui;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class BottomBar extends HBox {
    public ProgressBar progressBar;
    public Label progressLabel;

    public BottomBar(){
        this.getChildren().add(progressBar = new ProgressBar());
        this.getChildren().add(progressLabel = new Label("Apply mods to load"));
        this.setSpacing(10);

        progressBar.setProgress(0);
    }

    public void setProgressString(String progressString){
        progressLabel.setText(progressString);
    }

    public void setProgress(double val){
        progressBar.setProgress(val);
    }
}
