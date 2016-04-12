/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.libraries;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.inject.factories.ButtonWidgetFactory;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.button.ButtonWidget;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.entry.NodeWidget;
import org.eclipse.che.ide.extension.machine.client.command.edit.EditCommandsView;

/**
 * The implementation of {@link LibPropertyView}.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class LibPropertyViewImpl extends Composite implements LibPropertyView {
    private static LibPropertyViewImplUiBinder ourUiBinder = GWT.create(LibPropertyViewImplUiBinder.class);

    private final ButtonWidgetFactory        buttonWidgetFactory;

    @UiField
    FlowPanel buttonsPanel;
    @UiField
    FlowPanel libraryPanel;

    private ActionDelegate delegate;

    @Inject
    public LibPropertyViewImpl(ButtonWidgetFactory buttonWidgetFactory, JavaLocalizationConstant localization) {
        this.buttonWidgetFactory = buttonWidgetFactory;
        initWidget(ourUiBinder.createAndBindUi(this));

        ButtonWidget.ActionDelegate addBtnDelegate = new ButtonWidget.ActionDelegate() {
            @Override
            public void onButtonClicked() {
                delegate.onAddJarClicked();
            }
        };
        createButton(localization.addJarButton(), addBtnDelegate);

        ButtonWidget.ActionDelegate removeBtnDelegate = new ButtonWidget.ActionDelegate() {
            @Override
            public void onButtonClicked() {
                delegate.onRemoveClicked();
            }
        };
        createButton(localization.removeElementButton(), removeBtnDelegate);
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addNode(NodeWidget addedNode) {
        libraryPanel.add(addedNode);
    }

    @Override
    public void removeNode(NodeWidget nodeWidget) {
        libraryPanel.remove(nodeWidget);
    }

    interface LibPropertyViewImplUiBinder
            extends UiBinder<Widget, LibPropertyViewImpl> {
    }

    private ButtonWidget createButton(String title, ButtonWidget.ActionDelegate delegate) {
        ButtonWidget button = buttonWidgetFactory.createEditorButton(title);
        button.setDelegate(delegate);

        buttonsPanel.add(button);

        return button;
    }
}
