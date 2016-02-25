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
package org.eclipse.che.ide.extension.maven.shared;

/**
 * @author Evgen Vidolob
 */
public enum MessageType {
    NOTIFICATION(1), UPDATE(2);

    private final int type;

    MessageType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static MessageType valueOf(int type) {
        for (MessageType messageType : values()) {
            if(messageType.type == type){
                return messageType;
            }
        }
        throw new IllegalArgumentException();
    }
}