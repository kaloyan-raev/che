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
package org.eclipse.che.ide.ext.java.client.inject.factories;

import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.button.ButtonWidget;
import org.eclipse.che.ide.ext.java.client.project.properties.valueproviders.button.ButtonWidgetImpl;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor.button.EditorButtonWidget;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.header.TabHeader;

import javax.validation.constraints.NotNull;

/**
 * Special factory for creating custom buttons.
 *
 * @author Valeriy Svydenko
 */
public interface ButtonWidgetFactory {

    /**
     * Creates property button widget.
     *
     * @param title
     *         title of button
     * @return an instance of {@link EditorButtonWidget}
     */
    @NotNull
    ButtonWidget createEditorButton(@NotNull String title);

}
