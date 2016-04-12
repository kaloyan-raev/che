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
package org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.selectnode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.PropertiesPagePresenter;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Presenter for choosing directory for searching a node.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class SelectNodePresenter implements SelectNodeView.ActionDelegate {
    private final SelectNodeView           view;
    private final ProjectExplorerPresenter projectExplorerPresenter;

    PropertiesPagePresenter propertiesPagePresenter;

    @Inject
    public SelectNodePresenter(SelectNodeView view, ProjectExplorerPresenter projectExplorerPresenter) {
        this.view = view;
        this.projectExplorerPresenter = projectExplorerPresenter;
        this.view.setDelegate(this);
    }

    /**
     * Show tree view with all needed nodes of the workspace.
     *
     * @param pagePresenter
     *         delegate from the property page
     * @param nodeInterceptor
     *         interceptor for showing nodes
     */
    public void show(PropertiesPagePresenter pagePresenter, NodeInterceptor nodeInterceptor) {
        this.propertiesPagePresenter = pagePresenter;
        view.setStructure(projectExplorerPresenter.getRootNodes(), nodeInterceptor);
        view.show();
    }

    /** {@inheritDoc} */
    @Override
    public void setSelectedNode(String path, SVGResource icon) {
        propertiesPagePresenter.addNode(path, icon);
    }
}
