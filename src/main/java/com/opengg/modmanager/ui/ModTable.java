package com.opengg.modmanager.ui;

import com.opengg.modmanager.Mod;
import com.opengg.modmanager.TTModManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class ModTable extends VBox {
    private final TableView<Mod> table;

    public ModTable(){
        table = new TableView<>();
        table.setEditable(true);

        var enabled = new TableColumn<Mod, Boolean>();
        enabled.setCellFactory(CheckBoxTableCell.forTableColumn(enabled));
        enabled.setCellValueFactory(param -> param.getValue().loaded());
        enabled.setEditable(true);
        enabled.setMinWidth(40);
        enabled.setMaxWidth(40);
        enabled.setResizable(false);
        enabled.setReorderable(false);
        enabled.setStyle("-fx-table-cell-border-color: transparent;");

        var name = new TableColumn<Mod, String>("Mod Name");
        name.setCellValueFactory(new PropertyValueFactory<>("name"));

        var author = new TableColumn<Mod, String>("Mod Author");
        author.setCellValueFactory(new PropertyValueFactory<>("author"));

        var version = new TableColumn<Mod, String>("Version");
        version.setCellValueFactory(new PropertyValueFactory<>("version"));

        var source = new TableColumn<Mod, String>("Source File");
        source.setCellValueFactory(new PropertyValueFactory<>("sourceFile"));

        table.getColumns().addAll(List.of(enabled, name, author, version, source));

        table.setPlaceholder(new Label("No mods loaded, press the \"Add folder/archive\" button to the right to get started!"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.prefHeightProperty().bind(this.heightProperty());
        table.prefWidthProperty().bind(this.widthProperty());

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                TTModManager.CURRENT.bottomPane.modPane.setMod(newSelection);
            }
        });

        this.getChildren().add(table);
    }

    public void setModList(List<Mod> mods){
        table.getItems().clear();
        table.getItems().addAll(mods);
    }

    public Mod getSelectedMod() {
        return table.getSelectionModel().getSelectedItem();
    }
}
