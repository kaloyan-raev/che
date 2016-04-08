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
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#inspectImage(InspectImageParams)}.
 *
 * @author Mykola Morhun
 */
public class InspectImageParams {

    private String image;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param image
     *         info about this parameter @see {@link #withImage(String)}
     * @return arguments holder with required parameters
     */
    public static InspectImageParams from(@NotNull String image) {
        return new InspectImageParams().withImage(image);
    }

    private InspectImageParams() {}

    /**
     * @param image
     *         id or full repository name of docker image
     */
    public InspectImageParams withImage(@NotNull String image) {
        requireNonNull(image);
        this.image = image;
        return this;
    }

    public String image() {
        return image;
    }

}
