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

import static org.eclipse.che.api.machine.shared.Constants.WSAGENT_REFERENCE;
import static org.eclipse.che.api.machine.shared.Constants.WS_AGENT_PORT;

/**
 * Provides server conf that describes workspace agent server
 *
 * @author Kaloyan Raev
 */
@Singleton
public class WsAgentServerConfProvider implements Provider<ServerConf> {

    @Inject
    @Named("api.endpoint")
    private URI apiEndpoint;

    @Override
    public ServerConf get() {
        return new ServerConfImpl(WSAGENT_REFERENCE,
                                  WS_AGENT_PORT,
                                  apiEndpoint.getScheme(),
                                  "api/ext");
    }

}
