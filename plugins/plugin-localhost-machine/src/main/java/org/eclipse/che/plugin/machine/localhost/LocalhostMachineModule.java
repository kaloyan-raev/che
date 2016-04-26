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
import org.eclipse.che.api.machine.server.terminal.MachineImplSpecificTerminalLauncher;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

/**
 * Provides bindings needed for localhost machine implementation usage.
 *
 * @author Kaloyan Raev
 */
public class LocalhostMachineModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<org.eclipse.che.api.machine.server.spi.InstanceProvider> machineProviderMultibinder =
                Multibinder.newSetBinder(binder(),
                                         org.eclipse.che.api.machine.server.spi.InstanceProvider.class);
        machineProviderMultibinder.addBinding()
                                  .to(LocalhostMachineInstanceProvider.class);

        install(new FactoryModuleBuilder()
                        .implement(org.eclipse.che.api.machine.server.spi.Instance.class,
                                   org.eclipse.che.plugin.machine.localhost.LocalhostMachineInstance.class)
                        .implement(org.eclipse.che.api.machine.server.spi.InstanceProcess.class,
                                   org.eclipse.che.plugin.machine.localhost.LocalhostMachineProcess.class)
                        .build(LocalhostMachineFactory.class));

        Multibinder<MachineImplSpecificTerminalLauncher> terminalLaunchers =
                Multibinder.newSetBinder(binder(),
                                         MachineImplSpecificTerminalLauncher.class);
        terminalLaunchers.addBinding().to(LocalhostMachineImplTerminalLauncher.class);

        bindConstant().annotatedWith(Names.named(LocalhostMachineImplTerminalLauncher.TERMINAL_LAUNCH_COMMAND_PROPERTY))
                      .to("~/che/terminal/che-websocket-terminal -addr :4411 -cmd /bin/bash -static ~/che/terminal/");

        bindConstant().annotatedWith(Names.named(LocalhostMachineImplTerminalLauncher.TERMINAL_LOCATION_PROPERTY))
                      .to("~/che/terminal/");

        Multibinder<ServerConf> devMachineServers = Multibinder.newSetBinder(binder(),
                ServerConf.class,
                Names.named("machine.localhost.dev_machine.machine_servers"));
        devMachineServers.addBinding().toProvider(WsAgentServerConfProvider.class);
        
        Multibinder<ServerConf> machineServers = Multibinder.newSetBinder(binder(),
                org.eclipse.che.api.core.model.machine.ServerConf.class,
                Names.named("machine.localhost.machine_servers"));
        machineServers.addBinding().toProvider(TerminalServerConfProvider.class);
    }
}
