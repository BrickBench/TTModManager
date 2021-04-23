package modmanager.ui;

import modmanager.Mod;
import modmanager.ModManager;
import modmanager.ModSorter;
import modmanager.TTModManager;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.util.List;
import java.util.stream.Collectors;

public class RightPane extends BorderPane {
    private final ListView<String> sourcesList;

    public RightPane(){

        var launchGrid = new LaunchGrid();

        this.setTop(launchGrid);

        var center = new VBox();
        sourcesList = new ListView<>();
        sourcesList.prefHeightProperty().bind(center.heightProperty());

        var sourceScroll = new ScrollPane(sourcesList);
        sourceScroll.setFitToWidth(true);
        sourceScroll.setFitToHeight(true);
        sourceScroll.prefHeightProperty().bind(center.heightProperty());

                center.getChildren().add(new Label("Sources"));
        center.getChildren().add(sourceScroll);

        this.setCenter(center);

        var addModButton = new Button("Add folder");
        addModButton.setOnAction(a -> {
            var chooser = new DirectoryChooser();
            var file = chooser.showDialog(TTModManager.CURRENT.stage);

            if(file != null) {
                ModManager.addNewMod(file);
                ModSorter.sortMods(ModManager.getLoadedMods());
            }
        });
        addModButton.setTooltip(new Tooltip("Add a mod source to the mod list."));

        var addModArchiveButton = new Button("Add archive");
        addModArchiveButton.setOnAction(a -> {
            var chooser = new FileChooser();
            var file = chooser.showOpenDialog(TTModManager.CURRENT.stage);

            if(file != null) {
                ModManager.addNewMod(file);
                ModSorter.sortMods(ModManager.getLoadedMods());

            }
        });
        addModArchiveButton.setTooltip(new Tooltip("Add a mod source to the mod list."));

        var removeMod = new Button("Remove");
        removeMod.setOnAction(a -> {
            var source = sourcesList.getSelectionModel().getSelectedItem();
            if(source != null){
                BottomPane.log("Removing mod source " + source);
                ModManager.getLoadedMods().removeIf(m -> m.sourceFile().equals(source));
                ModManager.refreshModList();
                ModManager.writeModList();
                this.refreshSourceList(ModManager.getLoadedMods());
            }
        });
        removeMod.setTooltip(new Tooltip("Removes the selected mod source from the mod list."));

        var bottom = new HBox();
        bottom.getChildren().add(addModButton);
        bottom.getChildren().add(addModArchiveButton);
        bottom.getChildren().add(removeMod);

        this.setBottom(bottom);
    }

    public void refreshSourceList(List<Mod> mods){
        var sources = mods.stream().map(Mod::sourceFile).distinct().collect(Collectors.toList());
        sourcesList.getItems().clear();
        sourcesList.getItems().addAll(sources);
    }


}
