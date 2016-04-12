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

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;


/**
 * Class contains references to resources which need to correct displaying of command wizard dialog.
 *
 * @author Oleksii Orel
 */
public interface ProjectPropertiesResources extends ClientBundle {

    interface EditCommandStyles extends CssResource {

        String categoryHeader();

        String categorySubElementHeader();

        String hintLabel();

        String buttonArea();

        String opacityButton();

        String selectNode();
    }

    @Source({"PropertiesRenderer.css", "org/eclipse/che/ide/api/ui/style.css"})
    EditCommandStyles getCss();
}
