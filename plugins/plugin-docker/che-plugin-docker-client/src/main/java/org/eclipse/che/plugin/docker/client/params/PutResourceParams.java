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
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

/**
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#putResource(PutResourceParams)}.
 *
 * @author Mykola Morhun
 */
public class PutResourceParams {

    private String      container;
    private String      targetPath;
    private InputStream sourceStream;
    private Boolean     noOverwriteDirNonDir;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param container
     *         info about this parameter @see {@link #withContainer(String)}
     * @param targetPath
     *         info about this parameter @see {@link #withTargetPath(String)}
     * @return arguments holder with required parameters
     */
    public static PutResourceParams from(@NotNull String container, @NotNull String targetPath) {
        return new PutResourceParams().withContainer(container)
                                      .withTargetPath(targetPath);
    }

    private PutResourceParams() {}

    /**
     * @param container
     *         container id or name
     */
    public PutResourceParams withContainer(@NotNull String container) {
        requireNonNull(container);
        this.container = container;
        return this;
    }

    /**
     * @param targetPath
     *         path to a directory in the container to extract the archive’s contents into. Required.
     *         If not an absolute path, it is relative to the container’s root directory. The path resource must exist.
     */
    public PutResourceParams withTargetPath(@NotNull String targetPath) {
        requireNonNull(targetPath);
        this.targetPath = targetPath;
        return this;
    }

    /**
     * @param sourceStream
     *         stream of files from source container, must be obtained from another container
     *          using {@link org.eclipse.che.plugin.docker.client.DockerConnector#getResource(GetResourceParams)}
     */
    public PutResourceParams withSourceStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
        return this;
    }

    /**
     * @param noOverwriteDirNonDir
     *         if {@code true} then it will be an error if unpacking the given content would cause
     *          an existing directory to be replaced with a non-directory and vice versa.
     */
    public PutResourceParams withNoOverwriteDirNonDir(Boolean noOverwriteDirNonDir) {
        this.noOverwriteDirNonDir = noOverwriteDirNonDir;
        return this;
    }

    public String container() {
        return container;
    }

    public String targetPath() {
        return targetPath;
    }

    public InputStream sourceStream() {
        return sourceStream;
    }

    public Boolean noOverwriteDirNonDir() {
        return noOverwriteDirNonDir;
    }

}
