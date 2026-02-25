package com.bureauveritas.modelparser.model.view;

import com.bureauveritas.modelparser.BurpApi;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;
import java.util.regex.Pattern;

public class Table {
    @Getter
    private final JTable jTable;

    public Table(JTable table) {
        jTable = table;
    }

    public Table() {
        this(new JTable());
    }

    public void insertValues(List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            getModel().addRow(new Object[]{value});
        }
    }

    public void insertRows(List<Object[]> rows) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            getModel().addRow(row);
        }
    }

    public int removeSelectedRow() {
        int selectedRow = jTable.getSelectedRow();
        if (selectedRow >= 0) {
            getModel().removeRow(selectedRow);
        }
        return selectedRow;
    }

    public int getRowCount() {
        return jTable.getRowCount();
    }

    public boolean isTableReady(ListSelectionEvent e) {
        return e.getValueIsAdjusting() || jTable.getRowCount() <= 0;
    }

    private DefaultTableModel getModel() {
        return (DefaultTableModel) jTable.getModel();
    }

    public void addColumn(String columnName) {
        getModel().addColumn(columnName);
    }

    public void renameColumn(String oldColumnNamePattern, String newColumnName) {
        for (int i=0; i<jTable.getColumnModel().getColumnCount(); i++) {
            if (Pattern.matches(oldColumnNamePattern,
                jTable.getColumnModel().getColumn(i).getHeaderValue().toString())
            ) {
                renameColumn(i, newColumnName);
                return;
            }
        }
    }

    public void renameColumn(int columnIndex, String newColumnName) {
        jTable.getColumnModel().getColumn(columnIndex).setHeaderValue(newColumnName);
        jTable.getTableHeader().repaint();
    }

    public void clearRows() {
        getModel().setRowCount(0);
    }

    public void clearColumns() {
        getModel().setColumnCount(0);
    }

    public void addRow(Object... row) {
        getModel().addRow(row);
    }

    public void applyFilter(String regex) {
        try {
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(getModel());
            sorter.setRowFilter(RowFilter.regexFilter(regex));
            jTable.setRowSorter(sorter);
        }
        catch (Exception e) {
            BurpApi.getInstance().logging().logToOutput("Failed to apply table filter: " + e.getMessage());
            BurpApi.getInstance().logging().logToError(e);
            jTable.setRowSorter(null);
        }
    }
}
