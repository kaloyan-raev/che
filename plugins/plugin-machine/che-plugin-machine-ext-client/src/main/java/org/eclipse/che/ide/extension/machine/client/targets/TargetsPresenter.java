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
package org.eclipse.che.ide.extension.machine.client.targets;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.machine.gwt.client.MachineManager;
import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.LimitsDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.machine.shared.dto.recipe.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeUpdate;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.notification.StatusNotification;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.extension.machine.client.MachineLocalizationConstant;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.util.StringUtils;
import org.eclipse.che.ide.util.loging.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Targets manager presenter.
 *
 * @author Vitaliy Guliy
 */
public class TargetsPresenter implements TargetsView.ActionDelegate {

    private final TargetsView                           view;
    private final RecipeServiceClient                   recipeServiceClient;
    private final DtoFactory                            dtoFactory;
    private final DialogFactory                         dialogFactory;
    private final MachineManager                        machineManager;
    private final NotificationManager                   notificationManager;
    private final MachineLocalizationConstant           machineLocale;
    private final AppContext                            appContext;
    private final MachineServiceClient                  machineService;
    private final WorkspaceServiceClient                workspaceServiceClient;

    private final List<Target>                          targets = new ArrayList<>();
    private Target                                      selectedTarget;
    private final List<MachineDto>                      machines = new ArrayList<>();

    @Inject
    public TargetsPresenter(final TargetsView view,
                            final RecipeServiceClient recipeServiceClient,
                            final DtoFactory dtoFactory,
                            final DialogFactory dialogFactory,
                            final MachineManager machineManager,
                            final NotificationManager notificationManager,
                            final MachineLocalizationConstant machineLocale,
                            final AppContext appContext,
                            final MachineServiceClient machineService,
                            final WorkspaceServiceClient workspaceServiceClient) {
        this.view = view;
        this.recipeServiceClient = recipeServiceClient;
        this.dtoFactory = dtoFactory;
        this.dialogFactory = dialogFactory;
        this.machineManager = machineManager;
        this.notificationManager = notificationManager;
        this.machineLocale = machineLocale;
        this.appContext = appContext;
        this.machineService = machineService;
        this.workspaceServiceClient = workspaceServiceClient;

        view.setDelegate(this);
    }

    /**
     * Opens Targets popup.
     */
    public void edit() {
        view.show();
        view.clear();

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                updateTargets(null);
            }
        });
    }

    /**
     * Fetches all recipes from the server, makes a list of targets and selects specified target.
     */
    private void updateTargets(final String targetToSelect) {
        targets.clear();
        machines.clear();

        machineService.getMachines(appContext.getWorkspaceId()).then(new Operation<List<MachineDto>>() {
            @Override
            public void apply(List<MachineDto> list) throws OperationException {
                machines.addAll(list);

                recipeServiceClient.getAllRecipes().then(new Operation<List<RecipeDescriptor>>() {
                    @Override
                    public void apply(List<RecipeDescriptor> recipeList) throws OperationException {
                        for (RecipeDescriptor recipe : recipeList) {
                            // Display only "ssh" recipes
                            if (!"ssh".equalsIgnoreCase(recipe.getType())) {
                                continue;
                            }

                            Target target = new Target(recipe.getName(), recipe.getType(), recipe);
                            target.setRecipe(recipe);
                            targets.add(target);

                            target.setConnected(isMachineRunning(recipe.getName()));

                            restoreTarget(target);
                        }

                        view.showTargets(targets);

                        if (targetToSelect != null) {
                            for (Target target : targets) {
                                if (target.getName().equals(targetToSelect)) {
                                    view.selectTarget(target);
                                    break;
                                }
                            }
                        }
                    }
                });

            }
        });
    }

    /**
     * Determines whether machine is running or not.
     *
     * @return
     *      true for running machine
     */
    private boolean isMachineRunning(String machineName) {
        for (MachineDto machine : machines) {
            if (machine.getConfig().getName().equals(machineName) &&
                    "persistent".equals(machine.getConfig().getType()) &&
                    machine.getStatus() == MachineStatus.RUNNING) {
                return true;
            }
        }

        return false;
    }

    /**
     * Restore target properties from its recipe.
     *
     * @param target
     *          target to rectore
     */
    private void restoreTarget(Target target) {
        try {
            JSONObject json = JSONParser.parseStrict(target.getRecipe().getScript()).isObject();

            String host = json.get("host").isString().stringValue();
            String port = json.get("port").isString().stringValue();
            String username = json.get("username").isString().stringValue();
            String password = json.get("password").isString().stringValue();

            target.setHost(host);
            target.setPort(port);
            target.setUserName(username);
            target.setPassword(password);

        } catch (Exception e) {
            Log.error(TargetsPresenter.class, "Unable to parse recipe JSON. " + e.getMessage());
        }
    }

    @Override
    public void onCloseClicked() {
        view.hide();
    }

    @Override
    public void onAddTarget(String category) {
        Target target = new Target("[new target]", "ssh");
        target.setHost("127.0.0.1");
        target.setPort("22");
        target.setUserName("root");
        target.setPassword("root");
        target.setDirty(true);
        target.setConnected(false);
        targets.add(target);

        view.showTargets(targets);
        view.selectTarget(target);
    }

    @Override
    public void onTargetSelected(Target target) {
        if (target == null) {
            view.showHintPanel();
            return;
        }

        if ("docker".equalsIgnoreCase(target.getType())) {
            view.showInfoPanel();

        } else if ("ssh".equalsIgnoreCase(target.getType())) {
            view.showPropertiesPanel();
            view.setTargetName(target.getName());

            view.setHost(target.getHost());
            view.setPort(target.getPort());
            view.setUserName(target.getUserName());
            view.setPassword(target.getPassword());

            view.selectTargetName();
        } else {
            view.showInfoPanel();
        }

        selectedTarget = target;
        updateButtons();
    }

    @Override
    public void onTargetNameChanged(String value) {
        if (selectedTarget.getName().equals(value)) {
            return;
        }

        selectedTarget.setName(value);
        selectedTarget.setDirty(true);
        updateButtons();
    }

    @Override
    public void onHostChanged(String value) {
        if (selectedTarget.getHost().equals(value)) {
            return;
        }

        selectedTarget.setHost(value);
        selectedTarget.setDirty(true);
        updateButtons();
    }

    @Override
    public void onPortChanged(String value) {
        if (selectedTarget.getPort().equals(value)) {
            return;
        }

        selectedTarget.setPort(value);
        selectedTarget.setDirty(true);
        updateButtons();
    }

    @Override
    public void onUserNameChanged(String value) {
        if (selectedTarget.getUserName().equals(value)) {
            return;
        }

        selectedTarget.setUserName(value);
        selectedTarget.setDirty(true);
        updateButtons();
    }

    @Override
    public void onPasswordChanged(String value) {
        if (selectedTarget.getPassword().equals(value)) {
            return;
        }

        selectedTarget.setPassword(value);
        selectedTarget.setDirty(true);
        updateButtons();
    }

    /**
     * Updates buttons state.
     */
    private void updateButtons() {
        if (selectedTarget == null) {
            return;
        }

        view.enableConnectButton(!selectedTarget.isDirty());

        if (selectedTarget.isConnected()) {
            view.setConnectButtonText("Disconnect");
        } else {
            view.setConnectButtonText("Connect");
        }

        view.enableCancelButton(selectedTarget.isDirty());

        if (StringUtils.isNullOrEmpty(view.getTargetName()) ||
                StringUtils.isNullOrEmpty(view.getHost()) ||
                StringUtils.isNullOrEmpty(view.getPort())) {
            view.enableSaveButton(false);
        } else {
            view.enableSaveButton(selectedTarget.isDirty());
        }
    }

    @Override
    public void onSaveClicked() {
        // Save only SSH target
        if (!"ssh".equals(selectedTarget.getType())) {
            return;
        }

        if (selectedTarget.getRecipe() == null) {
            createTarget();
        } else {
            updateTarget();
        }
    }

    /**
     * Creates a new target.
     */
    private void createTarget() {
        List<String> tags = new ArrayList<>();
        tags.add("ssh");

        NewRecipe newRecipe = dtoFactory.createDto(NewRecipe.class)
                .withName(selectedTarget.getName())
                .withType("ssh")
                .withScript("{\"" +
                        "host\": \"" + selectedTarget.getHost() + "\", " +
                        "\"port\": \"" + selectedTarget.getPort() + "\", " +
                        "\"username\": \"" + selectedTarget.getUserName() + "\", " +
                        "\"password\": \"" + selectedTarget.getPassword() + "\"}")
                .withTags(tags);

        Promise<RecipeDescriptor> createRecipe = recipeServiceClient.createRecipe(newRecipe);
        createRecipe.then(new Operation<RecipeDescriptor>() {
            @Override
            public void apply(RecipeDescriptor recipe) throws OperationException {
                onTargetSaved(recipe);
            }
        });

        createRecipe.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                //notificationManager.notify(machineLocale.targetsViewSaveError(), StatusNotification.Status.FAIL, true);
                dialogFactory.createMessageDialog("Error", machineLocale.targetsViewSaveError(), null).show();
            }
        });
    }

    /**
     * Updates as existent target.
     */
    private void updateTarget() {
        RecipeUpdate recipeUpdate = dtoFactory.createDto(RecipeUpdate.class)
                .withId(selectedTarget.getRecipe().getId())
                .withName(view.getTargetName())
                .withType(selectedTarget.getRecipe().getType())
                .withTags(selectedTarget.getRecipe().getTags())
                .withDescription(selectedTarget.getRecipe().getDescription())
                .withPermissions(selectedTarget.getRecipe().getPermissions())
                .withScript("{\"" +
                        "host\": \"" + selectedTarget.getHost() + "\", " +
                        "\"port\": \"" + selectedTarget.getPort() + "\", " +
                        "\"username\": \"" + selectedTarget.getUserName() + "\", " +
                        "\"password\": \"" + selectedTarget.getPassword() + "\"}");

        Promise<RecipeDescriptor> updateRecipe = recipeServiceClient.updateRecipe(recipeUpdate);
        updateRecipe.then(new Operation<RecipeDescriptor>() {
            @Override
            public void apply(RecipeDescriptor recipe) throws OperationException {
                onTargetSaved(recipe);
            }
        });

        updateRecipe.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                //notificationManager.notify(machineLocale.targetsViewSaveError(), StatusNotification.Status.FAIL, true);
                dialogFactory.createMessageDialog("Error", machineLocale.targetsViewSaveError(), null).show();
            }
        });
    }

    /**
     * Performs actions when target is saved.
     */
    private void onTargetSaved(RecipeDescriptor recipe) {
        selectedTarget.setRecipe(recipe);
        selectedTarget.setDirty(false);

        view.showTargets(targets);
        view.selectTarget(selectedTarget);

        //updateButtons();
        notificationManager.notify(machineLocale.targetsViewSaveSuccess(), StatusNotification.Status.SUCCESS, true);
    }

    @Override
    public void onCancelClicked() {
        if (selectedTarget.getRecipe() == null) {
            targets.remove(selectedTarget);
            view.showTargets(targets);

            view.selectTarget(null);
            view.showHintPanel();

            return;
        }

        selectedTarget.setName(selectedTarget.getRecipe().getName());
        restoreTarget(selectedTarget);
        selectedTarget.setDirty(false);
        view.selectTarget(selectedTarget);
    }

    @Override
    public void onConnectClicked() {
        if (selectedTarget == null || selectedTarget.getRecipe() == null) {
            return;
        }

        if (selectedTarget.isConnected()) {
            disconnect();
        } else {
            connect();
        }
    }

    private StatusNotification connectNotification;

    /**
     * Opens a connection to the selected target.
     * Starts a machine based on the selected recipe.
     */
    private void connect() {
        view.setConnectButtonText(null);

        connectNotification = notificationManager.notify(machineLocale.targetsViewConnectProgress(selectedTarget.getName()), StatusNotification.Status.PROGRESS, true);

        String recipeURL = selectedTarget.getRecipe().getLink("get recipe script").getHref();

        LimitsDto limitsDto = dtoFactory.createDto(LimitsDto.class).withRam(1024);
        MachineSourceDto sourceDto = dtoFactory.createDto(MachineSourceDto.class).withType("ssh-config").withLocation(recipeURL);

        MachineConfigDto configDto = dtoFactory.createDto(MachineConfigDto.class)
                .withDev(false)
                .withName(selectedTarget.getName())
                .withSource(sourceDto)
                .withLimits(limitsDto)
                .withType("persistent");

        Promise<MachineDto> machinePromise = workspaceServiceClient.createMachine(appContext.getWorkspace().getId(), configDto);

        machinePromise.then(new Operation<MachineDto>() {
            @Override
            public void apply(final MachineDto machineDto) throws OperationException {
                ensureMachineIsStarted(machineDto.getId());
            }
        });

        machinePromise.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError promiseError) throws OperationException {
                onConnectingFailed();
            }
        });
    }

    /**
     * Ensures machine is started.
     */
    private void ensureMachineIsStarted(final String machineId) {
        machineService.getMachine(machineId).then(new Operation<MachineDto>() {
            @Override
            public void apply(MachineDto machineDto) throws OperationException {
                if (machineDto.getStatus() == MachineStatus.RUNNING) {
                    onConnected();
                } else {
                    new Timer() {
                        @Override
                        public void run() {
                            ensureMachineIsStarted(machineId);
                        }
                    }.schedule(1000);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                onConnectingFailed();
            }
        });
    }

    /**
     * Displays a notification
     */
    private void onConnected() {
        connectNotification.setTitle(machineLocale.targetsViewConnectSuccess(selectedTarget.getName()));
        connectNotification.setStatus(StatusNotification.Status.SUCCESS);
        updateTargets(selectedTarget.getName());
    }

    private void onConnectingFailed() {
        connectNotification.setTitle(machineLocale.targetsViewConnectError(selectedTarget.getName()));
        connectNotification.setStatus(StatusNotification.Status.FAIL);

        view.selectTarget(selectedTarget);
    }

    /**
     * Closes the connection to the selected target.
     * Destroys a machine based on the selected recipe.
     */
    private void disconnect() {
        if (selectedTarget == null || !selectedTarget.isConnected()) {
            return;
        }

        String machineId = null;
        for (MachineDto machine : machines) {
            if (machine.getConfig().getName().equals(selectedTarget.getName()) &&
                    "persistent".equals(machine.getConfig().getType()) &&
                    machine.getStatus() == MachineStatus.RUNNING) {
                machineId = machine.getId();
                break;
            }
        }

        if (machineId == null) {
            return;
        }

        view.setConnectButtonText(null);

        machineService.destroyMachine(machineId).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                notificationManager.notify(machineLocale.targetsViewDisconnectSuccess(selectedTarget.getName()), StatusNotification.Status.SUCCESS, true);
                updateTargets(selectedTarget.getName());
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.notify(machineLocale.targetsViewDisconnectError(selectedTarget.getName()), StatusNotification.Status.FAIL, true);
                updateTargets(selectedTarget.getName());
            }
        });
    }

    @Override
    public void onDeleteTarget(final Target target) {
        dialogFactory.createConfirmDialog("IDE", machineLocale.targetsViewDeleteConfirm(target.getName()),
                new ConfirmCallback() {
                    @Override
                    public void accepted() {
                        if (target.isConnected()) {
                            disconnectAndDelete(target);
                        } else {
                            deleteTarget(target);
                        }
                    }
                }, new CancelCallback() {
                    @Override
                    public void cancelled() {
                    }
                }).show();
    }

    private void disconnectAndDelete(final Target target) {
        String machineId = null;
        for (MachineDto machine : machines) {
            if (machine.getConfig().getName().equals(target.getName()) &&
                    "persistent".equals(machine.getConfig().getType()) &&
                    machine.getStatus() == MachineStatus.RUNNING) {
                machineId = machine.getId();
                break;
            }
        }

        if (machineId == null) {
            return;
        }

        machineService.destroyMachine(machineId).then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                notificationManager.notify(machineLocale.targetsViewDisconnectSuccess(target.getName()), StatusNotification.Status.SUCCESS, true);
                new Timer() {
                    @Override
                    public void run() {
                        deleteTarget(target);
                    }
                }.schedule(1000);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.notify(machineLocale.targetsViewDisconnectError(target.getName()), StatusNotification.Status.FAIL, true);
                updateTargets(target.getName());
            }
        });
    }

    /**
     * Deletes specified  target.
     *
     * @param target
     *          target to delete
     */
    private void deleteTarget(final Target target) {
        Promise<Void> deletePromice = recipeServiceClient.removeRecipe(target.getRecipe().getId());
        deletePromice.then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                targets.remove(target);
                view.showTargets(targets);

                view.selectTarget(null);
                view.showHintPanel();

                notificationManager.notify(machineLocale.targetsViewDeleteSuccess(target.getName()), StatusNotification.Status.SUCCESS, true);
            }
        });

        deletePromice.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                dialogFactory.createMessageDialog("Error", machineLocale.targetsViewDeleteError(target.getName()), null).show();
            }
        });

    }

}