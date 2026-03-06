package com.bureauveritas.modelparser.view;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.bureauveritas.modelparser.BurpApi;
import com.bureauveritas.modelparser.control.BotocorePythonRPC;
import com.bureauveritas.modelparser.model.Settings;
import com.bureauveritas.modelparser.model.view.VersionNumber;
import com.bureauveritas.modelparser.view.component.JTextFieldWithPlaceholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainTab {
    private JPanel panelRoot;
    private JTextField textFieldPythonServerPort;
    private JButton buttonPythonServerHealthCheck;
    private JButton buttonSaveSettings;
    private JLabel labelPythonServerHealth;
    private JLabel labelVersionNumber;
    private JTabbedPane tabbedPaneMainView;
    private JComboBox<String> comboBoxValidateHostValue;
    private JTextField textFieldConnectionTimeout;
    private JLabel labelError;
    private static MainTab instance;

    private MainTab() {
        $$$setupUI$$$();

        createNewViewTab(0);

        labelError.setForeground(Color.RED);
        labelVersionNumber.setText(VersionNumber.get());
        comboBoxValidateHostValue.addItem("No (replace invalid hosts with first valid)");
        comboBoxValidateHostValue.addItem("Yes");

        buttonSaveSettings.addActionListener(this::handleSaveSettings);
        buttonPythonServerHealthCheck.addActionListener(this::handlePythonServerHealthCheck);

        tabbedPaneMainView.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    int index = tabbedPaneMainView.indexAtLocation(e.getX(), e.getY());
                    if (index == tabbedPaneMainView.getTabCount() - 1) {
                        createNewViewTab(index);
                    }
                } catch (Exception ex) {
                    BurpApi.getInstance().logging().logToError(ex);
                }
            }
        });
    }

    private void createNewViewTab(int index) {
        JPanel tabTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        tabTitle.add(new JLabel("New tab"));
        JButton tabCloseButton = new JButton("×");
        tabCloseButton.setBorder(null);
        tabCloseButton.setContentAreaFilled(false);
        tabCloseButton.addActionListener(e -> {
            int tabIndex = tabbedPaneMainView.indexOfTabComponent(tabTitle);
            if (tabIndex != -1) {
                tabbedPaneMainView.removeTabAt(tabIndex);
            }
            if (tabIndex == tabbedPaneMainView.getTabCount() - 1) {
                tabbedPaneMainView.setSelectedIndex(tabIndex - 1);
            }
        });
        tabTitle.add(tabCloseButton);

        tabbedPaneMainView.insertTab(
            null,
            null,
            ModelViewTab.getNewComponentInstance(tabbedPaneMainView),
            null,
            index);
        tabbedPaneMainView.setTabComponentAt(index, tabTitle);
        tabbedPaneMainView.setSelectedIndex(index);
    }

    public static MainTab getInstance() {
        if (instance == null) {
            instance = new MainTab();
        }
        return instance;
    }

    private void handleSaveSettings(ActionEvent actionEvent) {
        labelError.setText("");
        String errorMessage = "Error saving settings. Please check your inputs.";
        try {
            // Python port
            String portText = textFieldPythonServerPort.getText().trim();
            if (portText.isEmpty()) {
                portText = String.valueOf(Settings.DEFAULT_GRPC_PORT_NUMBER);
            }
            int port = Integer.parseInt(portText);
            if (port != Settings.getGrpcPortNumber()) {
                BotocorePythonRPC.getInstance().connect(port);
                Settings.setGrpcPortNumber(port);
            }
        } catch (Exception e) {
            labelError.setText(errorMessage);
            BurpApi.getInstance().logging().logToError(e);
        }

        try {
            // OpenAPI host validation
            boolean shouldAllowInvalidHosts = comboBoxValidateHostValue.getSelectedItem() != null ?
                comboBoxValidateHostValue.getSelectedItem().toString().equals("Yes") :
                Settings.DEFAULT_INVALID_OPENAPI_HOST_ALLOWED;
            Settings.setInvalidOpenAPIHostAllowed(shouldAllowInvalidHosts);
        } catch (Exception e) {
            labelError.setText(errorMessage);
            BurpApi.getInstance().logging().logToError(e);
        }

        try {
            // MCP connection timeout
            String timeoutText = textFieldConnectionTimeout.getText().trim();
            if (timeoutText.isEmpty()) {
                timeoutText = String.valueOf(Settings.DEFAULT_MCP_CONNECTION_TIMEOUT);
            }
            int timeoutSeconds = Integer.parseInt(timeoutText);
            if (timeoutSeconds != Settings.getMcpConnectionTimeoutSeconds()) {
                if (timeoutSeconds <= 0) {
                    throw new IllegalArgumentException("Timeout must be greater than 0.");
                }
                Settings.setMcpConnectionTimeoutSeconds(timeoutSeconds);
            }
        } catch (Exception e) {
            labelError.setText(errorMessage);
            BurpApi.getInstance().logging().logToError(e);
        }
    }

    private void handlePythonServerHealthCheck(ActionEvent actionEvent) {
        handleSaveSettings(null);
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("[yyyy-MM-dd] h:mm:ss a"));
        labelPythonServerHealth.setText(
            String.format("%s (%s)", BotocorePythonRPC.getInstance().healthCheck(), timestamp));
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
        panelRoot = new JPanel();
        panelRoot.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        tabbedPane1.setTabPlacement(3);
        panelRoot.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("View", panel1);
        tabbedPaneMainView = new JTabbedPane();
        tabbedPaneMainView.setTabPlacement(1);
        panel1.add(tabbedPaneMainView, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        Font panel2Font = this.$$$getFont$$$(null, Font.BOLD, -1, panel2.getFont());
        if (panel2Font != null) panel2.setFont(panel2Font);
        tabbedPaneMainView.addTab("+", panel2);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Settings", panel3);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel4.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "AWS JSON", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(1515, 100), null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Python Server", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel6.getFont()), null));
        panel6.add(textFieldPythonServerPort, new GridConstraints(0, 1, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Health Check");
        panel6.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Port Number");
        panel6.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelPythonServerHealth = new JLabel();
        labelPythonServerHealth.setText("Unknown");
        panel6.add(labelPythonServerHealth, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonPythonServerHealthCheck = new JButton();
        buttonPythonServerHealthCheck.setText("Check");
        panel6.add(buttonPythonServerHealthCheck, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Status:");
        panel6.add(label3, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel4.add(panel7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "OpenAPI / Swagger", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Send to Tool", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel8.getFont()), null));
        final JLabel label4 = new JLabel();
        label4.setText("Allow hosts not in spec");
        panel8.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel8.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, new Dimension(1223, 11), null, 0, false));
        comboBoxValidateHostValue = new JComboBox();
        panel8.add(comboBoxValidateHostValue, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel9, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonSaveSettings = new JButton();
        buttonSaveSettings.setText("Save settings");
        panel9.add(buttonSaveSettings, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel9.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Version:");
        panel10.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        labelVersionNumber = new JLabel();
        labelVersionNumber.setText("x.y.z");
        panel10.add(labelVersionNumber, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(750, 17), null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel10.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel4.add(panel11, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel11.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "MCP Servers JSON", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel12.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Connection", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.$$$getFont$$$(null, Font.BOLD, -1, panel12.getFont()), null));
        final JLabel label6 = new JLabel();
        label6.setText("Connection attempt timeout");
        panel12.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel12.add(spacer5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        panel12.add(textFieldConnectionTimeout, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        labelError = new JLabel();
        labelError.setText("");
        panel4.add(labelError, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel3.add(spacer6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        return panelRoot;
    }

    private void createUIComponents() {
        // FIXME: newest burp w/ JDK 25 breaks JFormattedTextFieldWithPlaceholder. Use JFormattedTextField for now
//        textFieldPythonServerPort = new JFormattedTextFieldWithPlaceholder(
//            "Override gRPC Port (Default: 50055)",
//            new PortNumberFormatter()
//        );
        textFieldPythonServerPort = new JTextFieldWithPlaceholder("Override gRPC Port (Default: 50055)");
        textFieldConnectionTimeout = new JTextFieldWithPlaceholder("MCP connection timeout in seconds (Default: 5)");
    }
}