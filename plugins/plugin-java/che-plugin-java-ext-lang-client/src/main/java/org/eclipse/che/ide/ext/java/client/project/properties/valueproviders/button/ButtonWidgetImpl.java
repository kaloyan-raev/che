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
package org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.button;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.api.theme.Style;
import org.eclipse.che.ide.ext.java.client.project.properties.ProjectPropertiesResources;

import javax.validation.constraints.NotNull;

/**
 * Class provides view representation of property button on properties panel.
 *
 * @author Valeriy Svydenko
 */
public class ButtonWidgetImpl extends Composite implements ButtonWidget, ClickHandler {
    interface ButtonWidgetImplUiBinder extends UiBinder<Widget, ButtonWidgetImpl> {
    }

    private static final ButtonWidgetImplUiBinder UI_BINDER = GWT.create(ButtonWidgetImplUiBinder.class);

    @UiField
    Label button;

    @UiField(provided = true)
    final ProjectPropertiesResources  resources;

    private ActionDelegate delegate;
    private boolean        isEnable;

    @Inject
    public ButtonWidgetImpl(ProjectPropertiesResources resources, @Assisted String title) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));

        getElement().getStyle().setBackgroundColor(Style.getButtonBackground());

        button.setText(title);

        addDomHandler(this, ClickEvent.getType());

        setEnable(true);
    }

    /** {@inheritDoc} */
    @Override
    public void setEnable(boolean isEnable) {
        this.isEnable = isEnable;

        if (isEnable) {
            button.removeStyleName(resources.getCss().opacityButton());
        } else {
            button.addStyleName(resources.getCss().opacityButton());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@NotNull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(ClickEvent event) {
        if (isEnable) {
            delegate.onButtonClicked();
        }
    }
}
