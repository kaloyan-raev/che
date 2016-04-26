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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.inject.Inject;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.impl.AbstractMachineProcess;
import org.eclipse.che.commons.annotation.Nullable;

import com.google.inject.assistedinject.Assisted;

import static java.lang.String.format;

/**
 * Localhost machine implementation of {@link InstanceProcess}
 * 
 * @author Kaloyan Raev
 */
public class LocalhostMachineProcess extends AbstractMachineProcess implements InstanceProcess {
    private final String    commandLine;

    private volatile boolean started;
    
    private Process process;

    @Inject
    public LocalhostMachineProcess(@Assisted Command command,
                             @Nullable @Assisted("outputChannel") String outputChannel,
                             @Assisted int pid) {
        super(command, pid, outputChannel);
        this.commandLine = command.getCommandLine();
        this.started = false;
    }

    @Override
    public boolean isAlive() {
        if (!started) {
            return false;
        }
        try {
            checkAlive();
            return true;
        } catch (MachineException | NotFoundException e) {
            // when process is not found (may be finished or killed)
            // when process is not running yet
            return false;
        }
    }

    @Override
    public void start() throws ConflictException, MachineException {
        start(null);
    }

    @Override
    public void start(LineConsumer output) throws ConflictException, MachineException {
        if (started) {
            throw new ConflictException("Process already started.");
        }

        try {
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", commandLine);
            builder.redirectErrorStream(true);
            process = builder.start();
            
            started = true;
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // TODO format output as it is done in docker impl
                    // TODO use async streams?
                    if (output != null) {
                        output.writeLine(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new MachineException("Localhost machine command execution error:" + e.getLocalizedMessage());
        }
    }

    @Override
    public void checkAlive() throws MachineException, NotFoundException {
        if (!started) {
            throw new NotFoundException("Process is not started yet");
        }

        if (!process.isAlive()) {
            throw new NotFoundException(format("Process with pid %s not found", getPid()));
        }
    }

    @Override
    public void kill() throws MachineException {
        process.destroy();
    }
}
