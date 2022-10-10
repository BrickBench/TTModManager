package modmanager.ui;

import modmanager.Mod;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ModPane extends GridPane {
    private Label name = new Label();
    private Label author = new Label();
    private Label version = new Label();
    private Label description = new Label();
    private Label source = new Label();

    public ModPane(){
        var infoColumn = new ColumnConstraints();
        infoColumn.setPercentWidth(50);
        infoColumn.setFillWidth(true);

        var row = new RowConstraints();
        row.setVgrow(Priority.ALWAYS);
        row.setPercentHeight(25);
        row.setFillHeight(true);

        var descColumn = new ColumnConstraints();
        descColumn.setPercentWidth(50);
        descColumn.setFillWidth(true);

        description.setWrapText(true);

        name.setStyle("-fx-font-weight: bold;");

        this.getColumnConstraints().addAll(infoColumn, descColumn);
        this.getRowConstraints().addAll(row,row,row,row);
        this.setPadding(new Insets(10, 10, 10, 10));
        this.add(name, 0,0,1,1);
        this.add(author, 0, 1, 1, 1);
        this.add(version, 0,2, 1, 1);
        this.add(source, 0,3,1,1);
        this.add(description, 1,0,1,4);

    }

    public void setMod(Mod mod){
        this.name.setText(mod.name());
        this.author.setText("Made by " + mod.author());
        this.version.setText("Version " + mod.version());
        this.description.setText(mod.description());
        this.source.setText(mod.sourceFile().toString());

    }
}
