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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event fired when editor's settings has been changed.
 *
 * @author Roman Nikitenko
 */
public class EditorSettingsChangedEvent extends GwtEvent<EditorSettingsChangedHandler> {

    public static final Type<EditorSettingsChangedHandler> TYPE = new Type<>();

    @Override
    public Type<EditorSettingsChangedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(EditorSettingsChangedHandler handler) {
        handler.onEditorSettingsChanged(this);
    }
}
