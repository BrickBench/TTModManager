package modmanager.ui;

import modmanager.Mod;
import modmanager.ModManager;
import modmanager.TTModManager;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;

public class ModTable extends VBox {
    private final TableView<Mod> table;

    public ModTable(){
        table = new TableView<>();
        table.setEditable(true);

        var enabled = new TableColumn<Mod, Boolean>();
        enabled.setCellFactory(CheckBoxTableCell.forTableColumn(enabled));
        enabled.setCellValueFactory(param -> param.getValue().enabled());
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

        var order = new TableColumn<Mod, Integer>("Load Order");
        order.setCellValueFactory(new PropertyValueFactory<>("modOrder"));
        order.setMinWidth(40);
        order.setMaxWidth(100);
        order.setResizable(false);
        order.setReorderable(false);

        table.getColumns().addAll(List.of(enabled, name, author, version, source, order));

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

    public void setConflicts(List<ModManager.ConflictEntry> conflicts){
        var worstConflict = new HashMap<String, ModManager.ConflictEntry>();

        for(var conflict : conflicts){
            switch (conflict.type()){
                case CONFLICTING_FILES -> {
                    if(worstConflict.get(conflict.conflicting().id()) == null ||
                            (worstConflict.get(conflict.conflicting().id()).type() != ModManager.ConflictEntry.Type.DEPENDENT_DISABLED &&
                              worstConflict.get(conflict.conflicting().id()).type() != ModManager.ConflictEntry.Type.MISSING_DEPENDENCY)){
                        worstConflict.put(conflict.conflicting().id(), conflict);
                    }
                }
                case DEPENDENT_DISABLED -> {
                    if(worstConflict.get(conflict.conflicting().id()) == null ||
                            worstConflict.get(conflict.conflicting().id()).type() != ModManager.ConflictEntry.Type.MISSING_DEPENDENCY){
                            worstConflict.put(conflict.conflicting().id(), conflict);
                    }
                }
                case MISSING_DEPENDENCY -> worstConflict.put(conflict.conflicting().id(), conflict);
            }
        }

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            public void updateItem(Mod item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null) {
                    setStyle("");
                } else if (worstConflict.containsKey(item.id())) {
                    switch (worstConflict.get(item.id()).type()) {
                        case CONFLICTING_FILES -> setStyle("-fx-background-color: yellow;");
                        case DEPENDENT_DISABLED -> setStyle("-fx-background-color: orange;");
                        case MISSING_DEPENDENCY -> setStyle("-fx-background-color: red;");
                    }
                } else {
                    setStyle("");
                }
            }
        });
        table.refresh();
    }
}
