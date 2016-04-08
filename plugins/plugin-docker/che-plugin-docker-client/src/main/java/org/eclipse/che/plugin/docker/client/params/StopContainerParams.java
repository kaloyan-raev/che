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
package org.eclipse.che.plugin.docker.client.params;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#stopContainer(StopContainerParams)}.
 *
 * @author Mykola Morhun
 */
public class StopContainerParams {

    private String   container;
    private Long     timeout;
    private TimeUnit timeunit;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param container
     *         info about this parameter @see {@link #withContainer(String)}
     * @return arguments holder with required parameters
     */
    public static StopContainerParams from(@NotNull String container) {
        return new StopContainerParams().withContainer(container);
    }

    private StopContainerParams() {}

    /**
     * @param container
     *         container identifier, either id or name
     */
    public StopContainerParams withContainer(@NotNull String container) {
        requireNonNull(container);
        this.container = container;
        return this;
    }

    /**
     * @param timeout
     *         time in seconds to wait for the container to stop before killing it
     */
    public StopContainerParams withTimeout(long timeout) {
        withTimeout(timeout, TimeUnit.SECONDS);
        return this;
    }

    /**
     * @param timeout
     *         time to wait for the container to stop before killing it
     * @param timeunit
     *         time unit of the timeout parameter
     */
    public StopContainerParams withTimeout(long timeout, TimeUnit timeunit) {
        this.timeout = timeout;
        this.timeunit = timeunit;
        return this;
    }

    public String container() {
        return container;
    }

    public Long timeout() {
        return timeout;
    }

    public TimeUnit timeunit() {
        return timeunit;
    }

}
