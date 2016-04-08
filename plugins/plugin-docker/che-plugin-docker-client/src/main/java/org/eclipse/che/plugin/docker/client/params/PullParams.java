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

import org.eclipse.che.plugin.docker.client.ProgressMonitor;

import javax.validation.constraints.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#pull(PullParams, ProgressMonitor)}.
 *
 * @author Mykola Morhun
 */
public class PullParams {

    private String image;
    private String tag;
    private String registry;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param image
     *         info about this parameter @see {@link #withImage(String)}
     * @return arguments holder with required parameters
     */
    public static PullParams from(@NotNull String image) {
        return new PullParams().withImage(image);
    }

    private PullParams() {}

    /**
     * @param image
     *         name of the image to pull
     */
    public PullParams withImage(@NotNull String image) {
        requireNonNull(image);
        this.image = image;
        return this;
    }

    /**
     * @param tag
     *         tag of the image
     */
    public PullParams withTag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @param registry
     *         host and port of registry, e.g. localhost:5000.
     *         If it is not set, default value "hub.docker.com" will be used
     */
    public PullParams withRegistry(String registry) {
        this.registry = registry;
        return this;
    }

    public String image() {
        return image;
    }

    public String tag() {
        return tag;
    }

    public String registry() {
        return registry;
    }

}
