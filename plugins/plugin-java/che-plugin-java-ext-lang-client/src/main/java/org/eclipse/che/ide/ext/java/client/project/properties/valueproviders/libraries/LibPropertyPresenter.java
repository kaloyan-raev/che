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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.project.properties.ProjectPropertiesResources;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.AbstractPropertiesPagePresenter;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.entry.NodeWidget;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.selectnode.JarNodeInterceptor;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.selectnode.SelectNodePresenter;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * Page for the information about libraries which are including into classpath.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class LibPropertyPresenter extends AbstractPropertiesPagePresenter implements LibPropertyView.ActionDelegate,
                                                                                     NodeWidget.ActionDelegate {

    private final ProjectPropertiesResources resources;
    private final SelectNodePresenter        selectNodePresenter;
    private final LibPropertyView            view;

    private boolean dirty = false;
    private Map<String, NodeWidget> addedLibs;
    private String                  selectedNode;

    @Inject
    public LibPropertyPresenter(LibPropertyView view,
                                JavaLocalizationConstant localization,
                                ProjectPropertiesResources resources,
                                SelectNodePresenter selectNodePresenter) {
        super(localization.librariesPropertyName(), localization.javaBuildPathCategory(), null);
        this.view = view;
        this.resources = resources;
        this.selectNodePresenter = selectNodePresenter;

        addedLibs = new HashMap<>();

        view.setDelegate(this);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    @Override
    public void onAddJarClicked() {
        selectNodePresenter.show(this, new JarNodeInterceptor());
    }

    @Override
    public void onRemoveClicked() {
        removeSelectedNode();
    }

    @Override
    public void storeChanges() {
        dirty = false;
    }

    @Override
    public void revertChanges() {
        dirty = false;
    }

    @Override
    public void addNode(String path, SVGResource icon) {
        if (path.equals(selectedNode)) {
            return;
        }
        if (selectedNode != null) {
            addedLibs.get(selectedNode).deselect();
        }
        NodeWidget addedNode = new NodeWidget(path, resources, icon);
        addedNode.setDelegate(this);
        addedNode.select();
        addedLibs.put(path, addedNode);
        dirty = true;
        delegate.onDirtyChanged();
        selectedNode = path;
        view.addNode(addedNode);
    }

    @Override
    public void removeSelectedNode() {
        dirty = true;
        delegate.onDirtyChanged();
        view.removeNode(addedLibs.remove(selectedNode));
        if (!addedLibs.isEmpty()) {
            selectedNode = addedLibs.keySet().iterator().next();
            addedLibs.get(selectedNode).select();
        } else {
            selectedNode = null;
        }
    }

    @Override
    public void onNodeClicked(@NotNull NodeWidget nodeWidget) {
        addedLibs.get(selectedNode).deselect();
        nodeWidget.select();
        selectedNode = nodeWidget.getNodePath();
    }
}
