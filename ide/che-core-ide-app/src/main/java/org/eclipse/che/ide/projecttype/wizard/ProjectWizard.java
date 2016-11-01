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
package org.eclipse.che.ide.projecttype.wizard;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.command.CommandImpl;
import org.eclipse.che.ide.api.command.CommandManager;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.resource.Path;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkState;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.IMPORT;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.UPDATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar.PROJECT_NAME_KEY;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar.WIZARD_MODE_KEY;
import static org.eclipse.che.ide.api.resources.Resource.FOLDER;
import static org.eclipse.che.ide.api.resources.Resource.PROJECT;

/**
 * Project wizard used for creating new a project or updating an existing one.
 *
 * @author Artem Zatsarynnyi
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 * @author Valeriy Svydenko
 */
public class ProjectWizard extends AbstractWizard<MutableProjectConfig> {
    
    private final static String PROJECT_PATH_MACRO_REGEX = "\\$\\{current.project.path\\}";

    private final ProjectWizardMode mode;
    private final AppContext        appContext;
    private final CommandManager    commandManager;

    @Inject
    public ProjectWizard(@Assisted MutableProjectConfig dataObject,
                         @Assisted ProjectWizardMode mode,
                         AppContext appContext,
                         CommandManager commandManager) {
        super(dataObject);
        this.mode = mode;
        this.appContext = appContext;
        this.commandManager = commandManager;

        context.put(WIZARD_MODE_KEY, mode.toString());
        context.put(PROJECT_NAME_KEY, dataObject.getName());
    }

    /** {@inheritDoc} */
    @Override
    public void complete(@NotNull final CompleteCallback callback) {
        if (mode == CREATE) {
            appContext.getWorkspaceRoot()
                      .newProject()
                      .withBody(dataObject)
                      .send()
                      .then(onComplete(callback))
                      .catchError(onFailure(callback));
        } else if (mode == UPDATE) {
            appContext.getWorkspaceRoot().getContainer(Path.valueOf(dataObject.getPath())).then(new Operation<Optional<Container>>() {
                @Override
                public void apply(Optional<Container> optContainer) throws OperationException {
                    checkState(optContainer.isPresent(), "Failed to update non existed path");

                    final Container container = optContainer.get();
                    if (container.getResourceType() == PROJECT) {
                        ((Project)container).update()
                                            .withBody(dataObject)
                                            .send()
                                            .then(onComplete(callback))
                                            .catchError(onFailure(callback));
                    } else if (container.getResourceType() == FOLDER) {
                        ((Folder)container).toProject()
                                           .withBody(dataObject)
                                           .send()
                                           .then(onComplete(callback))
                                           .catchError(onFailure(callback));
                    }
                }
            });
        } else if (mode == IMPORT) {
            appContext.getWorkspaceRoot().importProject().withBody(dataObject).send().thenPromise(new Function<Project, Promise<Project>>() {
                @Override
                public Promise<Project> apply(Project project) throws FunctionException {
                    return project.update().withBody(dataObject).send();
                }
            }).then(new Operation<Project>() {
                @Override
                public void apply(final Project project) throws OperationException {
                    Promise<CommandImpl> chain = Promises.resolve(new CommandImpl(null, null, null));
                    final String projectPath = appContext.getProjectsRoot().append(project.getPath()).toString();
                    for (final CommandDto command : dataObject.getCommands()) {
                        chain = chain.thenPromise(new Function<CommandImpl, Promise<CommandImpl>>() {
                            @Override
                            public Promise<CommandImpl> apply(CommandImpl ignored) throws FunctionException {
                                String name = project.getName() + ": " + command.getName();
                                String commandLine = command.getCommandLine().replaceAll(PROJECT_PATH_MACRO_REGEX, projectPath);
                                return commandManager.create(name, commandLine, command.getType(), command.getAttributes());
                            }
                        });
                    }
                    chain.then(new Operation<CommandImpl>() {
                        @Override
                        public void apply(CommandImpl ignored) throws OperationException {
                            callback.onCompleted();
                        }
                    }).catchError(onFailure(callback));
                }
            }).catchError(onFailure(callback));
        }
    }

    private Operation<Project> onComplete(final CompleteCallback callback) {
        return new Operation<Project>() {
            @Override
            public void apply(Project ignored) throws OperationException {
                callback.onCompleted();
            }
        };
    }

    private Operation<PromiseError> onFailure(final CompleteCallback callback) {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                callback.onFailure(error.getCause());
            }
        };
    }

}
