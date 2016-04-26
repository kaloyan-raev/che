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

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;

import com.google.inject.assistedinject.Assisted;

/**
 * Provides localhost machine implementation instances.
 *
 * @author Kaloyan Raev
 */
public interface LocalhostMachineFactory {

    /**
     * Creates localhost machine implementation of {@link Instance}.
     *
     * @param machine description of machine
     * @param outputConsumer consumer of output from container main process
     * @throws MachineException if error occurs on creation of {@code Instance}
     */
    LocalhostMachineInstance createInstance(@Assisted Machine machine,
                                      @Assisted LineConsumer outputConsumer) throws MachineException;

    /**
     * Creates localhost machine implementation of {@link org.eclipse.che.api.machine.server.spi.InstanceProcess}.
     *
     * @param command command that should be executed on process start
     * @param outputChannel channel where output will be available on process execution
     * @param pid virtual id of that process
     */
    LocalhostMachineProcess createInstanceProcess(@Assisted Command command,
                                            @Assisted("outputChannel") String outputChannel,
                                            @Assisted int pid);
}
