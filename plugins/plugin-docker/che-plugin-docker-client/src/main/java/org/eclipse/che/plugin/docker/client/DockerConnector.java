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
package org.eclipse.che.plugin.docker.client;

import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonNameConvention;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.TarUtils;
import org.eclipse.che.commons.lang.ws.rs.ExtMediaType;
import org.eclipse.che.plugin.docker.client.connection.CloseConnectionInputStream;
import org.eclipse.che.plugin.docker.client.connection.DockerConnection;
import org.eclipse.che.plugin.docker.client.connection.DockerConnectionFactory;
import org.eclipse.che.plugin.docker.client.connection.DockerResponse;
import org.eclipse.che.plugin.docker.client.dto.AuthConfigs;
import org.eclipse.che.plugin.docker.client.json.ContainerCommitted;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.eclipse.che.plugin.docker.client.json.ContainerCreated;
import org.eclipse.che.plugin.docker.client.json.ContainerExitStatus;
import org.eclipse.che.plugin.docker.client.json.ContainerInfo;
import org.eclipse.che.plugin.docker.client.json.ContainerProcesses;
import org.eclipse.che.plugin.docker.client.json.Event;
import org.eclipse.che.plugin.docker.client.json.ExecConfig;
import org.eclipse.che.plugin.docker.client.json.ExecCreated;
import org.eclipse.che.plugin.docker.client.json.ExecInfo;
import org.eclipse.che.plugin.docker.client.json.ExecStart;
import org.eclipse.che.plugin.docker.client.json.Filters;
import org.eclipse.che.plugin.docker.client.json.HostConfig;
import org.eclipse.che.plugin.docker.client.json.Image;
import org.eclipse.che.plugin.docker.client.json.ImageInfo;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;
import org.eclipse.che.plugin.docker.client.json.Version;
import org.eclipse.che.plugin.docker.client.params.AttachContainerParams;
import org.eclipse.che.plugin.docker.client.params.BuildImageParams;
import org.eclipse.che.plugin.docker.client.params.CommitParams;
import org.eclipse.che.plugin.docker.client.params.CreateContainerParams;
import org.eclipse.che.plugin.docker.client.params.CreateExecParams;
import org.eclipse.che.plugin.docker.client.params.GetEventsParams;
import org.eclipse.che.plugin.docker.client.params.GetExecInfoParams;
import org.eclipse.che.plugin.docker.client.params.GetResourceParams;
import org.eclipse.che.plugin.docker.client.params.InspectContainerParams;
import org.eclipse.che.plugin.docker.client.params.InspectImageParams;
import org.eclipse.che.plugin.docker.client.params.KillContainerParams;
import org.eclipse.che.plugin.docker.client.params.PullParams;
import org.eclipse.che.plugin.docker.client.params.PushParams;
import org.eclipse.che.plugin.docker.client.params.PutResourceParams;
import org.eclipse.che.plugin.docker.client.params.RemoveContainerParams;
import org.eclipse.che.plugin.docker.client.params.RemoveImageParams;
import org.eclipse.che.plugin.docker.client.params.StartContainerParams;
import org.eclipse.che.plugin.docker.client.params.StartExecParams;
import org.eclipse.che.plugin.docker.client.params.StopContainerParams;
import org.eclipse.che.plugin.docker.client.params.TagParams;
import org.eclipse.che.plugin.docker.client.params.TopParams;
import org.eclipse.che.plugin.docker.client.params.WaitContainerParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * Client for docker API.
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 * @author Anton Korneta
 * @author Mykola Morhun
 */
@Singleton
public class DockerConnector {
    private static final Logger LOG = LoggerFactory.getLogger(DockerConnector.class);

    private final URI                     dockerDaemonUri;
    private final InitialAuthConfig       initialAuthConfig;
    private final ExecutorService         executor;
    private final DockerConnectionFactory connectionFactory;

    @Inject
    public DockerConnector(DockerConnectorConfiguration connectorConfiguration, 
                           DockerConnectionFactory connectionFactory) {
        this.dockerDaemonUri = connectorConfiguration.getDockerDaemonUri();
        this.initialAuthConfig = connectorConfiguration.getAuthConfigs();
        this.connectionFactory = connectionFactory;
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                         .setNameFormat("DockerApiConnector-%d")
                                                         .setDaemon(true)
                                                         .build());
    }

    /**
     * Gets system-wide information.
     *
     * @return system-wide information
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public org.eclipse.che.plugin.docker.client.json.SystemInfo getSystemInfo() throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/info")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), org.eclipse.che.plugin.docker.client.json.SystemInfo.class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Gets docker version.
     *
     * @return information about version docker
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public Version getVersion() throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/version")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), Version.class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Lists docker images.
     *
     * @return list of docker images
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public Image[] listImages() throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/images/json")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), Image[].class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Builds new docker image from specified dockerfile.
     *
     * @param repository
     *         full repository name to be applied to newly created image
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @param authConfigs
     *         Authentication configuration for private registries. Can be null.
     * @param memoryLimit
     *         Memory limit for build in bytes
     * @param memorySwapLimit
     *         Total memory in bytes (memory + swap), -1 to enable unlimited swap
     * @param files
     *         files that are needed for creation docker images (e.g. file of directories used in ADD instruction in Dockerfile), one of
     *         them must be Dockerfile.
     * @return image id
     * @throws IOException
     * @throws InterruptedException
     *         if build process was interrupted
     * @deprecated use {@link #buildImage(BuildImageParams, ProgressMonitor)} instead
     */
    @Deprecated
    public String buildImage(String repository,
                             ProgressMonitor progressMonitor,
                             AuthConfigs authConfigs,
                             boolean doForcePull,
                             long memoryLimit,
                             long memorySwapLimit,
                             File... files) throws IOException, InterruptedException {
            return doBuildImage(BuildImageParams.from(files)
                                                .withRepository(repository)
                                                .withAuthConfigs(authConfigs)
                                                .withDoForcePull(doForcePull)
                                                .withMemoryLimit(memoryLimit)
                                                .withMemorySwapLimit(memorySwapLimit),
                                progressMonitor,
                                dockerDaemonUri);
    }

    /**
     * Gets detailed information about docker image.
     *
     * @return detailed information about {@code image}
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public ImageInfo inspectImage(InspectImageParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/images/" + params.image() + "/json")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ImageInfo.class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Stops container.
     *
     * @param container
     *         container identifier, either id or name
     * @param timeout
     *         time to wait for the container to stop before killing it
     * @param timeunit
     *         time unit of the timeout parameter
     * @throws IOException
     * @deprecated use {@link #stopContainer(StopContainerParams)} instead
     */
    @Deprecated
    public void stopContainer(String container, long timeout, TimeUnit timeunit) throws IOException {
        stopContainer(StopContainerParams.from(container)
                                         .withTimeout(timeout, timeunit));
    }

    /**
     * Stops container.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void stopContainer(final StopContainerParams params) throws IOException {
        final Long timeout = (params.timeunit() == null) ?
                             params.timeout() : params.timeunit().toSeconds(params.timeout());

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/stop")) {
            addQueryParamIfSet(connection, "t", timeout);
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (!(NO_CONTENT.getStatusCode() == status || NOT_MODIFIED.getStatusCode() == status)) {
                throw getDockerException(response);
            }
        }
    }

    /**
     * Sends specified signal to running container.
     * If signal not set, then SIGKILL will be used.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void killContainer(final KillContainerParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/kill")) {
            addQueryParamIfSet(connection, "signal", params.signal());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (NO_CONTENT.getStatusCode() != status) {
                throw getDockerException(response);
            }
        }
    }

    /**
     * Kills container with SIGKILL signal.
     *
     * @param container
     *         container identifier, either id or name
     * @throws IOException
     * @deprecated use {@link #killContainer(KillContainerParams)} instead
     */
    @Deprecated
    public void killContainer(String container) throws IOException {
        killContainer(KillContainerParams.from(container)
                                         .withSignal(9));
    }

    /**
     * Removes container.
     *
     * @param container
     *         container identifier, either id or name
     * @param force
     *         if {@code true} kills the running container then remove it
     * @param removeVolumes
     *         if {@code true} removes volumes associated to the container
     * @throws IOException
     *         when problems occurs with docker api calls
     * @deprecated use {@link #removeContainer(RemoveContainerParams)} instead
     */
    @Deprecated
    public void removeContainer(String container, boolean force, boolean removeVolumes) throws IOException {
        removeContainer(RemoveContainerParams.from(container)
                                             .withForce(force)
                                             .withRemoveVolumes(removeVolumes));
    }

    /**
     * Removes docker container.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void removeContainer(final RemoveContainerParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("DELETE")
                                                            .path("/containers/" + params.container())) {
            addQueryParamIfSet(connection, "force", params.force());
            addQueryParamIfSet(connection, "v", params.removeVolumes());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (NO_CONTENT.getStatusCode() != status) {
                throw getDockerException(response);
            }
        }
    }

    /**
     * Blocks until container stops, then returns the exit code
     *
     * @return exit code
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public int waitContainer(final WaitContainerParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/wait")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ContainerExitStatus.class).getStatusCode();
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Gets detailed information about docker container.
     *
     * @param container
     *         id of container
     * @return detailed information about {@code container}
     * @throws IOException
     * @deprecated use {@link #inspectContainer(InspectContainerParams)} instead
     */
    @Deprecated
    public ContainerInfo inspectContainer(String container) throws IOException {
        return inspectContainer(InspectContainerParams.from(container));
    }

    /**
     * Gets detailed information about docker container.
     *
     * @return detailed information about {@code container}
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public ContainerInfo inspectContainer(final InspectContainerParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/containers/" + params.container() + "/json")) {
            addQueryParamIfSet(connection, "size", params.returnContainerSize());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ContainerInfo.class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Attaches to the container with specified id.
     * <br/>
     * Note, that if @{code stream} parameter is {@code true} then get 'live' stream from container.
     * Typically need to run this method in separate thread, if {@code stream}
     * is {@code true} since this method blocks until container is running.
     * @param containerLogsProcessor
     *         output for container logs
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void attachContainer(final AttachContainerParams params, MessageProcessor<LogMessage> containerLogsProcessor)
            throws IOException {
        final Boolean stream = params.stream();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/attach")
                                                            .query("stdout", 1)
                                                            .query("stderr", 1)) {
            addQueryParamIfSet(connection, "stream", stream);
            addQueryParamIfSet(connection, "logs", stream);
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            try (InputStream responseStream = response.getInputStream()) {
                new LogMessagePumper(responseStream, containerLogsProcessor).start();
            }
        }
    }

    /**
     * Attaches to the container with specified id.
     *
     * @param container
     *         id of container
     * @param containerLogsProcessor
     *         output for container logs
     * @param stream
     *         if {@code true} then get 'live' stream from container. Typically need to run this method in separate thread, if {@code
     *         stream} is {@code true} since this method blocks until container is running.
     * @throws IOException
     * @deprecated use {@link #attachContainer(AttachContainerParams, MessageProcessor)} instead
     */
    @Deprecated
    public void attachContainer(String container, MessageProcessor<LogMessage> containerLogsProcessor, boolean stream) throws IOException {
        attachContainer(AttachContainerParams.from(container)
                                             .withStream(stream),
                        containerLogsProcessor);
    }

    @Deprecated
    public Exec createExec(String container, boolean detach, String... cmd) throws IOException {
        return createExec(CreateExecParams.from(container, cmd)
                                          .withDetach(detach));
    }

    /**
     * Sets up an exec instance in a running container.
     *
     * @return just created exec info
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public Exec createExec(final CreateExecParams params) throws IOException {
        final ExecConfig execConfig = new ExecConfig().withCmd(params.cmd());
        if (params.detach() != null && !params.detach()) {
            execConfig.withAttachStderr(true).withAttachStdout(true);
        }
        final String entity = JsonHelper.toJson(execConfig, FIRST_LETTER_LOWERCASE);
        byte[] entityBytesArray = entity.getBytes();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/exec")
                                                            .header(Pair.of("Content-Type", MediaType.APPLICATION_JSON))
                                                            .header(Pair.of("Content-Length", entityBytesArray.length))
                                                            .entity(entityBytesArray)) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (status / 100 != 2) {
                throw getDockerException(response);
            }
            return new Exec(params.cmd(), parseResponseStreamAndClose(response.getInputStream(), ExecCreated.class).getId());
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @deprecated use {@link #startExec(StartExecParams, MessageProcessor)} instead
     */
    @Deprecated
    public void startExec(String execId, MessageProcessor<LogMessage> execOutputProcessor) throws IOException {
        startExec(StartExecParams.from(execId), execOutputProcessor);
    }

    /**
     * Starts a previously set up exec instance.
     *
     * @param execOutputProcessor
     *         processor for exec output
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void startExec(final StartExecParams params, MessageProcessor<LogMessage> execOutputProcessor) throws IOException {
        final ExecStart execStart = new ExecStart().withDetach(execOutputProcessor == null);
        if (params.detach() != null) {
            execStart.withDetach(params.detach());
        }
        if (params.tty() != null) {
            execStart.withTty(params.tty());
        }

        final String entity = JsonHelper.toJson(execStart, FIRST_LETTER_LOWERCASE);
        byte[] entityBytesArray = entity.getBytes();
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/exec/" + params.execId() + "/start")
                                                            .header(Pair.of("Content-Type", MediaType.APPLICATION_JSON))
                                                            .header(Pair.of("Content-Length", entityBytesArray.length))
                                                            .entity(entityBytesArray)) {

            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            // According to last doc (https://docs.docker.com/reference/api/docker_remote_api_v1.15/#exec-start) status must be 201 but
            // in fact docker API returns 200 or 204 status.
            if (status / 100 != 2) {
                throw getDockerException(response);
            }
            if (status != NO_CONTENT.getStatusCode() && execOutputProcessor != null) {
                try (InputStream responseStream = response.getInputStream()) {
                    new LogMessagePumper(responseStream, execOutputProcessor).start();
                }
            }
        }
    }

    /**
     * Gets detailed information about exec
     *
     * @return detailed information about {@code execId}
     * @throws IOException
     * @deprecated use {@link #getExecInfo(GetExecInfoParams)} instead
     */
    @Deprecated
    public ExecInfo getExecInfo(String execId) throws IOException {
        return getExecInfo(GetExecInfoParams.from(execId));
    }

    /**
     * Gets detailed information about exec
     *
     * @return detailed information about {@code execId}
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public ExecInfo getExecInfo(final GetExecInfoParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/exec/" + params.execId() + "/json")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ExecInfo.class);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @deprecated use {@link #top(TopParams)} instead
     */
    @Deprecated
    public ContainerProcesses top(String container, String... psArgs) throws IOException {
        return top(TopParams.from(container)
                            .withPsArgs(psArgs));
    }

    /**
     * List processes running inside the container.
     *
     * @return processes running inside the container
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public ContainerProcesses top(final TopParams params) throws IOException {
        final String[] psArgs = params.psArgs();

        try (final DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                                  .method("GET")
                                                                  .path("/containers/" + params.container() + "/top")) {
            if (psArgs != null && psArgs.length != 0) {
                StringBuilder psArgsQueryBuilder = new StringBuilder();
                for (int i = 0, l = psArgs.length; i < l; i++) {
                    if (i > 0) {
                        psArgsQueryBuilder.append('+');
                    }
                    psArgsQueryBuilder.append(URLEncoder.encode(psArgs[i], "UTF-8"));
                }
                connection.query("ps_args", psArgsQueryBuilder.toString());
            }

            try {
                final DockerResponse response = connection.request();
                final int status = response.getStatus();
                if (OK.getStatusCode() != status) {
                    throw getDockerException(response);
                }
                return parseResponseStreamAndClose(response.getInputStream(), ContainerProcesses.class);
            } catch (JsonParseException e) {
                throw new IOException(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Gets files from the specified container.
     *
     * @param container
     *         container id
     * @param sourcePath
     *         path to file or directory inside specified container
     * @return stream of resources from the specified container filesystem, with retention connection
     * @throws IOException
     *         when problems occurs with docker api calls
     * @apiNote this method implements 1.20 docker API and requires docker not less than 1.8.0 version
     * @deprecated use {@link #getResource(GetResourceParams)} instead
     */
    @Deprecated
    public InputStream getResource(String container, String sourcePath) throws IOException {
       return getResource(GetResourceParams.from(container, sourcePath));
    }

    /**
     * Gets files from the specified container.
     *
     * @return stream of resources from the specified container filesystem, with retention connection
     * @throws IOException
     *         when problems occurs with docker api calls
     * @apiNote this method implements 1.20 docker API and requires docker not less than 1.8.0 version
     */
    public InputStream getResource(final GetResourceParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/containers/" + params.container() + "/archive")
                                                            .query("path", params.sourcePath())) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (status != OK.getStatusCode()) {
                throw getDockerException(response);
            }
            return new CloseConnectionInputStream(response.getInputStream(), connection);
        }
    }

    /**
     * Puts files into specified container.
     *
     * @param container
     *         container id
     * @param targetPath
     *         path to file or directory inside specified container
     * @param sourceStream
     *         stream of files from source container
     * @param noOverwriteDirNonDir
     *         If "false" then it will be an error if unpacking the given content would cause
     *         an existing directory to be replaced with a non-directory or other resource and vice versa.
     * @throws IOException
     *         when problems occurs with docker api calls, or during file system operations
     * @apiNote this method implements 1.20 docker API and requires docker not less than 1.8 version
     * @deprecated use {@link #putResource(PutResourceParams)} instead
     */
    @Deprecated
    public void putResource(String container,
                            String targetPath,
                            InputStream sourceStream,
                            boolean noOverwriteDirNonDir) throws IOException {
       putResource(PutResourceParams.from(container, targetPath)
                                    .withSourceStream(sourceStream)
                                    .withNoOverwriteDirNonDir(noOverwriteDirNonDir));
    }

    /**
     * Puts files into specified container.
     *
     * @throws IOException
     *         when problems occurs with docker api calls, or during file system operations
     * @apiNote this method implements 1.20 docker API and requires docker not less than 1.8 version
     */
    public void putResource(final PutResourceParams params) throws IOException {
        File tarFile;
        long length;
        try (InputStream sourceData = params.sourceStream()) {
            // we save stream to file, because we have to know its length
            Path tarFilePath = Files.createTempFile("compressed-resources", ".tar");
            tarFile = tarFilePath.toFile();
            length = Files.copy(sourceData, tarFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        try (InputStream tarStream = new BufferedInputStream(new FileInputStream(tarFile));
             DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("PUT")
                                                            .path("/containers/" + params.container() + "/archive")
                                                            .query("path", params.targetPath())
                                                            .header(Pair.of("Content-Type", ExtMediaType.APPLICATION_X_TAR))
                                                            .header(Pair.of("Content-Length", length))
                                                            .entity(tarStream)) {
            addQueryParamIfSet(connection, "noOverwriteDirNonDir", params.noOverwriteDirNonDir());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (status != OK.getStatusCode()) {
                throw getDockerException(response);
            }
        } finally {
            FileCleaner.addFile(tarFile);
        }
    }

    /**
     * Get docker events.
     * Parameter {@code untilSecond} does nothing if {@code sinceSecond} is 0.<br>
     * If {@code untilSecond} and {@code sinceSecond} are 0 method gets new events only (streaming mode).<br>
     * If {@code untilSecond} and {@code sinceSecond} are not 0 (but less that current date)
     * methods get events that were generated between specified dates.<br>
     * If {@code untilSecond} is 0 but {@code sinceSecond} is not method gets old events and streams new ones.<br>
     * If {@code sinceSecond} is 0 no old events will be got.<br>
     * With some connection implementations method can fail due to connection timeout in streaming mode.
     *
     * @param sinceSecond
     *         UNIX date in seconds. allow omit events created before specified date.
     * @param untilSecond
     *         UNIX date in seconds. allow omit events created after specified date.
     * @param filters
     *         filter of needed events. Available filters: {@code event=<string>}
     *         {@code image=<string>} {@code container=<string>}
     * @param messageProcessor
     *         processor of all found events that satisfy specified parameters
     * @throws IOException
     * @deprecated use {@link #getEvents(GetEventsParams, MessageProcessor)} instead
     */
    @Deprecated
    public void getEvents(long sinceSecond,
                          long untilSecond,
                          Filters filters,
                          MessageProcessor<Event> messageProcessor) throws IOException {
        getEvents(GetEventsParams.create()
                                 .withSinceSecond(sinceSecond)
                                 .withUntilSecond(untilSecond)
                                 .withFilters(filters),
                  messageProcessor);
    }

    /**
     * Get docker events.
     * Parameter {@code untilSecond} does nothing if {@code sinceSecond} is 0.<br>
     * If {@code untilSecond} and {@code sinceSecond} are 0 method gets new events only (streaming mode).<br>
     * If {@code untilSecond} and {@code sinceSecond} are not 0 (but less that current date)
     * methods get events that were generated between specified dates.<br>
     * If {@code untilSecond} is 0 but {@code sinceSecond} is not method gets old events and streams new ones.<br>
     * If {@code sinceSecond} is 0 no old events will be got.<br>
     * With some connection implementations method can fail due to connection timeout in streaming mode.
     *
     * @param messageProcessor
     *         processor of all found events that satisfy specified parameters
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void getEvents(final GetEventsParams params, MessageProcessor<Event> messageProcessor) throws IOException {
        final Filters filters = params.filters();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("GET")
                                                            .path("/events")) {
            addQueryParamIfSet(connection, "since", params.sinceSecond());
            addQueryParamIfSet(connection, "until", params.untilSecond());
            if (filters != null) {
                connection.query("filters", urlPathSegmentEscaper().escape(JsonHelper.toJson(filters.getFilters())));
            }
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }

            try (InputStream responseStream = response.getInputStream()) {
                new MessagePumper<>(new JsonMessageReader<>(responseStream, Event.class), messageProcessor).start();
            }
        }
    }

    /**
     * Builds new image.
     *
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @return image id
     * @throws IOException
     * @throws InterruptedException
     *         if build process was interrupted
     */
    public String buildImage(BuildImageParams params, ProgressMonitor progressMonitor) throws IOException, InterruptedException {
            return doBuildImage(params, progressMonitor, dockerDaemonUri);
    }

    /**
     * The same as {@link #buildImage(BuildImageParams, ProgressMonitor)} but with docker service uri parameter
     *
     * @param dockerDaemonUri
     *         docker service URI
     */
    protected String doBuildImage(final BuildImageParams params,
                                  final ProgressMonitor progressMonitor,
                                  final URI dockerDaemonUri) throws IOException, InterruptedException {
        File[] files = (File[]) params.files().toArray();

        final File tar = Files.createTempFile(null, ".tar").toFile();
        try {
            createTarArchive(tar, files);
            AuthConfigs authConfigs = firstNonNull(params.authConfigs(), initialAuthConfig.getAuthConfigs());

            try (InputStream tarInput = new FileInputStream(tar);
                 DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                                .method("POST")
                                                                .path("/build")
                                                                .query("rm", 1)
                                                                .query("forcerm", 1)
                                                                .header(Pair.of("Content-Type", "application/x-compressed-tar"))
                                                                .header(Pair.of("Content-Length", tar.length()))
                                                                .header(Pair.of("X-Registry-Config",
                                                                                Base64.encodeBase64String(JsonHelper.toJson(authConfigs)
                                                                                                                    .getBytes())))
                                                                .entity(tarInput)) {
                addQueryParamIfSet(connection, "t", params.repository());
                addQueryParamIfSet(connection, "memory", params.memoryLimit());
                addQueryParamIfSet(connection, "memswap", params.memorySwapLimit());
                addQueryParamIfSet(connection, "pull", params.doForcePull());
                final DockerResponse response = connection.request();
                final int status = response.getStatus();
                if (OK.getStatusCode() != status) {
                    throw getDockerException(response);
                }
                try (InputStream responseStream = response.getInputStream()) {
                    JsonMessageReader<ProgressStatus> progressReader = new JsonMessageReader<>(responseStream, ProgressStatus.class);

                    final ValueHolder<IOException> errorHolder = new ValueHolder<>();
                    final ValueHolder<String> imageIdHolder = new ValueHolder<>();
                    // Here do some trick to be able interrupt build process. Basically for now it is not possible interrupt docker daemon while
                    // it's building images but here we need just be able to close connection to the unix socket. Thread is blocking while read
                    // from the socket stream so need one more thread that is able to close socket. In this way we can release thread that is
                    // blocking on i/o.
                    final Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ProgressStatus progressStatus;
                                while ((progressStatus = progressReader.next()) != null) {
                                    final String buildImageId = getBuildImageId(progressStatus);
                                    if (buildImageId != null) {
                                        imageIdHolder.set(buildImageId);
                                    }
                                    progressMonitor.updateProgress(progressStatus);
                                }
                            } catch (IOException e) {
                                errorHolder.set(e);
                            }
                            synchronized (this) {
                                notify();
                            }
                        }
                    };
                    executor.execute(runnable);
                    // noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (runnable) {
                        runnable.wait();
                    }
                    final IOException ioe = errorHolder.get();
                    if (ioe != null) {
                        throw ioe;
                    }
                    if (imageIdHolder.get() == null) {
                        throw new IOException("Docker image build failed");
                    }
                    return imageIdHolder.get();
                }
            }
        } finally {
            FileCleaner.addFile(tar);
        }
    }

    /**
     * @deprecated use {@link #removeImage(RemoveImageParams)} instead
     */
    @Deprecated
    public void removeImage(String image, boolean force) throws IOException {
        removeImage(RemoveImageParams.from(image)
                                     .withForce(force));
    }

    /**
     * Removes docker image.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void removeImage(final RemoveImageParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("DELETE")
                                                            .path("/images/" + params.image())) {
            addQueryParamIfSet(connection, "force", params.force());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
        }
    }

    /**
     * @deprecated use {@link #tag(TagParams)} nstead
     */
    @Deprecated
    public void tag(String image, String repository, String tag) throws IOException {
       tag(TagParams.from(image, repository)
                    .withTag(tag)
                    .withForce(false));
    }

    /**
     * Tag the docker image into a repository.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void tag(final TagParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/images/" + params.image() + "/tag")
                                                            .query("repo", params.repository())) {
            addQueryParamIfSet(connection, "force", params.force());
            addQueryParamIfSet(connection, "tag", params.tag());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (status / 100 != 2) {
                throw getDockerException(response);
            }
        }
    }

    /**
     * Push docker image to the registry
     *
     * @param repository
     *         full repository name to be applied to newly created image
     * @param tag
     *         tag of the image
     * @param registry
     *         registry url
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @return digest of just pushed image
     * @throws IOException
     *         when problems occurs with docker api calls
     * @throws InterruptedException
     *         if push process was interrupted
     * @deprecated use {@link #push(PushParams, ProgressMonitor)} instead
     */
    @Deprecated
    public String push(String repository,
                       String tag,
                       String registry,
                       final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        return push(PushParams.from(repository)
                              .withTag(tag)
                              .withRegistry(registry),
                    progressMonitor);
    }

    /**
     * Push docker image to the registry.
     *
     * @param progressMonitor
     *         ProgressMonitor for images pushing process
     * @return digest of just pushed image
     * @throws IOException
     *         when problems occurs with docker api calls
     * @throws InterruptedException
     *         if push process was interrupted
     */
    public String push(final PushParams params, final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        final String fullRepo = (params.registry() != null) ?
                                params.registry() + "/" + params.repository() : params.repository();
        final ValueHolder<String> digestHolder = new ValueHolder<>();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/images/" + fullRepo + "/push")
                                                            .header(Pair.of("X-Registry-Auth", initialAuthConfig.getAuthConfigHeader()))) {
            addQueryParamIfSet(connection, "tag", params.tag());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            try (InputStream responseStream = response.getInputStream()) {
                JsonMessageReader<ProgressStatus> progressReader = new JsonMessageReader<>(responseStream, ProgressStatus.class);

                final ValueHolder<IOException> errorHolder = new ValueHolder<>();
                //it is necessary to track errors during the push, this is useful in the case when docker API returns status 200 OK,
                //but in fact we have an error (e.g docker registry is not accessible but we are trying to push).
                final ValueHolder<String> exceptionHolder = new ValueHolder<>();
                // Here do some trick to be able interrupt push process. Basically for now it is not possible interrupt docker daemon while
                // it's pushing images but here we need just be able to close connection to the unix socket. Thread is blocking while read
                // from the socket stream so need one more thread that is able to close socket. In this way we can release thread that is
                // blocking on i/o.
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String digestPrefix = firstNonNull(params.tag(), "latest") + ": digest: ";
                            ProgressStatus progressStatus;
                            while ((progressStatus = progressReader.next()) != null && exceptionHolder.get() == null) {
                                progressMonitor.updateProgress(progressStatus);
                                if (progressStatus.getError() != null) {
                                    exceptionHolder.set(progressStatus.getError());
                                }
                                String status = progressStatus.getStatus();
                                // Here we find string with digest which has following format:
                                // <tag>: digest: <digest> size: <size>
                                // for example:
                                // latest: digest: sha256:9a70e6222ded459fde37c56af23887467c512628eb8e78c901f3390e49a800a0 size: 62189
                                if (status != null && status.startsWith(digestPrefix)) {
                                    String digest = status.substring(digestPrefix.length(), status.indexOf(" ", digestPrefix.length()));
                                    digestHolder.set(digest);
                                }
                            }
                        } catch (IOException e) {
                            errorHolder.set(e);
                        }
                        synchronized (this) {
                            notify();
                        }
                    }
                };
                executor.execute(runnable);
                // noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (runnable) {
                    runnable.wait();
                }
                if (exceptionHolder.get() != null) {
                    throw new DockerException(exceptionHolder.get(), 500);
                }
                if (digestHolder.get() == null) {
                    LOG.error("Docker image {}:{} was successfully pushed, but its digest wasn't obtained",
                              fullRepo,
                              firstNonNull(params.tag(), "latest"));
                    throw new DockerException("Docker image was successfully pushed, but its digest wasn't obtained", 500);
                }
                final IOException ioe = errorHolder.get();
                if (ioe != null) {
                    throw ioe;
                }
            }
        }
        return digestHolder.get();
    }

    /**
     * @deprecated use {@link #commit(CommitParams)} instead
     */
    @Deprecated
    public String commit(String container, String repository, String tag, String comment, String author) throws IOException {
        // todo: pause container
        return commit(CommitParams.from(container, repository)
                                  .withTag(tag)
                                  .withComment(comment)
                                  .withAuthor(author));
    }

    /**
     * Creates a new image from a container’s changes.
     *
     * @return id of a new image
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public String commit(final CommitParams params) throws IOException {
        // TODO: add option to pause container
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/commit")
                                                            .query("container", params.container())
                                                            .query("repo", params.repository())) {
            addQueryParamIfSet(connection, "tag", params.tag());
            addQueryParamIfSet(connection, "comment", (params.comment() == null) ? null : URLEncoder.encode(params.comment(), "UTF-8"));
            addQueryParamIfSet(connection, "author", (params.author() == null) ? null : URLEncoder.encode(params.author(), "UTF-8"));
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (CREATED.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ContainerCommitted.class).getId();
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * See <a href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#create-an-image">Docker remote API # Create an
     * image</a>.
     * To pull from private registry use registry.address:port/image as image. This is not documented.
     *
     * @throws IOException
     * @throws InterruptedException
     * @deprecated use {@link #pull(PullParams, ProgressMonitor)} instead
     */
    @Deprecated
    public void pull(String image,
                     String tag,
                     String registry,
                     final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        doPull(PullParams.from(image)
                         .withTag(tag)
                         .withRegistry(registry),
               progressMonitor,
               dockerDaemonUri);
    }

    /**
     * Pulls docker image from registry.
     *
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @throws IOException
     *         when problems occurs with docker api calls
     * @throws InterruptedException
     *         if push process was interrupted
     */
    public void pull(final PullParams params, final ProgressMonitor progressMonitor) throws IOException, InterruptedException {
        doPull(params, progressMonitor, dockerDaemonUri);
    }

    /**
     * See <a href="https://docs.docker.com/reference/api/docker_remote_api_v1.16/#create-an-image">Docker remote API # Create an
     * image</a>.
     * To pull from private registry use registry.address:port/image as image. This is not documented.
     *
     * @param progressMonitor
     *         ProgressMonitor for images creation process
     * @param dockerDaemonUri
     *         docker service URI
     * @throws IOException
     *         when problems occurs with docker api calls
     * @throws InterruptedException
     *         if push process was interrupted
     */
    protected void doPull(final PullParams params,
                          final ProgressMonitor progressMonitor,
                          final URI dockerDaemonUri) throws IOException, InterruptedException {
        final String image = params.image();
        final String registry = params.registry();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/images/create")
                                                            .query("fromImage", registry != null ? registry + "/" + image : image)
                                                            .header(Pair.of("X-Registry-Auth", initialAuthConfig.getAuthConfigHeader()))) {
            addQueryParamIfSet(connection, "tag", params.tag());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (OK.getStatusCode() != status) {
                throw getDockerException(response);
            }
            try (InputStream responseStream = response.getInputStream()) {
                JsonMessageReader<ProgressStatus> progressReader = new JsonMessageReader<>(responseStream, ProgressStatus.class);

                final ValueHolder<IOException> errorHolder = new ValueHolder<>();
                // Here do some trick to be able interrupt pull process. Basically for now it is not possible interrupt docker daemon while
                // it's pulling images but here we need just be able to close connection to the unix socket. Thread is blocking while read
                // from the socket stream so need one more thread that is able to close socket. In this way we can release thread that is
                // blocking on i/o.
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressStatus progressStatus;
                            while ((progressStatus = progressReader.next()) != null) {
                                progressMonitor.updateProgress(progressStatus);
                            }
                        } catch (IOException e) {
                            errorHolder.set(e);
                        }
                        synchronized (this) {
                            notify();
                        }
                    }
                };
                executor.execute(runnable);
                // noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (runnable) {
                    runnable.wait();
                }
                final IOException ioe = errorHolder.get();
                if (ioe != null) {
                    throw ioe;
                }
            }
        }
    }

    /**
     * @deprecated use {@link #createContainer(CreateContainerParams)} instead
     */
    @Deprecated
    public ContainerCreated createContainer(ContainerConfig containerConfig, String containerName) throws IOException {
        return doCreateContainer(CreateContainerParams.from(containerConfig)
                                                      .withContainerName(containerName),
                                 dockerDaemonUri);
    }

    /**
     * Creates docker container.
     *
     * @return information about just created container
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public ContainerCreated createContainer(final CreateContainerParams params) throws IOException {
        return doCreateContainer(params, dockerDaemonUri);
    }

    /**
     * The same as {@link #createContainer(CreateContainerParams)} but with docker service uri parameter
     *
     * @param dockerDaemonUri
     *         docker service URI
     */
    protected ContainerCreated doCreateContainer(final CreateContainerParams params, final URI dockerDaemonUri) throws IOException {
        final String entity = JsonHelper.toJson(params.containerConfig(), FIRST_LETTER_LOWERCASE);
        byte[] entityBytesArray = entity.getBytes();

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/create")
                                                            .header(Pair.of("Content-Type", MediaType.APPLICATION_JSON))
                                                            .header(Pair.of("Content-Length", entityBytesArray.length))
                                                            .entity(entityBytesArray)) {
            addQueryParamIfSet(connection, "name", params.containerName());
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (CREATED.getStatusCode() != status) {
                throw getDockerException(response);
            }
            return parseResponseStreamAndClose(response.getInputStream(), ContainerCreated.class);
        } catch (JsonParseException e) {
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @deprecated use {@link #stopContainer(StopContainerParams)} instead
     */
    @Deprecated
    public void startContainer(String container, HostConfig hostConfig) throws IOException {
        final List<Pair<String, ?>> headers = new ArrayList<>(2);
        headers.add(Pair.of("Content-Type", MediaType.APPLICATION_JSON));
        final String entity = (hostConfig == null) ? "{}" : JsonHelper.toJson(hostConfig, FIRST_LETTER_LOWERCASE);
        byte[] entityBytesArray = entity.getBytes();
        headers.add(Pair.of("Content-Length", entityBytesArray.length));

        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + container + "/start")
                                                            .headers(headers)
                                                            .entity(entityBytesArray)) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (!(NO_CONTENT.getStatusCode() == status || NOT_MODIFIED.getStatusCode() == status)) {

                final DockerException dockerException = getDockerException(response);
                if (OK.getStatusCode() == status) {
                    // docker API 1.20 returns 200 with warning message about usage of loopback docker backend
                    LOG.warn(dockerException.getLocalizedMessage());
                } else {
                    throw dockerException;
                }
            }
        }
    }

    /**
     * Starts docker container.
     *
     * @throws IOException
     *         when problems occurs with docker api calls
     */
    public void startContainer(final StartContainerParams params) throws IOException {
        try (DockerConnection connection = connectionFactory.openConnection(dockerDaemonUri)
                                                            .method("POST")
                                                            .path("/containers/" + params.container() + "/start")) {
            final DockerResponse response = connection.request();
            final int status = response.getStatus();
            if (!(NO_CONTENT.getStatusCode() == status || NOT_MODIFIED.getStatusCode() == status)) {

                final DockerException dockerException = getDockerException(response);
                if (OK.getStatusCode() == status) {
                    // docker API 1.20 returns 200 with warning message about usage of loopback docker backend
                    LOG.warn(dockerException.getLocalizedMessage());
                } else {
                    throw dockerException;
                }
            }
        }
    }

    private String getBuildImageId(ProgressStatus progressStatus) {
        final String stream = progressStatus.getStream();
        if (stream != null && stream.startsWith("Successfully built ")) {
            int endSize = 19;
            while (endSize < stream.length() && Character.digit(stream.charAt(endSize), 16) != -1) {
                endSize++;
            }
            return stream.substring(19, endSize);
        }
        return null;
    }

    private <T> T parseResponseStreamAndClose(InputStream inputStream, Class<T> clazz) throws IOException, JsonParseException {
        try (InputStream responseStream = inputStream) {
            return JsonHelper.fromJson(responseStream,
                                       clazz,
                                       null,
                                       FIRST_LETTER_LOWERCASE);
        }
    }

    protected DockerException getDockerException(DockerResponse response) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(response.getInputStream())) {
            String dockerResponseContent = CharStreams.toString(isr);
            return new DockerException(
                    "Error response from docker API, status: " + response.getStatus() + ", message: " + dockerResponseContent,
                    dockerResponseContent,
                    response.getStatus());
        }
    }

    // Unfortunately we can't use generated DTO here.
    // Docker uses uppercase in first letter in names of json objects, e.g. {"Id":"123"} instead of {"id":"123"}
    protected static JsonNameConvention FIRST_LETTER_LOWERCASE = new JsonNameConvention() {
        @Override
        public String toJsonName(String javaName) {
            return Character.toUpperCase(javaName.charAt(0)) + javaName.substring(1);
        }

        @Override
        public String toJavaName(String jsonName) {
            return Character.toLowerCase(jsonName.charAt(0)) + jsonName.substring(1);
        }
    };

    private void createTarArchive(File tar, File... files) throws IOException {
        TarUtils.tarFiles(tar, 0, files);
    }

    /**
     * Adds given parameter to query if it set (not null).
     *
     * @param connection
     *         connection to docker service
     * @param queryParamName
     *         name of query parameter
     * @param paramValue
     *         value of query parameter
     */
    private void addQueryParamIfSet(DockerConnection connection, String queryParamName, Object paramValue) {
        if (paramValue != null && queryParamName != null && !queryParamName.equals("")) {
            connection.query(queryParamName, paramValue);
        }
    }

    /**
     * The same as {@link #addQueryParamIfSet(DockerConnection, String, Object)}, but
     * in case of {@code paramValue} is {@code true} '1' will be added as parameter value, in case of {@code false} '0'.
     */
    private void addQueryParamIfSet(DockerConnection connection, String queryParamName, Boolean paramValue) {
        if (paramValue != null && queryParamName != null && !queryParamName.equals("")) {
            connection.query(queryParamName, paramValue ? 1 : 0);
        }
    }

}
