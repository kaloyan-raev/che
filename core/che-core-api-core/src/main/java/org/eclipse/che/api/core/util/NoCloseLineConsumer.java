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
package org.eclipse.che.api.core.util;

import java.io.IOException;

/**
 * {@link LineConsumer} that doesn't have to be closed.
 * <p/>
 * Can be used to pass Java 8 lambda to method that accepts {@code LineConsumer}
 *
 * @author Alexander Garagatyi
 */
public interface NoCloseLineConsumer extends LineConsumer {
    @Override
    default void close() throws IOException {}
}
