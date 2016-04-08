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

import static java.util.Objects.requireNonNull;

/**
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#top(TopParams)}.
 *
 * @author Mykola Morhun
 */
public class TopParams {

    private String   container;
    private String[] psArgs;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param container
     *         info about this parameter @see {@link #withContainer(String)}
     * @return arguments holder with required parameters
     */
    public static TopParams from(@NotNull String container) {
        return new TopParams().withContainer(container);
    }

    private TopParams() {}

    /**
     * @param container
     *         container id or name
     */
    public TopParams withContainer(@NotNull String container) {
        requireNonNull(container);
        this.container = container;
        return this;
    }

    /**
     * @param psArgs
     *         ps arguments to use
     */
    public TopParams withPsArgs(String... psArgs) {
        if (psArgs.length > 0) {
            this.psArgs = psArgs;
        }
        return this;
    }

    public String container() {
        return container;
    }

    public String[] psArgs() {
        return psArgs;
    }

}
