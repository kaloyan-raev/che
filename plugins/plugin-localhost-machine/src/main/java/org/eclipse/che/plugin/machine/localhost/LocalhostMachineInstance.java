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

import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.MachineRuntimeInfoImpl;
import org.eclipse.che.api.machine.server.model.impl.ServerImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceNode;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.impl.AbstractInstance;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

/**
 * Implementation of {@link Instance} that represents localhost machine.
 *
 * @author Kaloyan Raev
 * 
 * @see LocalhostMachineInstanceProvider
 */
// todo try to avoid map of processes
public class LocalhostMachineInstance extends AbstractInstance {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LocalhostMachineInstance.class);
    
    private static final AtomicInteger pidSequence = new AtomicInteger(1);

    private final LineConsumer                                outputConsumer;
    private final LocalhostMachineFactory                     machineFactory;
    private final List<ServerConf>                            serversConf;
    private final ConcurrentHashMap<Integer, InstanceProcess> machineProcesses;

    private MachineRuntimeInfoImpl machineRuntime;

    @Inject
    public LocalhostMachineInstance(@Assisted Machine machine,
                              @Assisted LineConsumer outputConsumer,
                              LocalhostMachineFactory machineFactory,
                              @Named("machine.localhost.dev_machine.machine_servers") Set<ServerConf> devMachineSystemServers,
                              @Named("machine.localhost.machine_servers") Set<ServerConf> allMachinesSystemServers) {
        super(machine);
        this.outputConsumer = outputConsumer;
        this.machineFactory = machineFactory;
        Stream<ServerConf> confStream = Stream.concat(machine.getConfig().getServers().stream(), allMachinesSystemServers.stream());
        if (machine.getConfig().isDev()) {
            confStream = Stream.concat(confStream, devMachineSystemServers.stream());
        }
        this.serversConf = confStream.collect(toList());
        this.machineProcesses = new ConcurrentHashMap<>();
    }

    @Override
    public LineConsumer getLogger() {
        return outputConsumer;
    }

    @Override
    public MachineRuntimeInfoImpl getRuntime() {
        // lazy initialization
        if (machineRuntime == null) {
            synchronized (this) {
                if (machineRuntime == null) {
                    UriBuilder uriBuilder = UriBuilder.fromUri("http://" + getHost());

                    final Map<String, ServerImpl> servers = new HashMap<>();
                    for (ServerConf serverConf : serversConf) {
                        servers.put(serverConf.getPort(), serverConfToServer(serverConf, uriBuilder.clone()));
                    }
                    machineRuntime = new MachineRuntimeInfoImpl(emptyMap(), emptyMap(), servers);
                }
            }
            // todo get env from client
        }
        return machineRuntime;
    }

    @Override
    public InstanceProcess getProcess(final int pid) throws NotFoundException, MachineException {
        final InstanceProcess machineProcess = machineProcesses.get(pid);
        if (machineProcess == null) {
            throw new NotFoundException(format("Process with pid %s not found", pid));
        }
        try {
            machineProcess.checkAlive();
            return machineProcess;
        } catch (NotFoundException e) {
            machineProcesses.remove(pid);
            throw e;
        }
    }

    @Override
    public List<InstanceProcess> getProcesses() throws MachineException {
        // todo get children of session process
        return machineProcesses.values()
                               .stream()
                               .filter(InstanceProcess::isAlive)
                               .collect(Collectors.toList());

    }

    @Override
    public InstanceProcess createProcess(Command command, String outputChannel) throws MachineException {
        final Integer pid = pidSequence.getAndIncrement();

        LocalhostMachineProcess instanceProcess = machineFactory.createInstanceProcess(command, outputChannel, pid);

        machineProcesses.put(pid, instanceProcess);

        return instanceProcess;
    }

    /**
     * Not implemented.<p/>
     *
     * {@inheritDoc}
     */
    @Override
    public InstanceKey saveToSnapshot(String owner) throws MachineException {
        throw new MachineException("Snapshot feature is unsupported for localhost machine implementation");
    }

    @Override
    public void destroy() throws MachineException {
        // session destroying stops all processes
        // todo kill all processes started by code, we should get parent pid of session and kill all children
    }

    @Override
    public InstanceNode getNode() {
        return null;// todo
    }

    /**
     * Not implemented.<p/>
     *
     * {@inheritDoc}
     */
    @Override
    public String readFileContent(String filePath, int startFrom, int limit) throws MachineException {
        // todo
        throw new MachineException("File content reading is not implemented in localhost machine implementation");
    }

    /**
     * Not implemented.<p/>
     *
     * {@inheritDoc}
     */
    @Override
    public void copy(Instance sourceMachine, String sourcePath, String targetPath, boolean overwrite) throws MachineException {
        //todo
        throw new MachineException("Copying is not implemented in localhost machine implementation");
    }

    @Override
    public void copy(String sourcePath, String targetPath) throws MachineException {
        throw new MachineException("Copying is not implemented in localhost machine implementation");
    }

    private String getHost() {
        try {
            String apiEndpoint = System.getenv("CHE_API_ENDPOINT");
            if (apiEndpoint == null) {
                return "localhost";
            } else {
                return new URI(apiEndpoint).getHost();
            }
        } catch (URISyntaxException e) {
            LOG.error(e.getLocalizedMessage());
            return null;
        }
    }

    private ServerImpl serverConfToServer(ServerConf serverConf, UriBuilder uriBuilder) {
        String port = serverConf.getPort().split("/")[0];
        uriBuilder.port(Integer.parseInt(port));
        if (serverConf.getPath() != null) {
            uriBuilder.path(serverConf.getPath());
        }
        URI serverUri = uriBuilder.build();

        return new ServerImpl(serverConf.getRef(),
                              serverConf.getProtocol(),
                              serverUri.getHost() + ":" + serverUri.getPort(),
                              serverUri.getPath(),
                              serverConf.getProtocol() != null ? serverUri.toString() : null);
    }

}
