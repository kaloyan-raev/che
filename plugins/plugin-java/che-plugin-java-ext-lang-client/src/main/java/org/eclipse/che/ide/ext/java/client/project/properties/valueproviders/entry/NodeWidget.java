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
package org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import org.eclipse.che.ide.ext.java.client.project.properties.ProjectPropertiesResources;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;

/**
 * The class describes special widget which is entry in list of nodes.
 *
 * @author Valeriy Svydenko
 */
public class NodeWidget extends Composite implements NodeEntry, ClickHandler {
    interface RecipeEntryWidgetUiBinder extends UiBinder<Widget, NodeWidget> {
    }

    private final static RecipeEntryWidgetUiBinder UI_BINDER = GWT.create(RecipeEntryWidgetUiBinder.class);

    @UiField
    SimplePanel image;
    @UiField
    Label       name;
    @UiField
    FlowPanel   main;

    private final ProjectPropertiesResources resources;
    private final String                     nodeName;

    private ActionDelegate delegate;

    public NodeWidget(String nodeName, ProjectPropertiesResources resources, SVGResource nodeIcon) {
        this.resources = resources;
        this.nodeName = nodeName;

        initWidget(UI_BINDER.createAndBindUi(this));

        SVGImage icon = new SVGImage(nodeIcon.getSvg());
        image.getElement().appendChild(icon.getSvgElement().getElement());

        name.setText(nodeName);

        addDomHandler(this, ClickEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@NotNull ClickEvent event) {
        delegate.onNodeClicked(this);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@NotNull ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** Changes style of widget as selected */
    public void select() {
        main.addStyleName(resources.getCss().selectNode());
    }

    /** Changes style of widget as unselected */
    public void deselect() {
        main.removeStyleName(resources.getCss().selectNode());
    }

    /** Sets name of the Recipe. */
    public void setName(@NotNull String name) {
        this.name.setText(name);
    }

    /** Returns path of the current node. */
    public String getNodePath() {
        return nodeName;
    }

}
