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
package org.eclipse.che.plugin.machine.ssh;

import org.eclipse.che.api.machine.server.exception.MachineException;

/**
 * Client for communication with ssh machine using ssh protocol.
 *
 * author Alexander Garagatyi
 */
public interface SshClient {

    String getHost();

    void start() throws MachineException;

    void stop() throws MachineException;

    SshProcess createProcess(String commandLine) throws MachineException;

    void copy(String sourcePath, String targetPath) throws MachineException;
}
