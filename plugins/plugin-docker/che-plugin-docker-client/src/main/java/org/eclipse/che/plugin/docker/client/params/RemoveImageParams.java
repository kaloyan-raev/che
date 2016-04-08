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
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#removeImage(RemoveImageParams)}.
 *
 * @author Mykola Morhun
 */
public class RemoveImageParams {

    private String  image;
    private Boolean force;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param image
     *         info about this parameter @see {@link #withImage(String)}
     * @return arguments holder with required parameters
     */
    public static RemoveImageParams from(@NotNull String image) {
        return new RemoveImageParams().withImage(image);
    }

    private RemoveImageParams() {}

    /**
     * @param image
     *         image identifier, either id or name
     */
    public RemoveImageParams withImage(@NotNull String image) {
        requireNonNull(image);
        this.image = image;
        return this;
    }

    /**
     * @param force
     *         {@code true} means remove an image anyway, despite using of this image
     */
    public RemoveImageParams withForce(boolean force) {
        this.force = force;
        return this;
    }

    public String image() {
        return image;
    }

    public Boolean force() {
        return force;
    }

}
