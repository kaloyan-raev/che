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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;

/**
 * Implementation of {@link InstanceProvider} based on running local processes.
 *
 * <p>This implementation ignores machine limits {@link MachineConfig#getLimits()}.</p>
 *
 * @author Kaloyan Raev
 */
// todo tests
public class LocalhostMachineInstanceProvider implements InstanceProvider {

    private final Set<String>             supportedRecipeTypes;
    private final LocalhostMachineFactory localhostMachineFactory;

    @Inject
    public LocalhostMachineInstanceProvider(LocalhostMachineFactory localhostMachineFactory) throws IOException {
        this.localhostMachineFactory = localhostMachineFactory;
        this.supportedRecipeTypes = Collections.singleton("localhost-config");
    }

    @Override
    public String getType() {
        return "localhost";
    }

    @Override
    public Set<String> getRecipeTypes() {
        return supportedRecipeTypes;
    }

    @Override
    public Instance createInstance(Recipe recipe,
                                   Machine machine,
                                   LineConsumer machineLogsConsumer) throws MachineException {
        requireNonNull(machine, "Non null machine required");
        requireNonNull(machineLogsConsumer, "Non null logs consumer required");

        return localhostMachineFactory.createInstance(machine,
                                                machineLogsConsumer);
    }

    @Override
    public Instance createInstance(InstanceKey instanceKey,
                                   Machine machine,
                                   LineConsumer creationLogsOutput) throws NotFoundException, MachineException {
        throw new MachineException("Snapshot feature is unsupported for localhost machine implementation");
    }

    @Override
    public void removeInstanceSnapshot(InstanceKey instanceKey) throws SnapshotException {
        throw new SnapshotException("Snapshot feature is unsupported for localhost machine implementation");
    }

}
