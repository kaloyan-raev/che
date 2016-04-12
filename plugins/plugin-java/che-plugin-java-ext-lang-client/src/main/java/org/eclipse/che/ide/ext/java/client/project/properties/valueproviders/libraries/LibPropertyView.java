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

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.entry.NodeWidget;

/**
 * View interface for the information about JARs and class folders on the build path.
 *
 * @author Valeriy Svydenko
 */
@ImplementedBy(LibPropertyViewImpl.class)
public interface LibPropertyView extends View<LibPropertyView.ActionDelegate> {
    /**
     * Adds new node to the library.
     *
     * @param addedNode
     *         widget of the new lib node
     */
    void addNode(NodeWidget addedNode);

    /**
     * Removes node from the library.
     *
     * @param nodeWidget
     *         widget which should be removed
     */
    void removeNode(NodeWidget nodeWidget);

    interface ActionDelegate {
        /** Performs some actions when user click on Add button. */
        void onAddJarClicked();

        /** Performs some actions when user click on Remove button. */
        void onRemoveClicked();
    }
}
