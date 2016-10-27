/*******************************************************************************
 * Copyright (c) 2016 Rogue Wave Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Rogue Wave Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.zdb.ide;

import org.eclipse.che.ide.api.debug.DebugConfigurationType;
import org.eclipse.che.ide.api.extension.ExtensionGinModule;
import org.eclipse.che.plugin.zdb.ide.configuration.ZendDbgConfigurationPageView;
import org.eclipse.che.plugin.zdb.ide.configuration.ZendDbgConfigurationPageViewImpl;
import org.eclipse.che.plugin.zdb.ide.configuration.ZendDbgConfigurationType;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.inject.client.multibindings.GinMultibinder;
import com.google.inject.Singleton;

/**
 * Zend debugger runtime GIN module.
 *
 * @author Bartlomiej Laczkowski
 */
@ExtensionGinModule
public class ZendDbgGinModule extends AbstractGinModule {

    @Override
    protected void configure() {
        GinMultibinder.newSetBinder(binder(), DebugConfigurationType.class).addBinding()
                .to(ZendDbgConfigurationType.class);
        bind(ZendDbgConfigurationPageView.class).to(ZendDbgConfigurationPageViewImpl.class).in(Singleton.class);
    }

}
