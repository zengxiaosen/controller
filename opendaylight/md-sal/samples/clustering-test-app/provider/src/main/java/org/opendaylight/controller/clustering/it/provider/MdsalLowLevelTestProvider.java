/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.sharding.DistributedShardFactory;
import org.opendaylight.controller.clustering.it.provider.impl.FlappingSingletonService;
import org.opendaylight.controller.clustering.it.provider.impl.GetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.IdIntsDOMDataTreeLIstener;
import org.opendaylight.controller.clustering.it.provider.impl.IdIntsListener;
import org.opendaylight.controller.clustering.it.provider.impl.PrefixLeaderHandler;
import org.opendaylight.controller.clustering.it.provider.impl.PrefixShardHandler;
import org.opendaylight.controller.clustering.it.provider.impl.ProduceTransactionsHandler;
import org.opendaylight.controller.clustering.it.provider.impl.PublishNotificationsTask;
import org.opendaylight.controller.clustering.it.provider.impl.RoutedGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.SingletonGetConstantService;
import org.opendaylight.controller.clustering.it.provider.impl.WriteTransactionsHandler;
import org.opendaylight.controller.clustering.it.provider.impl.YnlListener;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListeningException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.AddShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CheckPublishNotificationsOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.CreatePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.IsClientAbortedOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.OdlMdsalLowlevelControlService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.ProduceTransactionsOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterDefaultConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RegisterSingletonConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemovePrefixShardInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.RemoveShardReplicaInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.StartPublishNotificationsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.SubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterBoundConstantInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnregisterFlappingSingletonOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDdtlOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeDtclOutputBuilder;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.UnsubscribeYnlOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.WriteTransactionsOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalLowLevelTestProvider implements OdlMdsalLowlevelControlService {

    private static final Logger LOG = LoggerFactory.getLogger(MdsalLowLevelTestProvider.class);
    private static final org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType CONTROLLER_CONFIG =
            org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

    private final RpcProviderRegistry rpcRegistry;
    private final BindingAwareBroker.RpcRegistration<OdlMdsalLowlevelControlService> registration;
    private final DistributedShardFactory distributedShardFactory;
    private final DOMDataTreeService domDataTreeService;
    private final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer;
    private final DOMDataBroker domDataBroker;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final SchemaService schemaService;
    private final ClusterSingletonServiceProvider singletonService;
    private final DOMRpcProviderService domRpcService;
    private final PrefixLeaderHandler prefixLeaderHandler;
    private final PrefixShardHandler prefixShardHandler;
    private final DOMDataTreeChangeService domDataTreeChangeService;

    private Map<InstanceIdentifier<?>, DOMRpcImplementationRegistration<RoutedGetConstantService>> routedRegistrations =
            new HashMap<>();

    private Map<String, ListenerRegistration<YnlListener>> ynlRegistrations = new HashMap<>();

    private DOMRpcImplementationRegistration<GetConstantService> globalGetConstantRegistration = null;
    private ClusterSingletonServiceRegistration getSingletonConstantRegistration;
    private FlappingSingletonService flappingSingletonService;
    private ListenerRegistration<DOMDataTreeChangeListener> dtclReg;
    private IdIntsListener idIntsListener;
    private Map<String, PublishNotificationsTask> publishNotificationsTasks = new HashMap<>();
    private ListenerRegistration<IdIntsDOMDataTreeLIstener> ddtlReg;
    private IdIntsDOMDataTreeLIstener idIntsDdtl;



    public MdsalLowLevelTestProvider(final RpcProviderRegistry rpcRegistry,
                                     final DOMRpcProviderService domRpcService,
                                     final ClusterSingletonServiceProvider singletonService,
                                     final SchemaService schemaService,
                                     final BindingNormalizedNodeSerializer bindingNormalizedNodeSerializer,
                                     final NotificationPublishService notificationPublishService,
                                     final NotificationService notificationService,
                                     final DOMDataBroker domDataBroker,
                                     final DOMDataTreeService domDataTreeService,
                                     final DistributedShardFactory distributedShardFactory) {
        this.rpcRegistry = rpcRegistry;
        this.domRpcService = domRpcService;
        this.singletonService = singletonService;
        this.schemaService = schemaService;
        this.bindingNormalizedNodeSerializer = bindingNormalizedNodeSerializer;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.domDataBroker = domDataBroker;
        this.domDataTreeService = domDataTreeService;
        this.distributedShardFactory = distributedShardFactory;
        this.prefixLeaderHandler = new PrefixLeaderHandler(domDataTreeService, bindingNormalizedNodeSerializer);

        domDataTreeChangeService =
                (DOMDataTreeChangeService) domDataBroker.getSupportedExtensions().get(DOMDataTreeChangeService.class);

        registration = rpcRegistry.addRpcImplementation(OdlMdsalLowlevelControlService.class, this);

        prefixShardHandler = new PrefixShardHandler(distributedShardFactory, bindingNormalizedNodeSerializer);
    }

    @Override
    public Future<RpcResult<Void>> unregisterSingletonConstant() {
        LOG.debug("unregister-singleton-constant");

        if (getSingletonConstantRegistration == null) {
            LOG.debug("No get-singleton-constant registration present.");
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No get-singleton-constant rpc registration present.");
            final RpcResult<Void> result = RpcResultBuilder.<Void>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        try {
            getSingletonConstantRegistration.close();
            getSingletonConstantRegistration = null;

            return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
        } catch (final Exception e) {
            LOG.debug("There was a problem closing the singleton constant service", e);
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "error-closing", "There was a problem closing get-singleton-constant");
            final RpcResult<Void> result = RpcResultBuilder.<Void>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }
    }

    @Override
    public Future<RpcResult<Void>> startPublishNotifications(final StartPublishNotificationsInput input) {
        LOG.debug("publish-notifications, input: {}", input);

        final PublishNotificationsTask task = new PublishNotificationsTask(notificationPublishService, input.getId(),
                input.getSeconds(), input.getNotificationsPerSecond());

        publishNotificationsTasks.put(input.getId(), task);

        task.start();

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> subscribeDtcl() {

        if (dtclReg != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already dataTreeChangeListener registered on id-ints list.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        idIntsListener = new IdIntsListener();

        dtclReg = domDataTreeChangeService
                .registerDataTreeChangeListener(
                        new org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier(
                                CONTROLLER_CONFIG, WriteTransactionsHandler.ID_INTS_YID),
                        idIntsListener);

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<WriteTransactionsOutput>> writeTransactions(final WriteTransactionsInput input) {
        LOG.debug("write-transactions, input: {}", input);

        final WriteTransactionsHandler writeTransactionsHandler = new WriteTransactionsHandler(domDataBroker, input);

        final SettableFuture<RpcResult<WriteTransactionsOutput>> settableFuture = SettableFuture.create();
        writeTransactionsHandler.start(settableFuture);

        return settableFuture;
    }

    @Override
    public Future<RpcResult<IsClientAbortedOutput>> isClientAborted() {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> removeShardReplica(RemoveShardReplicaInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> subscribeYnl(final SubscribeYnlInput input) {

        LOG.debug("subscribe-ynl, input: {}", input);

        if (ynlRegistrations.containsKey(input.getId())) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already ynl listener registered for this id: " + input.getId());
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        ynlRegistrations.put(input.getId(),
                notificationService.registerNotificationListener(new YnlListener(input.getId())));

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> removePrefixShard(final RemovePrefixShardInput input) {
        LOG.debug("remove-prefix-shard, input: {}", input);

        return prefixShardHandler.onRemovePrefixShard(input);
    }

    @Override
    public Future<RpcResult<Void>> becomePrefixLeader(final BecomePrefixLeaderInput input) {
        LOG.debug("become-prefix-leader, input: {}", input);

        return prefixLeaderHandler.makeLeaderLocal(input);
    }

    @Override
    public Future<RpcResult<Void>> unregisterBoundConstant(final UnregisterBoundConstantInput input) {
        LOG.debug("unregister-bound-constant, {}", input);

        final DOMRpcImplementationRegistration<RoutedGetConstantService> registration =
                routedRegistrations.remove(input.getContext());

        if (registration == null) {
            LOG.debug("No get-contexted-constant registration for context: {}", input.getContext());
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No get-constant rpc registration present.");
            final RpcResult<Void> result = RpcResultBuilder.<Void>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        registration.close();
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> registerSingletonConstant(final RegisterSingletonConstantInput input) {

        LOG.debug("Received register-singleton-constant rpc, input: {}", input);

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        getSingletonConstantRegistration =
                SingletonGetConstantService.registerNew(singletonService, domRpcService, input.getConstant());

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> registerDefaultConstant(RegisterDefaultConstantInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> unregisterConstant() {

        if (globalGetConstantRegistration == null) {
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No get-constant rpc registration present.");
            final RpcResult<Void> result = RpcResultBuilder.<Void>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        globalGetConstantRegistration.close();
        globalGetConstantRegistration = null;

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UnregisterFlappingSingletonOutput>> unregisterFlappingSingleton() {
        LOG.debug("unregister-flapping-singleton received.");

        if (flappingSingletonService == null) {
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No flapping-singleton registration present.");
            final RpcResult<UnregisterFlappingSingletonOutput> result =
                    RpcResultBuilder.<UnregisterFlappingSingletonOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        final long flapCount = flappingSingletonService.setInactive();
        flappingSingletonService = null;

        final UnregisterFlappingSingletonOutput output =
                new UnregisterFlappingSingletonOutputBuilder().setFlapCount(flapCount).build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<Void>> addShardReplica(AddShardReplicaInput input) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> subscribeDdtl() {

        if (ddtlReg != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already dataTreeChangeListener registered on id-ints list.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        idIntsDdtl = new IdIntsDOMDataTreeLIstener();

        try {
            ddtlReg =
                    domDataTreeService.registerListener(idIntsDdtl,
                            Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                                    ProduceTransactionsHandler.ID_INTS_YID))
                            , true, Collections.emptyList());
        } catch (DOMDataTreeLoopException e) {
            LOG.error("Failed to register DOMDataTreeListener.", e);

        }

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> registerBoundConstant(final RegisterBoundConstantInput input) {
        LOG.debug("register-bound-constant: {}", input);

        if (input.getContext() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Context value is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        if (routedRegistrations.containsKey(input.getContext())) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already a rpc registered for context: " + input.getContext());
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        final DOMRpcImplementationRegistration<RoutedGetConstantService> registration =
                RoutedGetConstantService.registerNew(bindingNormalizedNodeSerializer, domRpcService,
                        input.getConstant(), input.getContext());

        routedRegistrations.put(input.getContext(), registration);
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> registerFlappingSingleton() {
        LOG.debug("Received register-flapping-singleton.");

        if (flappingSingletonService != null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Registration present.", "flappin-singleton already registered");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        flappingSingletonService = new FlappingSingletonService(singletonService);

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<UnsubscribeDtclOutput>> unsubscribeDtcl() {
        LOG.debug("Received unsubscribe-dtcl");

        if (idIntsListener == null || dtclReg == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Dtcl missing.", "No DataTreeChangeListener registered.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed().withRpcError(error).build());
        }

        final DOMDataReadOnlyTransaction rTx = domDataBroker.newReadOnlyTransaction();
        try {
            if (dtclReg != null) {
                dtclReg.close();
                dtclReg = null;
            }

            final Optional<NormalizedNode<?, ?>> readResult =
                    rTx.read(CONTROLLER_CONFIG, WriteTransactionsHandler.ID_INTS_YID).checkedGet();

            if (!readResult.isPresent()) {
                final RpcError error = RpcResultBuilder.newError(
                        ErrorType.APPLICATION, "Final read empty.", "No data read from id-ints list.");
                return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                        .withRpcError(error).build());
            }

            return Futures.immediateFuture(
                    RpcResultBuilder.success(new UnsubscribeDtclOutputBuilder()
                            .setCopyMatches(idIntsListener.checkEqual(readResult.get()))).build());

        } catch (final ReadFailedException e) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "Read failed.", "Final read from id-ints failed.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDtclOutput>failed()
                    .withRpcError(error).build());

        }
    }

    @Override
    public Future<RpcResult<Void>> createPrefixShard(final CreatePrefixShardInput input) {
        LOG.debug("create-prefix-shard, input: {}", input);

        return prefixShardHandler.onCreatePrefixShard(input);
    }

    @Override
    public Future<RpcResult<Void>> deconfigureIdIntsShard() {
        return null;
    }

    @Override
    public Future<RpcResult<UnsubscribeYnlOutput>> unsubscribeYnl(final UnsubscribeYnlInput input) {
        LOG.debug("Received unsubscribe-ynl, input: {}", input);

        if (!ynlRegistrations.containsKey(input.getId())) {
            final RpcError rpcError = RpcResultBuilder
                    .newError(ErrorType.APPLICATION, "missing-registration", "No ynl listener with this id registered.");
            final RpcResult<UnsubscribeYnlOutput> result =
                    RpcResultBuilder.<UnsubscribeYnlOutput>failed().withRpcError(rpcError).build();
            return Futures.immediateFuture(result);
        }

        final ListenerRegistration<YnlListener> registration = ynlRegistrations.remove(input.getId());
        final UnsubscribeYnlOutput output = registration.getInstance().getOutput();

        registration.close();

        return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeYnlOutput>success().withResult(output).build());
    }

    @Override
    public Future<RpcResult<CheckPublishNotificationsOutput>> checkPublishNotifications(
            final CheckPublishNotificationsInput input) {

        final PublishNotificationsTask task = publishNotificationsTasks.get(input.getId());

        if (task == null) {
            return Futures.immediateFuture(RpcResultBuilder.success(
                    new CheckPublishNotificationsOutputBuilder().setActive(false)).build());
        }

        final CheckPublishNotificationsOutputBuilder checkPublishNotificationsOutputBuilder =
                new CheckPublishNotificationsOutputBuilder().setActive(!task.isFinished());

        if (task.getLastError() != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            task.getLastError().printStackTrace(pw);
            checkPublishNotificationsOutputBuilder.setLastError(task.getLastError().toString() + sw.toString());
        }

        final CheckPublishNotificationsOutput output =
                checkPublishNotificationsOutputBuilder.setPublishCount(task.getCurrentNotif()).build();

        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<ProduceTransactionsOutput>> produceTransactions(final ProduceTransactionsInput input) {
        LOG.debug("producer-transactions, input: {}", input);

        final ProduceTransactionsHandler handler =
                new ProduceTransactionsHandler(domDataTreeService, input);

        final SettableFuture<RpcResult<ProduceTransactionsOutput>> settableFuture = SettableFuture.create();
        handler.start(settableFuture);

        return settableFuture;
    }

    @Override
    public Future<RpcResult<Void>> registerConstant(final RegisterConstantInput input) {

        LOG.debug("Received register-constant rpc, input: {}", input);

        if (input.getConstant() == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Invalid input.", "Constant value is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        if (globalGetConstantRegistration != null) {
            final RpcError error = RpcResultBuilder.newError(ErrorType.RPC, "Registration present.",
                    "There is already a get-constant rpc registered.");
            return Futures.immediateFuture(RpcResultBuilder.<Void>failed().withRpcError(error).build());
        }

        globalGetConstantRegistration = GetConstantService.registerNew(domRpcService, input.getConstant());
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<Void>> unregisterDefaultConstant() {
        return null;
    }

    @Override
    public Future<RpcResult<UnsubscribeDdtlOutput>> unsubscribeDdtl() {
        LOG.debug("Received unsubscribe-ddtl.");

        if (idIntsDdtl == null || ddtlReg == null) {
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.RPC, "Ddtl missing.", "No DOMDataTreeListener registered.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed().withRpcError(error).build());
        }

        ddtlReg.close();
        ddtlReg = null;

        final ReadListener readListener = new ReadListener();
        try {
            final ListenerRegistration<ReadListener> registration = domDataTreeService.registerListener(readListener,
                    Collections.singleton(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                            ProduceTransactionsHandler.ID_INTS_YID))
                    , true, Collections.emptyList());

            final DataTreeCandidate dataTreeCandidate = readListener.getFirstNotif().get();
            registration.close();

            if (!dataTreeCandidate.getRootNode().getDataAfter().isPresent()) {
                final RpcError error = RpcResultBuilder.newError(
                        ErrorType.APPLICATION, "Final read empty.", "No data read from id-ints list.");
                return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                        .withRpcError(error).build());
            }

            final NormalizedNode<?, ?> lastRead = dataTreeCandidate.getRootNode().getDataAfter().get();

            return Futures.immediateFuture(
                    RpcResultBuilder.success(new UnsubscribeDdtlOutputBuilder()
                            .setCopyMatches(idIntsDdtl.checkEqual(lastRead))).build());


        } catch (final DOMDataTreeLoopException | InterruptedException | ExecutionException e) {
            LOG.error("Unable to read data to verify ddtl data.", e);
            final RpcError error = RpcResultBuilder.newError(
                    ErrorType.APPLICATION, "Read failed.", "Final read from id-ints failed.");
            return Futures.immediateFuture(RpcResultBuilder.<UnsubscribeDdtlOutput>failed()
                    .withRpcError(error).build());
        }
    }

    private static class ReadListener implements DOMDataTreeListener {

        private Collection<DataTreeCandidate> changes = null;
        private SettableFuture<DataTreeCandidate> readFuture;

        @Override
        public synchronized void onDataTreeChanged(@Nonnull final Collection<DataTreeCandidate> changes,
                                      @Nonnull final Map<DOMDataTreeIdentifier, NormalizedNode<?, ?>> subtrees) {
            Preconditions.checkArgument(changes.size() == 1);

            if (this.changes == null) {
                this.changes = changes;

                readFuture.set(changes.iterator().next());
            }
        }

        @Override
        public void onDataTreeFailed(@Nonnull final Collection<DOMDataTreeListeningException> causes) {
            LOG.error("Read Listener failed. {}", causes);
        }

        public synchronized ListenableFuture<DataTreeCandidate> getFirstNotif() {
            if (changes != null) {
                return Futures.immediateFuture(changes.iterator().next());
            }

            readFuture = SettableFuture.create();
            return readFuture;
        }
    }
}
