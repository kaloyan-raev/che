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
package org.eclipse.che.api.project.gwt.client;

import org.eclipse.che.api.machine.gwt.client.DevMachine;
import org.eclipse.che.api.machine.gwt.client.WsAgentStateController;
import org.eclipse.che.api.project.shared.dto.ProjectImporterData;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;

import javax.inject.Inject;

/**
 * @author Vitaly Parfonov
 */
public class ProjectImportersServiceClientImpl implements ProjectImportersServiceClient {

    private final AsyncRequestFactory asyncRequestFactory;
    private String wsAgentBaseUrl;

    @Inject
    public ProjectImportersServiceClientImpl(WsAgentStateController wsAgentStateController,
                                             AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        wsAgentStateController.getDevMachine().then(new Operation<DevMachine>() {
            @Override
            public void apply(DevMachine devMachine) throws OperationException {
                wsAgentBaseUrl = devMachine.getWsAgentBaseUrl();
            }
        });

    }

    @Override
    public void getProjectImporters(String workspaceId, AsyncRequestCallback<ProjectImporterData> callback) {
        asyncRequestFactory.createGetRequest(wsAgentBaseUrl + "/project-importers/" + workspaceId)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }
}
