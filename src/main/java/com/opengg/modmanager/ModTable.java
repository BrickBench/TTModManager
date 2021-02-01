package com.opengg.modmanager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

public class ModTable extends JTable {
    private DefaultTableModel model;

    private static ImageIcon folderIcon;
    private static ImageIcon zipIcon;

    public ModTable(){
        super(new MyTableModel());

        folderIcon = new ImageIcon("resource/folder.png");
        zipIcon = new ImageIcon("resource/zip.png");

        model = (DefaultTableModel) this.getModel();
    }

    public void setModList(List<Mod> mods){
        if (getRowCount() > 0) {
            for (int i = getRowCount() - 1; i > -1; i--) {
                model.removeRow(i);
            }
        }

        for(var mod : mods){
            var icon = switch (mod.getType()){
                case ZIP -> "Zipped File";
                case FOLDER -> "Directory";
            };

            model.addRow(new Object[]{icon, mod.getPath(), mod.isLoaded()});
        }
    }


    public static class MyTableModel extends DefaultTableModel {
        public MyTableModel() {
            super(new String[]{"Type", "Mod Path", "Active"}, 0);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class;
                case 1 -> String.class;
                case 2 -> Boolean.class;
                default -> ImageIcon.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (aValue instanceof Boolean bVal && column == 2) {
                Vector rowData = getDataVector().get(row);
                rowData.set(2, aValue);

                var mod = TTModManager.CURRENT.getLoadedMods().get(row);
                mod.setLoaded(bVal);
                fireTableCellUpdated(row, column);
            }
        }
    }

    public Mod getSelectedMod(){
        if(this.getSelectedRow() >= 0 && this.getSelectedRow() < this.getRowCount()){
            return TTModManager.CURRENT.getLoadedMods().get(this.getSelectedRow());
        }else{
            return null;
        }
    }
}
