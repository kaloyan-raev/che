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
package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.LinkParameter;
import org.eclipse.che.api.machine.server.MachineService;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.SnapshotDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDto;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.util.LinksHelper.createLink;
import static org.eclipse.che.api.machine.shared.Constants.WSAGENT_REFERENCE;
import static org.eclipse.che.api.machine.shared.Constants.WSAGENT_WEBSOCKET_REFERENCE;
import static org.eclipse.che.api.workspace.shared.Constants.GET_ALL_USER_WORKSPACES;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_GET_SNAPSHOT;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_GET_WORKSPACE_EVENTS_CHANNEL;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_IDE_URL;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_REMOVE_WORKSPACE;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_SELF;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_START_WORKSPACE;
import static org.eclipse.che.api.workspace.shared.Constants.LINK_REL_STOP_WORKSPACE;
import static org.eclipse.che.api.workspace.shared.Constants.LIN_REL_GET_WORKSPACE;
import static org.eclipse.che.dto.server.DtoFactory.cloneDto;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author Anton Korneta
 */
@Singleton
public class LinksInjector {

    final String ideContext;

    @Inject
    public LinksInjector(String ideContext) {
        this.ideContext = ideContext;
    }

    public WorkspaceDto injectLinks(WorkspaceDto workspace, ServiceContext serviceContext) {
        final UriBuilder uriBuilder = serviceContext.getServiceUriBuilder();
        final List<Link> links = new ArrayList<>();
        // add common workspace links
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(WorkspaceService.class, "getByKey")
                                       .build(workspace.getId())
                                       .toString(),
                             LINK_REL_SELF));
        links.add(createLink("POST",
                             uriBuilder.clone()
                                       .path(WorkspaceService.class, "startById")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             LINK_REL_START_WORKSPACE));
        links.add(createLink("DELETE",
                             uriBuilder.clone()
                                       .path(WorkspaceService.class, "delete")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             LINK_REL_REMOVE_WORKSPACE));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(WorkspaceService.class, "getWorkspaces")
                                       .build()
                                       .toString(),
                             APPLICATION_JSON,
                             GET_ALL_USER_WORKSPACES));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(WorkspaceService.class, "getSnapshot")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             LINK_REL_GET_SNAPSHOT));

        //TODO here we add url to IDE with workspace name not good solution do it here but critical for this task  https://jira.codenvycorp.com/browse/IDEX-3619
        final URI ideUri = uriBuilder.clone()
                                     .replacePath(ideContext)
                                     .path(workspace.getConfig().getName())
                                     .build();
        links.add(createLink("GET", ideUri.toString(), TEXT_HTML, LINK_REL_IDE_URL));

        // add workspace channel link
        final Link workspaceChannelLink = createLink("GET",
                                                     serviceContext.getBaseUriBuilder()
                                                                        .path("ws")
                                                                        .path(workspace.getId())
                                                                        .scheme("https".equals(ideUri.getScheme()) ? "wss" : "ws")
                                                                        .build()
                                                                        .toString(),
                                                     null);
        final LinkParameter channelParameter = newDto(LinkParameter.class).withName("channel")
                                                                          .withRequired(true);

        links.add(cloneDto(workspaceChannelLink).withRel(LINK_REL_GET_WORKSPACE_EVENTS_CHANNEL)
                                                .withParameters(singletonList(
                                                        cloneDto(channelParameter).withDefaultValue("workspace:" + workspace.getId()))));

        // add machine channels links to machines configs
        workspace.getConfig()
                 .getEnvironments()
                 .stream()
                 .forEach(environmentDto -> injectMachineChannelsLinks(environmentDto,
                                                                       workspace.getId(),
                                                                       workspaceChannelLink,
                                                                       channelParameter));
        // add links for running workspace
        if (workspace.getStatus() == RUNNING) {
            workspace.getRuntime()
                     .getLinks()
                     .add(createLink("DELETE",
                                     uriBuilder.clone()
                                               .path(WorkspaceService.class, "stop")
                                               .build(workspace.getId())
                                               .toString(),
                                     LINK_REL_STOP_WORKSPACE));

            if (workspace.getRuntime() != null && workspace.getRuntime().getDevMachine() != null) {
                workspace.getRuntime()
                         .getDevMachine()
                         .getRuntime()
                         .getServers()
                         .values()
                         .stream()
                         .filter(server -> WSAGENT_REFERENCE.equals(server.getRef()))
                         .findAny()
                         .ifPresent(wsAgent -> {
                             workspace.getRuntime()
                                      .getLinks()
                                      .add(createLink("GET",
                                                      wsAgent.getUrl(),
                                                      WSAGENT_REFERENCE));
                             workspace.getRuntime()
                                      .getLinks()
                                      .add(createLink("GET",
                                                      UriBuilder.fromUri(wsAgent.getUrl())
                                                                .scheme("https".equals(ideUri.getScheme()) ? "wss" : "ws")
                                                                .userInfo("codenvy:token")
                                                                .build()
                                                                .toString(),
                                                      WSAGENT_WEBSOCKET_REFERENCE));
                         });
            }
        }
        return workspace.withLinks(links);
    }

    private void injectMachineChannelsLinks(EnvironmentDto environmentDto,
                                            String workspaceId,
                                            Link machineChannelLink,
                                            LinkParameter channelParameter) {

        for (MachineConfigDto machineConfigDto : environmentDto.getMachineConfigs()) {
            MachineService.injectMachineChannelsLinks(machineConfigDto,
                                                      workspaceId,
                                                      environmentDto.getName(),
                                                      machineChannelLink,
                                                      channelParameter);
        }
    }

    public SnapshotDto injectLinks(SnapshotDto snapshotDto, ServiceContext serviceContext) {
        final UriBuilder uriBuilder = serviceContext.getServiceUriBuilder();
        final Link machineLink = createLink("GET",
                                            serviceContext.getBaseUriBuilder()
                                                               .path("/machine/{id}")
                                                               .build(snapshotDto.getId())
                                                               .toString(),
                                            APPLICATION_JSON,
                                            "get machine");
        final Link workspaceLink = createLink("GET",
                                              uriBuilder.clone()
                                                        .path(WorkspaceService.class, "getByKey")
                                                        .build(snapshotDto.getWorkspaceId())
                                                        .toString(),
                                              APPLICATION_JSON,
                                              LIN_REL_GET_WORKSPACE);
        final Link workspaceSnapshotLink = createLink("GET",
                                                      uriBuilder.clone()
                                                                .path(WorkspaceService.class, "getSnapshot")
                                                                .build(snapshotDto.getWorkspaceId())
                                                                .toString(),
                                                      APPLICATION_JSON,
                                                      LINK_REL_SELF);
        return snapshotDto.withLinks(asList(machineLink, workspaceLink, workspaceSnapshotLink));
    }
}
