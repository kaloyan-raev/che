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
package org.eclipse.che.ide.ext.java.client.project.properties;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.PropertiesPagePresenter;

import java.util.Map;
import java.util.Set;

/**
 * The view of {@link ProjectPropertiesPresenter}.
 *
 * @author Valeriy Svydenko
 */
@ImplementedBy(ProjectPropertiesViewImpl.class)
public interface ProjectPropertiesView extends View<ProjectPropertiesView.ActionDelegate> {

    /** Show view. */
    void show();

    /** Close view. */
    void close();

    /** Returns the component used for command configurations display. */
    AcceptsOneWidget getCommandConfigurationsContainer();

    /** Sets enabled state of the 'Cancel' button. */
    void setCancelButtonState(boolean enabled);

    /** Sets enabled state of the 'Apply' button. */
    void setSaveButtonState(boolean enabled);

    /** Sets the focus on the 'Close' button. */
    void setCloseButtonInFocus();

    /** Returns {@code true} if cancel button is in the focus and {@code false} - otherwise. */
    boolean isCancelButtonInFocus();

    /** Returns {@code true} if close button is in the focus and {@code false} - otherwise. */
    boolean isCloseButtonInFocus();

    /** Sets all properties */
    void setProperties(Map<String, Set<PropertiesPagePresenter>> properties);

    /**
     * Selects Property page
     *
     * @param property
     *         chosen page
     */
    void selectProperty(PropertiesPagePresenter property);

    /** Action handler for the view actions/controls. */
    interface ActionDelegate {

        /** Called when 'Ok' button is clicked. */
        void onCloseClicked();

        /** Called when 'Apply' button is clicked. */
        void onSaveClicked();

        /** Called when 'Cancel' button is clicked. */
        void onCancelClicked();

        /** Performs any actions appropriate in response to the user having clicked the Enter key. */
        void onEnterClicked();

        /** Performs any actions appropriate in response to the user having clicked on the configuration. */
        void onConfigurationSelected(PropertiesPagePresenter property);
    }
}
