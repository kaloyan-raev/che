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

import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.terminal.MachineImplSpecificTerminalLauncher;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Launch websocket terminal in localhost machines.
 *
 * @author Kaloyan Raev
 */
public class LocalhostMachineImplTerminalLauncher implements MachineImplSpecificTerminalLauncher {
    public static final String TERMINAL_LAUNCH_COMMAND_PROPERTY = "machine.localhost.server.terminal.run_command";
    public static final String TERMINAL_LOCATION_PROPERTY       = "machine.localhost.server.terminal.location";

    private final String                             runTerminalCommand;
    private final String                             terminalLocation;

    @Inject
    public LocalhostMachineImplTerminalLauncher(@Named(TERMINAL_LAUNCH_COMMAND_PROPERTY) String runTerminalCommand,
                                          @Named(TERMINAL_LOCATION_PROPERTY) String terminalLocation) {
        this.runTerminalCommand = runTerminalCommand;
        this.terminalLocation = terminalLocation;
    }

    @Override
    public String getMachineType() {
        return "localhost";
    }

    // todo stop outdated terminal
    // todo check existing version of terminal, do not copy if it is up to date
    @Override
    public void launchTerminal(Instance machine) throws MachineException {
        // TODO for now it can be started manually
        // ./che-websocket-terminal -addr :4411 -cmd /bin/bash -static $PWD
    }
}
