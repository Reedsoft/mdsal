/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yangtools.binding.data.codec.test;

import static org.junit.Assert.assertEquals;

import javassist.ClassPool;
import org.junit.Test;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.md.sal.test.top.via.uses.rev151112.container.top.ContainerTop;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;


public class TopLevelContainerViaUsesTest extends AbstractBindingRuntimeTest {

    private static final InstanceIdentifier<ContainerTop> TOP_LEVEL_CONTAINER_FROM_USES =
            InstanceIdentifier.create(ContainerTop.class);

    private BindingNormalizedNodeCodecRegistry registry;

    @Override
    public void setup() {
        super.setup();
        final JavassistUtils utils = JavassistUtils.forClassPool(ClassPool.getDefault());
        registry = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(utils));
        registry.onBindingRuntimeContextUpdated(getRuntimeContext());
    }

    @Test
    public void testBindingToDomFirst() {
        final YangInstanceIdentifier yangII = registry.toYangInstanceIdentifier(TOP_LEVEL_CONTAINER_FROM_USES);
        final PathArgument lastArg = yangII.getLastPathArgument();
        assertEquals(ContainerTop.QNAME, lastArg.getNodeType());
    }


    @Test
    public void testDomToBindingFirst() {
        final YangInstanceIdentifier yangII = YangInstanceIdentifier.of(ContainerTop.QNAME);
        InstanceIdentifier<?> bindingII = registry.fromYangInstanceIdentifier(yangII);
        assertEquals(TOP_LEVEL_CONTAINER_FROM_USES, bindingII);
    }

}
