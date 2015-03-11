/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Function;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.binding.util.RpcServiceInvoker;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class BindingRpcImplementationAdapter implements DOMRpcImplementation {

    private static final Function<? super Exception, DOMRpcException> EXCEPTION_MAPPER = new Function<Exception, DOMRpcException>() {

        @Override
        public DOMRpcException apply(Exception input) {
            // FIXME: Return correct exception
            return null;
        }

    };
    private final BindingNormalizedNodeCodecRegistry codec;
    private final RpcServiceInvoker invoker;
    private final RpcService delegate;
    private final QNameModule module;

    private Function<RpcResult<?>,DOMRpcResult> lazySerializedMapper = new Function<RpcResult<?>,DOMRpcResult>() {

        @Override
        public DOMRpcResult apply(final RpcResult<?> input) {
            return LazySerializedDOMRpcResult.create(input, codec);
        }
    };

    public <T extends RpcService> BindingRpcImplementationAdapter(BindingNormalizedNodeCodecRegistry codec, Class<T> type ,T delegate) {
        this.codec = codec;
        this.delegate = delegate;
        this.invoker = RpcServiceInvoker.from(type);
        this.module = BindingReflections.getQNameModule(type);
    }

    public QNameModule getQNameModule() {
        return module;
    }

    @Override
    public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {
        SchemaPath schemaPath = rpc.getType();
        DataObject bindingInput = deserilialize(rpc.getType(),input);
        ListenableFuture<RpcResult<?>> bindingResult = invoke(schemaPath,bindingInput);
        return transformResult(schemaPath,bindingResult);
    }

    private DataObject deserilialize(SchemaPath rpcPath, NormalizedNode<?, ?> input) {
        if(input instanceof LazySerializedContainerNode) {
            return ((LazySerializedContainerNode) input).bindingData();
        }
        SchemaPath inputSchemaPath = rpcPath.createChild(QName.create(module,"input"));
        return codec.fromNormalizedNodeRpcData(inputSchemaPath, (ContainerNode) input);
    }


    private ListenableFuture<RpcResult<?>> invoke(SchemaPath schemaPath, DataObject input) {
        return JdkFutureAdapters.listenInPoolThread(invoker.invokeRpc(delegate, schemaPath.getLastComponent(), input));
    }

    private CheckedFuture<DOMRpcResult, DOMRpcException> transformResult(SchemaPath schemaPath,
            ListenableFuture<RpcResult<?>> bindingResult) {
        ListenableFuture<DOMRpcResult> transformed = Futures.transform(bindingResult, lazySerializedMapper);
        return Futures.makeChecked(transformed, EXCEPTION_MAPPER);
    }

}
