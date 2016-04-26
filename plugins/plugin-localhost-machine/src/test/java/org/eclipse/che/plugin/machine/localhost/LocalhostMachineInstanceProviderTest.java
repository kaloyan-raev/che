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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Collections;
import java.util.HashSet;

import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.exception.SnapshotException;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.machine.server.model.impl.ServerConfImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.plugin.machine.localhost.LocalhostMachineInstanceProvider;
import org.eclipse.che.plugin.machine.localhost.LocalhostMachineFactory;
import org.eclipse.che.plugin.machine.localhost.LocalhostMachineInstance;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * @author Kaloyan Raev
 */
@Listeners(MockitoTestNGListener.class)
public class LocalhostMachineInstanceProviderTest {
    @Mock
    private LocalhostMachineFactory  localhostMachineFactory;
    @Mock
    private LocalhostMachineInstance localhostMachineInstance;

    private LocalhostMachineInstanceProvider provider;
    private RecipeImpl                 recipe;
    private MachineImpl                machine;

    @BeforeMethod
    public void setUp() throws Exception {
        provider = new LocalhostMachineInstanceProvider(localhostMachineFactory);
        machine = createMachine();
        recipe = new RecipeImpl().withType("localhost-config");
    }

    @Test
    public void shouldReturnCorrectType() throws Exception {
        assertEquals(provider.getType(), "localhost");
    }

    @Test
    public void shouldReturnCorrectRecipeTypes() throws Exception {
        assertEquals(provider.getRecipeTypes(), new HashSet<>(singletonList("localhost-config")));
    }

    @Test(expectedExceptions = MachineException.class,
          expectedExceptionsMessageRegExp = "Snapshot feature is unsupported for localhost machine implementation")
    public void shouldThrowMachineExceptionOnCreateInstanceFromSnapshot() throws Exception {
        InstanceKey instanceKey = () -> Collections.emptyMap();

        provider.createInstance(instanceKey, null, null);
    }

    @Test(expectedExceptions = SnapshotException.class,
          expectedExceptionsMessageRegExp = "Snapshot feature is unsupported for localhost machine implementation")
    public void shouldThrowSnapshotExceptionOnRemoveSnapshot() throws Exception {
        provider.removeInstanceSnapshot(null);
    }

    @Test
    public void shouldThrowExceptionOnDevMachineCreationFromRecipe() throws Exception {
        Machine machine = createMachine(true);

        provider.createInstance(recipe, machine, LineConsumer.DEV_NULL);
    }

    @Test
    public void shouldBeAbleToCreateLocalhostMachineInstanceOnMachineCreationFromRecipe() throws Exception {
        when(localhostMachineFactory.createInstance(eq(machine), any(LineConsumer.class))).thenReturn(localhostMachineInstance);

        Instance instance = provider.createInstance(recipe, machine, LineConsumer.DEV_NULL);

        assertEquals(instance, localhostMachineInstance);
    }

    private MachineImpl createMachine() {
        return createMachine(false);
    }

    private MachineImpl createMachine(boolean isDev) {
        MachineConfig machineConfig = MachineConfigImpl.builder()
                                                       .setDev(isDev)
                                                       .setEnvVariables(singletonMap("testEnvVar1", "testEnvVarVal1"))
                                                       .setName("name1")
                                                       .setServers(singletonList(new ServerConfImpl("myref1",
                                                                                                    "10011/tcp",
                                                                                                    "http",
                                                                                                    null)))
                                                       .setSource(new MachineSourceImpl("localhost-config",
                                                                                        "localhost:10012/recipe"))
                                                       .setType("localhost")
                                                       .build();
        return MachineImpl.builder()
                          .setConfig(machineConfig)
                          .setEnvName("env1")
                          .setId("id1")
                          .setOwner("owner1")
                          .setRuntime(null)
                          .setStatus(MachineStatus.CREATING)
                          .setWorkspaceId("wsId1")
                          .build();
    }
}
