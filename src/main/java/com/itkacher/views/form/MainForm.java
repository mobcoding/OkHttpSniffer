/*
 * Copyright 2018 LocaleBro.com [Ievgenii Tkachenko(gektor650@gmail.com)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itkacher.views.form;

import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import com.itkacher.Resources;
import com.itkacher.data.DebugDevice;
import com.itkacher.data.DebugProcess;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class MainForm {
    private JPanel panel;
    private JComboBox<DebugDevice> deviceList;
    private JComboBox<DebugProcess> appList;
    private JPanel mainContainer;
    private JEditorPane initialHtml;
    private JPanel buttonContainer;
    private final JButton scrollToBottomButton;
    private final JButton clearButton;
    private final JButton localizeButton;
    private final JButton donateButton;

    public MainForm() {
        buttonContainer.setLayout(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(2), JBUI.scale(2)));
        buttonContainer.setBorder(JBUI.Borders.emptyRight(6));

        localizeButton = createToolbarButton(
                AllIcons.Actions.Help,
                Resources.Companion.getString("tooltip_localize")
        );
        GridBagConstraints localeBroConstraints = new GridBagConstraints();
        localeBroConstraints.gridx = 0;
        localeBroConstraints.gridy = 0;

        donateButton = createToolbarButton(
                AllIcons.General.BalloonInformation,
                Resources.Companion.getString("tooltip_support")
        );
        GridBagConstraints donateButtonConstraints = new GridBagConstraints();
        donateButtonConstraints.gridx = 1;
        donateButtonConstraints.gridy = 0;

        scrollToBottomButton = createToolbarButton(
                AllIcons.Actions.MoveDown,
                Resources.Companion.getString("tooltip_scroll_to_bottom")
        );
        GridBagConstraints scrollConstraints = new GridBagConstraints();
        scrollConstraints.gridx = 2;
        scrollConstraints.gridy = 0;

        clearButton = createToolbarButton(
                AllIcons.General.Delete,
                Resources.Companion.getString("tooltip_clear_requests")
        );
        GridBagConstraints clearConstraints = new GridBagConstraints();
        clearConstraints.gridx = 3;
        clearConstraints.gridy = 0;

        buttonContainer.add(localizeButton, localeBroConstraints);
        buttonContainer.add(donateButton, donateButtonConstraints);
        buttonContainer.add(scrollToBottomButton, scrollConstraints);
        buttonContainer.add(clearButton, clearConstraints);

        initialHtml.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        initialHtml.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        initialHtml.setEditable(false);

        initialHtml.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }
        });

        URL initialFile = getClass().getClassLoader().getResource("initial.html");
        if (initialFile != null) {
            try {
                initialHtml.setPage(initialFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static JButton createToolbarButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setText(null);
        Dimension size = JBUI.size(26);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setMargin(JBUI.insets(5));
        button.setFocusable(false);
        button.setRolloverEnabled(true);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        return button;
    }

    public JPanel getPanel() {
        return panel;
    }

    public JComboBox<DebugDevice> getDeviceList() {
        return deviceList;
    }

    public JComboBox<DebugProcess> getAppList() {
        return appList;
    }

    public JButton getScrollToBottomButton() {
        return scrollToBottomButton;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JButton getLocalizeButton() {
        return localizeButton;
    }

    public JButton getDonateButton() {
        return donateButton;
    }

    public JPanel getMainContainer() {
        return mainContainer;
    }

    public JEditorPane getInitialHtml() {
        return initialHtml;
    }
}
