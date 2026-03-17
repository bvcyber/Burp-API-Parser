package com.bureauveritas.modelparser.view;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.HttpRequestUtils;
import com.bureauveritas.modelparser.control.SendToToolHandler;
import com.bureauveritas.modelparser.control.file.handler.iModelFileHandler;
import com.bureauveritas.modelparser.control.file.serializer.AbstractSerializer;
import com.bureauveritas.modelparser.control.file.serializer.SerializerType;
import com.bureauveritas.modelparser.model.OpenModelFile;
import com.bureauveritas.modelparser.model.view.ShapeNameStack;
import com.bureauveritas.modelparser.view.component.EnterKeyAdapter;
import com.bureauveritas.modelparser.model.view.Table;
import com.bureauveritas.modelparser.view.component.JTableImmutable;
import com.bureauveritas.modelparser.view.component.JTextFieldWithPlaceholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class ModelViewTab {
    private final JTabbedPane parentTabbedPane;
    private JLabel labelTopMessage;
    private JButton buttonSelectFile;
    private JTextField textFieldOpenFile;
    private JSplitPane splitPaneLists;
    private JTable tableOperations;
    private JComboBox<SendToToolHandler> comboBoxSendToTool;
    private JButton buttonSendToTool;
    private JSplitPane splitPaneDefinitionsRequest;
    private JSplitPane splitPaneOperationsShapes;
    private JLabel labelOperationName;
    private JComponent componentOperation;
    private JComboBox<String> comboBoxShapeName;
    private JButton buttonPrev;
    private JComponent componentShape;
    private JSplitPane splitPaneHttpRequest;
    private JTextField textFieldHost;
    private JTable tableCustomHeaders;
    private JButton buttonRemoveCustomHeader;
    private JTextField textFieldCustomHeaderValue;
    private JTextField textFieldCustomHeader;
    private JButton buttonAddCustomHeader;
    private JTable tableVariables;
    private JButton buttonRemoveVariable;
    private JTextField textFieldVariable;
    private JTextField textFieldVariableValue;
    private JButton buttonAddVariable;
    private JComboBox<String> comboBoxIncludeOptionalParameters;
    private JButton buttonRefreshHttpRequest;
    private JComboBox<AbstractSerializer<?>> comboBoxSelectSerializerView;
    private JComponent componentOperationSerializerView;
    private JPanel parentRoot;
    private JTextField textFieldFilterOperations;
    private JButton buttonFilterOperations;
    private JTextField textFieldFilterShapes;
    private JButton buttonFilterShapes;
    private JComboBox<String> comboBoxHost;
    private JComboBox<String> comboBoxAuth;
    private final WebSocketMessageEditor burpComponentOperation =
        BurpApi.getInstance().userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY);
    private final WebSocketMessageEditor burpComponentShape =
        BurpApi.getInstance().userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY);
    private final HttpRequestEditor burpComponentHttpRequestEditor =
        BurpApi.getInstance().userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
    private Table operationsTableWrapper;
    private Table customHeadersTableWrapper;
    private Table variablesTableWrapper;
    private final ShapeNameStack previousShapeNameStack = new ShapeNameStack();
    private iModelFileHandler modelFileHandler;
    private final OpenModelFile openModelFileHandler = new OpenModelFile();

    private ModelViewTab(JTabbedPane tabbedPane) {
        $$$setupUI$$$();

        parentTabbedPane = tabbedPane;

        comboBoxSendToTool.addItem(SendToToolHandler.REPEATER);
        comboBoxSendToTool.addItem(SendToToolHandler.INTRUDER);
        comboBoxSendToTool.addItem(SendToToolHandler.ORGANIZER);
        comboBoxSendToTool.addItem(SendToToolHandler.SITEMAP);
        comboBoxSendToTool.addItem(SendToToolHandler.SENDREQUEST);

        comboBoxIncludeOptionalParameters.addItem("Yes");
        comboBoxIncludeOptionalParameters.addItem("No (experimental)");

        labelOperationName.putClientProperty("html.disable", Boolean.FALSE);

        splitPaneOperationsShapes.setDividerLocation(0.5);
        splitPaneOperationsShapes.setResizeWeight(0.5);
        splitPaneLists.setDividerLocation(0.2);
        splitPaneLists.setResizeWeight(0.2);
        splitPaneDefinitionsRequest.setDividerLocation(0.5);
        splitPaneDefinitionsRequest.setResizeWeight(0.5);
        splitPaneHttpRequest.setDividerLocation(0.9);
        splitPaneHttpRequest.setResizeWeight(0.9);

        tableOperations.getSelectionModel().addListSelectionListener(this::updateOperationDefinitionBySelectedOperation);

        comboBoxSendToTool.addActionListener(this::updateSendToButtonCount);
        comboBoxSelectSerializerView.addActionListener(this::updateSerializeView);
        comboBoxShapeName.addActionListener(this::updateShapeDefinition);
        comboBoxHost.addActionListener(this::updateHostTextfieldBySelection);

        textFieldFilterOperations.addKeyListener(new EnterKeyAdapter(buttonFilterOperations));
        textFieldFilterShapes.addKeyListener(new EnterKeyAdapter(buttonFilterShapes));

        buttonRefreshHttpRequest.addActionListener(this::updateSerializeView);
        buttonSendToTool.addActionListener(this::sendSelectedOperationsToTool);
        buttonPrev.addActionListener(this::handlePrevButtonPress);
        buttonAddCustomHeader.addActionListener(this::handleAddCustomHeaderButtonPress);
        buttonAddVariable.addActionListener(this::handleAddVariableButtonPress);
        buttonRemoveCustomHeader.addActionListener(e ->
            removeRowFromTable(customHeadersTableWrapper, buttonRemoveCustomHeader));
        buttonRemoveVariable.addActionListener(e ->
            removeRowFromTable(variablesTableWrapper, buttonRemoveVariable));
        buttonFilterOperations.addActionListener(e ->
            handleFilterButtonPress(e, textFieldFilterOperations, operationsTableWrapper));
        buttonFilterShapes.addActionListener(e -> {
            handleFilterButtonPress(e, textFieldFilterShapes, comboBoxShapeName);
            previousShapeNameStack.resetShapeNamesStack(comboBoxShapeName.getSelectedItem().toString());
            buttonPrev.setEnabled(false);
        });
        buttonSelectFile.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            try {
                fileChooser.setFileFilter(new FileNameExtensionFilter(
                    "json,yaml,yml",
                    "json", "yaml", "yml"));
                int ret = fileChooser.showOpenDialog(parentRoot);
                if (ret != JFileChooser.APPROVE_OPTION) {
                    return;
                }

                BurpApi.getInstance().logging().logToOutput("Opening: " + fileChooser.getSelectedFile().getName());

                operationsTableWrapper.clearRows();
                buttonSelectFile.setEnabled(false);
                labelTopMessage.setText("Loading...");

                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        // The EDT needs to load our class loader to fix dependency issues with the MCP library
                        // Note: we restore the original class loader at the end, otherwise OpenAPI parser breaks
                        // FIXME: is there a better way
                        Thread ct = Thread.currentThread();
                        ClassLoader original = ct.getContextClassLoader();
                        ct.setContextClassLoader(getClass().getClassLoader());

                        openModelFileHandler.loadModelFromFile(fileChooser.getSelectedFile());
                        modelFileHandler = openModelFileHandler.getModelFileHandler();
                        ct.setContextClassLoader(original);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            textFieldOpenFile.setText(fileChooser.getSelectedFile().getAbsolutePath());
                            buttonSelectFile.setEnabled(true);
                            operationsTableWrapper.clearColumns();
                            if (modelFileHandler != null) {
                                // Description blurb at the top of the tab
                                StringBuilder descriptionBlurb = new StringBuilder();
                                String separator = " · ";
                                descriptionBlurb.append("<html>");
                                String[][] descriptionFields = {
                                    {"Model Type", modelFileHandler.getModelType()},
                                    {"Service Name", modelFileHandler.getServiceName()},
                                };
                                IntStream.range(0, descriptionFields.length).forEachOrdered(i -> {
                                    String key = descriptionFields[i][0];
                                    String val = descriptionFields[i][1];
                                    if (val != null && !val.isEmpty()) {
                                        descriptionBlurb
                                            .append("<b>").append(key).append("</b> ")
                                            .append(val);
                                        if (i < descriptionFields.length - 1) {
                                            descriptionBlurb.append(separator);
                                        }
                                    }
                                });
                                descriptionBlurb.append("</html>");
                                labelTopMessage.setText(descriptionBlurb.toString());

                                // Rename the tab title to the file name
                                int tabIndex = parentTabbedPane.indexOfComponent(parentRoot);
                                if (tabIndex != -1) {
                                    JPanel tabTitleComponent = (JPanel) parentTabbedPane.getTabComponentAt(tabIndex);
                                    if (tabTitleComponent.getComponent(0) instanceof JLabel) {
                                        ((JLabel) tabTitleComponent.getComponent(0))
                                            .setText(fileChooser.getSelectedFile().getName());
                                    }
                                }

                                // Populate the values
                                operationsTableWrapper.addColumn(
                                    String.format("Operations (%d)", modelFileHandler.getOperations().size()));
                                operationsTableWrapper.insertValues(modelFileHandler.getOperations());
                                modelFileHandler.getShapeNames().stream()
                                    .filter(Objects::nonNull)
                                    .forEach(comboBoxShapeName::addItem);
                                tableOperations.getColumnModel()
                                    .getColumn(0)
                                    .setCellRenderer(modelFileHandler.getOperationsTableCellRenderer());
                            }
                            SwingUtilities.invokeLater(() -> {
                                tableOperations.revalidate();
                                tableOperations.repaint();
                            });
                            if (tableOperations.getModel().getRowCount() > 0) {
                                // Triggers the tableOperations action listener
                                SwingUtilities.invokeLater(() -> tableOperations.setRowSelectionInterval(0, 0));
                            }
                        } catch (Exception ex) {
                            BurpApi.getInstance().logging().logToError(ex);
                            labelTopMessage.setText("Error loading file");
                        }
                    }
                }.execute();
            } catch (Exception ex) {
                BurpApi.getInstance().logging().logToError(ex);
                labelTopMessage.setText("Error loading file");
            }
        });
    }

    public static JComponent getNewComponentInstance(JTabbedPane tabbedPane) {
        JComponent component = new ModelViewTab(tabbedPane).$$$getRootComponent$$$();
        BurpApi.getInstance().userInterface().applyThemeToComponent(component);
        return component;
    }

    private void handleFilterButtonPress(ActionEvent e, JTextField textField, Table table) {
        table.applyFilter(textField.getText());
    }

    private void handleFilterButtonPress(ActionEvent e, JTextField textField, JComboBox<String> comboBox) {
        String filterText = textField.getText();
        comboBox.removeAllItems();
        if (filterText.isEmpty()) {
            for (String shapeName : modelFileHandler.getShapeNames()) {
                comboBox.addItem(shapeName);
            }
        }
        else {
            Pattern pattern = Pattern.compile(filterText);
            modelFileHandler.getShapeNames().stream()
                .filter(shapeName -> pattern.matcher(shapeName).find())
                .forEach(comboBox::addItem);
        }
    }

    private void handleAddVariableButtonPress(ActionEvent e) {
        if (textFieldVariable.getText().isEmpty()) {
            return;
        }
        variablesTableWrapper.addRow(textFieldVariable.getText(), textFieldVariableValue.getText());

        textFieldVariable.setText("");
        textFieldVariableValue.setText("");
        buttonRemoveVariable.setEnabled(true);
    }

    private void handleAddCustomHeaderButtonPress(ActionEvent e) {
        if (textFieldCustomHeader.getText().isEmpty()) {
            return;
        }
        customHeadersTableWrapper.addRow(textFieldCustomHeader.getText(), textFieldCustomHeaderValue.getText());

        textFieldCustomHeader.setText("");
        textFieldCustomHeaderValue.setText("");
        buttonRemoveCustomHeader.setEnabled(true);
    }

    private void handlePrevButtonPress(ActionEvent e) {
        try {
            String shapeName = previousShapeNameStack.popPreviousShapeName();
            comboBoxShapeName.setSelectedItem(shapeName);
        } catch (Exception ex) {
            BurpApi.getInstance().logging().logToError(ex);
            buttonPrev.setEnabled(false);
        }
    }

    private void updateShapeDefinition(ActionEvent e) {
        String shapeName = (String) comboBoxShapeName.getSelectedItem();
        updateShapeDefinitionByShapeName(shapeName);
    }

    private void sendSelectedOperationsToTool(ActionEvent e) {
        int[] selectedRows = tableOperations.getSelectedRows();
        for (int rowNum : selectedRows) {
            String operationName = (String) tableOperations.getValueAt(rowNum, 0);
            AbstractSerializer<?> serializer = (AbstractSerializer<?>) comboBoxSelectSerializerView.getSelectedItem();
            if (serializer == null) {
                return;
            }
            try {
                SendToToolHandler sendToToolHandler = (SendToToolHandler) comboBoxSendToTool.getSelectedItem();
                if (sendToToolHandler != null) {
                    sendToToolHandler.sendToTool(serializeRequest(operationName, serializer), operationName);
                }
            } catch (Exception ex) {
                BurpApi.getInstance().logging().logToError(ex);
            }
        }
    }

    private void updateSerializers(String operationName) {
        if (modelFileHandler.updateSerializers(operationName)) {
            // If serializers list was updated, we need to refresh the combobox
            // Remove the action listeners before updating the combobox to avoid triggering, then add them back
            ActionListener[] listeners = comboBoxSelectSerializerView.getActionListeners();
            for (ActionListener listener : listeners) {
                comboBoxSelectSerializerView.removeActionListener(listener);
            }
            try {
                comboBoxSelectSerializerView.removeAllItems();
                modelFileHandler.getSerializers().forEach(comboBoxSelectSerializerView::addItem);
            } finally {
                for (ActionListener listener : listeners) {
                    comboBoxSelectSerializerView.addActionListener(listener);
                }
            }
        }
    }

    private void updateOperationDefinitionBySelectedOperation(ListSelectionEvent e) {
        if (operationsTableWrapper.isTableReady(e)) {
            return;
        }
        updateOperationDefinitionBySelectedOperation((ActionEvent) null);
    }

    private void updateOperationDefinitionBySelectedOperation(ActionEvent e) {
        String operationName;
        try {
            BurpApi.getInstance().logging().logToOutput("Updating operation definition...");
            int[] selectedRows = updateSendToButtonCount(e);
            if (selectedRows.length == 0) {
                return;
            }
            int lastSelectedRow = selectedRows[selectedRows.length - 1];
            if (lastSelectedRow < 0) {
                return;
            }

            operationName = (String) tableOperations.getValueAt(lastSelectedRow, 0);
            String operationDefinition = modelFileHandler.getOperationDefinition(operationName);
            String shapeName = modelFileHandler.getOperationShapeName(operationName);
            BurpApi.getInstance().logging().logToOutput("Got shapeName: " + shapeName);

            updateHostsList(operationName);
            updateAuthList(operationName);

            updateSerializers(operationName);
            labelOperationName.setText(modelFileHandler.getFormattedOperationName(operationName));
            burpComponentOperation.setContents(ByteArray.byteArray(operationDefinition));
            comboBoxShapeName.setSelectedItem(shapeName);
            updateSerializeView(operationName);

            previousShapeNameStack.resetShapeNamesStack(shapeName);
            buttonPrev.setEnabled(false);
        } catch (Exception ex) {
            BurpApi.getInstance().logging().logToError(ex);
        }
    }

    private void updateSerializeView(ActionEvent e) {
        int[] selectedRows = tableOperations.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }
        int lastSelectedRow = selectedRows[selectedRows.length - 1];
        if (lastSelectedRow < 0) {
            return;
        }
        String operationName = (String) tableOperations.getValueAt(lastSelectedRow, 0);
        updateSerializeView(operationName);
    }

    private void updateSerializeView(String operationName) {
        BurpApi.getInstance().logging().logToOutput("Serializing request...");
        AbstractSerializer<?> serializer = (AbstractSerializer<?>) comboBoxSelectSerializerView.getSelectedItem();
        burpComponentHttpRequestEditor.setRequest(serializer != null ?
            serializeRequest(operationName, serializer) : HttpRequest.httpRequest(ByteArray.byteArray()));
        buttonRefreshHttpRequest.setEnabled(true);
    }

    private void updateAuthList(String operationName) {
        // Compare new auth list with current
        Set<String> authList = new HashSet<>(modelFileHandler.getAuthOptions(operationName));
        Set<String> currentAuthList = getComboBoxItems(comboBoxAuth);
        if (authList.equals(currentAuthList)) {
            return;
        }

        String currentSelection = comboBoxAuth.getSelectedItem() != null ?
            comboBoxAuth.getSelectedItem().toString() : null;
        comboBoxAuth.removeAllItems();
        authList.forEach(comboBoxAuth::addItem);
        if (currentSelection != null && authList.contains(currentSelection)) {
            // Maintain current selection
            comboBoxAuth.setSelectedItem(currentSelection);
        }
    }

    private void updateHostsList(String operationName) {
        // Compare new host list with current
        Set<String> hosts = new HashSet<>(modelFileHandler.getHosts(operationName));
        Set<String> currentHosts = getComboBoxItems(comboBoxHost);
        if (hosts.equals(currentHosts)) {
            return;
        }

        String currentSelection = comboBoxHost.getSelectedItem() != null ?
            comboBoxHost.getSelectedItem().toString() : null;
        comboBoxHost.removeAllItems();
        comboBoxHost.addItem(" ");
        hosts.forEach(comboBoxHost::addItem);
        if (currentSelection != null && hosts.contains(currentSelection)) {
            // Maintain current selection
            comboBoxHost.setSelectedItem(currentSelection);
        }
        else {
            // Change Host selection to the first non-blank
            for (int i = 0; i < comboBoxHost.getItemCount(); i++) {
                if (!comboBoxHost.getItemAt(i).isBlank()) {
                    comboBoxHost.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private Set<String> getComboBoxItems(JComboBox<String> comboBox) {
        Set<String> res = new HashSet<>();
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (!comboBox.getItemAt(i).isBlank()) {
                res.add(comboBox.getItemAt(i));
            }
        }
        return res;
    }

    private void updateHostTextfieldBySelection(ActionEvent e) {
        if (comboBoxHost.getSelectedItem() == null) {
            return;
        }
        String selection = comboBoxHost.getSelectedItem().toString();
        BurpApi.getInstance().logging().logToOutput("Updating selected host: " + selection);
        textFieldHost.setText(selection.isBlank() ? "" : selection);
    }

    private HttpRequest serializeRequest(String operationName, AbstractSerializer<?> serializer) {
        SerializerType selectedSerializer = serializer.getSerializerType();
        boolean includeOptionalParameters =
            comboBoxIncludeOptionalParameters.getSelectedItem().toString().equals("Yes");
        // TODO: should move this outside of the UI code
        switch (selectedSerializer) {
            case HTTP_REQUEST -> {
                BurpApi.getInstance().logging().logToOutput("Using HTTP_REQUEST serializer type...");
                HttpRequest httpRequest = (HttpRequest) modelFileHandler.serializeOperation(
                    operationName,
                    serializer,
                    includeOptionalParameters);

                // Apply Host value
                httpRequest = modelFileHandler.applyHost(textFieldHost.getText().trim(), httpRequest);

                try {
                    // Apply Headers values
                    List<HttpHeader> customHeaders = new ArrayList<>();
                    for (int row = 0; row < tableCustomHeaders.getRowCount(); row++) {
                        customHeaders.add(HttpHeader.httpHeader(
                            tableCustomHeaders.getValueAt(row, 0).toString(),
                            tableCustomHeaders.getValueAt(row, 1).toString()));
                    }
                    httpRequest = HttpRequestUtils.addHttpHeaders(httpRequest, customHeaders);

                    // Apply Variables values
                    Map<String, String> customVariables = new HashMap<>();
                    BurpApi.getInstance().logging().logToOutput(httpRequest.path());
                    for (int row = 0; row < tableVariables.getRowCount(); row++) {
                        customVariables.put(
                            tableVariables.getValueAt(row, 0).toString(),
                            tableVariables.getValueAt(row, 1).toString());
                    }
                    httpRequest = HttpRequestUtils.replacePlaceholderPathVariables(httpRequest, customVariables);

                    if (comboBoxAuth.getSelectedItem() != null) {
                        // Apply Auth value
                        httpRequest =
                            modelFileHandler.applyAuth(comboBoxAuth.getSelectedItem().toString(), httpRequest);
                    }
                } catch (Exception ex) {
                    BurpApi.getInstance().logging().logToError(ex);
                }
                return httpRequest;
            }
            case RAW -> {
                BurpApi.getInstance().logging().logToOutput("Using RAW serializer type...");
                ByteArray rawContent = (ByteArray) modelFileHandler.serializeOperation(
                    operationName,
                    serializer,
                    includeOptionalParameters);

                rawContent = modelFileHandler.applyHost(textFieldHost.getText(), rawContent, operationName);

                return HttpRequest.httpRequest(rawContent);
            }
            default -> {
                BurpApi.getInstance().logging().logToOutput("Unknown serializer: " + selectedSerializer);
                return HttpRequest.httpRequest(
                    ByteArray.byteArray("Unknown serializer: " + selectedSerializer));
            }
        }
    }

    private int[] updateSendToButtonCount(ActionEvent e) {
        int[] selectedRows = tableOperations.getSelectedRows();
        buttonSendToTool.setText(String.format("Send (%d)", selectedRows.length));
        buttonSendToTool.setEnabled(selectedRows.length > 0);
        return selectedRows;
    }

    private void updateShapeDefinitionByShapeName(String shapeName) {
        String shapeDefinition = modelFileHandler.getShapeDefinition(shapeName);
        burpComponentShape.setContents(ByteArray.byteArray(shapeDefinition));
        previousShapeNameStack.push(shapeName);
        BurpApi.getInstance().logging().logToOutput(previousShapeNameStack);
        buttonPrev.setEnabled(previousShapeNameStack.size() > 1);
    }

    private void removeRowFromTable(Table table, JButton button) {
        int selectedRow = table.removeSelectedRow();
        if (table.getRowCount() <= 0) {
            button.setEnabled(false);
        }
        else {
            int tableSize = table.getRowCount();
            if (selectedRow > tableSize - 1) {
                selectedRow = tableSize - 1;
            }
            table.getJTable().setRowSelectionInterval(selectedRow, selectedRow);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        parentRoot = new JPanel();
        parentRoot.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        parentRoot.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonSelectFile = new JButton();
        buttonSelectFile.setText("Select");
        panel1.add(buttonSelectFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(111, -1), null, 0, false));
        textFieldOpenFile = new JTextField();
        textFieldOpenFile.setEditable(false);
        textFieldOpenFile.setEnabled(true);
        panel1.add(textFieldOpenFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        splitPaneLists = new JSplitPane();
        splitPaneLists.setOneTouchExpandable(true);
        parentRoot.add(splitPaneLists, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 5), -1, -1));
        panel2.setMinimumSize(new Dimension(120, 98));
        panel2.setPreferredSize(new Dimension(200, 98));
        splitPaneLists.setLeftComponent(panel2);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(tableOperations);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Send to", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel3.getFont()), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBoxSendToTool = new JComboBox();
        Font comboBoxSendToToolFont = this.$$$getFont$$$(null, -1, -1, comboBoxSendToTool.getFont());
        if (comboBoxSendToToolFont != null) comboBoxSendToTool.setFont(comboBoxSendToToolFont);
        panel4.add(comboBoxSendToTool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonSendToTool = new JButton();
        buttonSendToTool.setEnabled(false);
        Font buttonSendToToolFont = this.$$$getFont$$$(null, -1, -1, buttonSendToTool.getFont());
        if (buttonSendToToolFont != null) buttonSendToTool.setFont(buttonSendToToolFont);
        buttonSendToTool.setText("Send (0)");
        panel5.add(buttonSendToTool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonFilterOperations = new JButton();
        buttonFilterOperations.setText("Go");
        panel6.add(buttonFilterOperations, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel6.add(textFieldFilterOperations, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 5, 0, 0), -1, -1));
        panel7.setMinimumSize(new Dimension(0, 489));
        splitPaneLists.setRightComponent(panel7);
        splitPaneDefinitionsRequest = new JSplitPane();
        splitPaneDefinitionsRequest.setOneTouchExpandable(true);
        splitPaneDefinitionsRequest.setOrientation(0);
        panel7.add(splitPaneDefinitionsRequest, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 1, new Insets(0, 5, 5, 0), -1, -1));
        splitPaneDefinitionsRequest.setLeftComponent(panel8);
        splitPaneOperationsShapes = new JSplitPane();
        splitPaneOperationsShapes.setOneTouchExpandable(true);
        splitPaneOperationsShapes.setResizeWeight(0.5);
        panel8.add(splitPaneOperationsShapes, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 5), -1, -1));
        splitPaneOperationsShapes.setLeftComponent(panel9);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(3, 0, 3, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("Operation Definition");
        panel10.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelOperationName = new JLabel();
        labelOperationName.setText("");
        panel10.add(labelOperationName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel11, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel11.add(componentOperation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(3, 1, new Insets(0, 5, 0, 0), -1, -1));
        panel12.setMinimumSize(new Dimension(0, 71));
        splitPaneOperationsShapes.setRightComponent(panel12);
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel12.add(panel13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, Font.BOLD, -1, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText("Shape Definition");
        panel13.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel13.add(textFieldFilterShapes, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonFilterShapes = new JButton();
        buttonFilterShapes.setText("Go");
        panel13.add(buttonFilterShapes, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel12.add(panel14, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel14.add(componentShape, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel12.add(panel15, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBoxShapeName = new JComboBox();
        panel15.add(comboBoxShapeName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        buttonPrev = new JButton();
        buttonPrev.setEnabled(false);
        buttonPrev.setText("Prev");
        panel15.add(buttonPrev, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 0, 0), -1, -1));
        splitPaneDefinitionsRequest.setRightComponent(panel16);
        splitPaneHttpRequest = new JSplitPane();
        splitPaneHttpRequest.setOneTouchExpandable(true);
        panel16.add(splitPaneHttpRequest, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 5), -1, -1));
        splitPaneHttpRequest.setLeftComponent(panel17);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel17.add(panel18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel18.add(panel19, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRefreshHttpRequest = new JButton();
        buttonRefreshHttpRequest.setEnabled(false);
        buttonRefreshHttpRequest.setHorizontalAlignment(0);
        buttonRefreshHttpRequest.setText("Refresh");
        panel19.add(buttonRefreshHttpRequest, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel18.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel18.add(panel20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comboBoxSelectSerializerView = new JComboBox();
        panel20.add(comboBoxSelectSerializerView, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("View");
        panel20.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel17.add(panel21, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel21.add(componentOperationSerializerView, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(1, 1, new Insets(5, 0, 0, 0), -1, -1));
        splitPaneHttpRequest.setRightComponent(panel22);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel22.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Additional Parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, scrollPane2.getFont()), null));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPane2.setViewportView(panel23);
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel24, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Host");
        panel24.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxHost = new JComboBox();
        panel24.add(comboBoxHost, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel24.add(textFieldHost, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel25, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Auth");
        panel25.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxAuth = new JComboBox();
        panel25.add(comboBoxAuth, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel25.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel26, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Include Optional Parameters");
        panel26.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBoxIncludeOptionalParameters = new JComboBox();
        panel26.add(comboBoxIncludeOptionalParameters, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel26.add(spacer3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel27, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Add Headers");
        panel27.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel27.add(scrollPane3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableCustomHeaders.setPreferredScrollableViewportSize(new Dimension(-1, 100));
        scrollPane3.setViewportView(tableCustomHeaders);
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel27.add(panel28, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRemoveCustomHeader = new JButton();
        buttonRemoveCustomHeader.setText("Remove");
        panel28.add(buttonRemoveCustomHeader, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel28.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel27.add(panel29, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel29.add(textFieldCustomHeaderValue, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel29.add(textFieldCustomHeader, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText(":");
        panel29.add(label8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddCustomHeader = new JButton();
        buttonAddCustomHeader.setText("Add");
        panel27.add(buttonAddCustomHeader, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel23.add(panel30, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Path {variables}");
        panel30.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel30.add(scrollPane4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableVariables.setPreferredScrollableViewportSize(new Dimension(-1, 100));
        scrollPane4.setViewportView(tableVariables);
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel30.add(panel31, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonRemoveVariable = new JButton();
        buttonRemoveVariable.setText("Remove");
        panel31.add(buttonRemoveVariable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel31.add(spacer5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel30.add(panel32, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel32.add(textFieldVariable, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel32.add(textFieldVariableValue, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText(":");
        panel32.add(label10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonAddVariable = new JButton();
        buttonAddVariable.setText("Add");
        panel30.add(buttonAddVariable, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelTopMessage = new JLabel();
        labelTopMessage.setText("<html><b>Supported files</b>: OpenAPI, Swagger, Postman Collection, AWS Botocore JSON, MCP servers JSON</html>");
        labelTopMessage.putClientProperty("html.disable", Boolean.FALSE);
        parentRoot.add(labelTopMessage, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        }
        else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            }
            else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return parentRoot;
    }

    private void createUIComponents() {
        operationsTableWrapper = new Table(new JTableImmutable());
        customHeadersTableWrapper = new Table();
        variablesTableWrapper = new Table();

        operationsTableWrapper.addColumn("Operations");
        customHeadersTableWrapper.addColumn("Header");
        customHeadersTableWrapper.addColumn("Value");
        variablesTableWrapper.addColumn("Variable");
        variablesTableWrapper.addColumn("Value");

        tableOperations = operationsTableWrapper.getJTable();
        tableCustomHeaders = customHeadersTableWrapper.getJTable();
        tableVariables = variablesTableWrapper.getJTable();

        textFieldHost = new JTextFieldWithPlaceholder("https://example.com");
        textFieldCustomHeader = new JTextFieldWithPlaceholder("Header");
        textFieldCustomHeaderValue = new JTextFieldWithPlaceholder("Value");
        textFieldVariable = new JTextFieldWithPlaceholder("Variable");
        textFieldVariableValue = new JTextFieldWithPlaceholder("Value");
        textFieldFilterOperations = new JTextFieldWithPlaceholder("Filter Operations (regex)...");
        textFieldFilterShapes = new JTextFieldWithPlaceholder("Filter Shapes (regex)...");

        componentOperationSerializerView = (JComponent) burpComponentHttpRequestEditor.uiComponent();
        componentOperation = (JComponent) burpComponentOperation.uiComponent();
        componentShape = (JComponent) burpComponentShape.uiComponent();
    }
}
