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
 * Arguments holder for {@link org.eclipse.che.plugin.docker.client.DockerConnector#tag(TagParams)}.
 *
 * @author Mykola Morhun
 */
public class TagParams {

    private String  image;
    private String  repository;
    private String  tag;
    private Boolean force;

    /**
     * Creates arguments holder with required parameters.
     *
     * @param image
     *         info about this parameter @see {@link #withImage(String)}
     * @param repository
     *         info about this parameter @see {@link #withRepository(String)}
     * @return arguments holder with required parameters
     */
    public static TagParams from(@NotNull String image, @NotNull String repository) {
        return new TagParams().withImage(image)
                              .withRepository(repository);
    }

    private TagParams() {}

    /**
     * @param image
     *         image name
     */
    public TagParams withImage(@NotNull String image) {
        requireNonNull(image);
        this.image = image;
        return this;
    }

    /**
     * @param repository
     *         the repository to tag in
     */
    public TagParams withRepository(@NotNull String repository) {
        requireNonNull(repository);
        this.repository = repository;
        return this;
    }

    /**
     * @param tag
     *         new tag name
     */
    public TagParams withTag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * @param force
     *         force tagging of the image
     */
    public TagParams withForce(boolean force) {
        this.force = force;
        return this;
    }

    public String image() {
        return image;
    }

    public String repository() {
        return repository;
    }

    public String tag() {
        return tag;
    }

    public Boolean force() {
        return force;
    }

}
