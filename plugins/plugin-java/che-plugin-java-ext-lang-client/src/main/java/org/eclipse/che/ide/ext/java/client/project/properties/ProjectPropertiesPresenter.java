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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.PropertiesPagePresenter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Presenter for managing project properties.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class ProjectPropertiesPresenter implements ProjectPropertiesView.ActionDelegate, PropertiesPagePresenter.DirtyStateListener {

    private final ProjectPropertiesView        view;
    private final Set<PropertiesPagePresenter> properties;

    private Map<String, Set<PropertiesPagePresenter>> propertiesMap;

    @Inject
    protected ProjectPropertiesPresenter(ProjectPropertiesView view,
                                         Set<PropertiesPagePresenter> properties) {
        this.view = view;
        this.properties = properties;
        this.view.setDelegate(this);
        for (PropertiesPagePresenter property : properties) {
            property.setUpdateDelegate(this);
        }
    }

    @Override
    public void onCloseClicked() {
        view.close();
    }

    @Override
    public void onSaveClicked() {
    }


    @Override
    public void onCancelClicked() {
    }

    @Override
    public void onEnterClicked() {
        if (view.isCancelButtonInFocus()) {
            onCancelClicked();
            return;
        }

        if (view.isCloseButtonInFocus()) {
            onCloseClicked();
            return;
        }
        onSaveClicked();
    }

    @Override
    public void onConfigurationSelected(PropertiesPagePresenter property) {
        property.go(view.getCommandConfigurationsContainer());
    }

    /** Show dialog. */
    public void show() {
        if (propertiesMap != null) {
            view.show();
            return;
        }

        propertiesMap = new HashMap<>();
        for (PropertiesPagePresenter property : properties) {
            Set<PropertiesPagePresenter> properties = propertiesMap.get(property.getCategory());
            if (properties == null) {
                properties = new HashSet<>();
                propertiesMap.put(property.getCategory(), properties);
            }

            properties.add(property);
        }
        view.setProperties(propertiesMap);

        view.show();
        view.setSaveButtonState(false);
        view.selectProperty(propertiesMap.entrySet().iterator().next().getValue().iterator().next());
    }

    @Override
    public void onDirtyChanged() {
        for (PropertiesPagePresenter p : properties) {
            if (p.isDirty()) {
                view.setSaveButtonState(true);
                return;
            }
        }
        view.setSaveButtonState(false);
    }
}
