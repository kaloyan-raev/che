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
package org.eclipse.che.ide.ext.java.client.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.ext.java.client.JavaLocalizationConstant;
import org.eclipse.che.ide.ext.java.client.project.properties.ProjectPropertiesPresenter;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Call Project wizard to change project type
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class ProjectPropertiesAction extends AbstractPerspectiveAction {

    private final ProjectPropertiesPresenter projectPropertiesPresenter;
    private final AppContext                 appContext;

    @Inject
    public ProjectPropertiesAction(AppContext appContext,
                                   ProjectPropertiesPresenter projectPropertiesPresenter,
                                   JavaLocalizationConstant localization,
                                   Resources resources) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID),
              localization.projectPropertiesTitle(),
              localization.projectPropertiesDescriptions(),
              null,
              resources.projectConfiguration());
        this.projectPropertiesPresenter = projectPropertiesPresenter;
        this.appContext = appContext;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (appContext.getCurrentProject() == null) {
            return;
        }
        projectPropertiesPresenter.show();
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);
        event.getPresentation().setEnabled(appContext.getCurrentProject() != null);
    }
}
