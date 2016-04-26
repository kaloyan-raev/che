/*******************************************************************************
 * Copyright (c) 2016 Zend Technologies
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.machine.localhost;

import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.machine.server.model.impl.ServerConfImpl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;

/**
 * Provides server conf that describes websocket terminal server
 *
 * @author Kaloyan Raev
 */
@Singleton
public class TerminalServerConfProvider implements Provider<ServerConf> {
    public static final String TERMINAL_SERVER_REFERENCE = "terminal";

    @Inject
    @Named("api.endpoint")
    private URI apiEndpoint;

    @Override
    public ServerConf get() {
        return new ServerConfImpl(TERMINAL_SERVER_REFERENCE, "4411/tcp", apiEndpoint.getScheme(), null);
    }
}
