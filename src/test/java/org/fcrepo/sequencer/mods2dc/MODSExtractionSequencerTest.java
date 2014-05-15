/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.sequencer.mods2dc;

import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.jcr.FedoraJcrTypes.FEDORA_RESOURCE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.NodeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;

/**
 * @author bbpennel
 */
public class MODSExtractionSequencerTest {

    @Mock
    private Property mockInputProperty;

    @Mock
    private Binary mockPropertyBinary;

    @Mock
    private Node mockDatastreamNode;

    @Mock
    private Node mockObjectNode;

    @Mock
    private Context mockContext;

    @Mock
    private NamespaceRegistry mockRegistry;

    @Mock
    NodeTypeManager mockNodeTypeManager;

    @Mock
    NodeType mockDatastreamType;

    private MODSExtractionSequencer sequencer;

    @Before
    public void init() throws Exception {
        initMocks(this);

        sequencer = new MODSExtractionSequencer();
        sequencer.initialize(mockRegistry, mockNodeTypeManager);

        when(mockDatastreamNode.getParent()).thenReturn(mockObjectNode);
        when(mockDatastreamType.getName()).thenReturn(FEDORA_DATASTREAM);
        when(mockDatastreamNode.getMixinNodeTypes()).thenReturn(new NodeType[] {mockDatastreamType});
        when(mockInputProperty.getName()).thenReturn(JCR_DATA);
    }

    @Test
    public void testWithTitle() throws Exception {
        when(mockInputProperty.getBinary()).thenReturn(mockPropertyBinary);
        when(mockPropertyBinary.getStream()).thenReturn(this.getClass().getResourceAsStream("/mods.xml"));

        final boolean result = sequencer.execute(mockInputProperty, mockDatastreamNode, mockContext);

        verify(mockObjectNode).setProperty(eq("dc:title"), eq("Fedora 4"));
        assertTrue("Sequencer should indicate that the output should be saved", result);
    }

    @Test
    public void testWithoutTitle() throws Exception {
        when(mockInputProperty.getBinary()).thenReturn(mockPropertyBinary);
        when(mockPropertyBinary.getStream()).thenReturn(this.getClass().getResourceAsStream("/noTitle.xml"));

        final boolean result = sequencer.execute(mockInputProperty, mockDatastreamNode, mockContext);

        verify(mockObjectNode, never()).setProperty(anyString(), anyString());
        assertFalse("Sequencer should indicate that the output should not be saved", result);
    }

    @Test
    public void testNotJcrDataInput() throws Exception {
        when(mockInputProperty.getName()).thenReturn(JCR_CONTENT);

        final boolean result = sequencer.execute(mockInputProperty, mockDatastreamNode, mockContext);

        verify(mockObjectNode, never()).setProperty(anyString(), anyString());
        verify(mockInputProperty, never()).getBinary();
        assertFalse("Sequencer should indicate that the output should not be saved", result);
    }

    @Test
    public void testNotDatastreamOutput() throws Exception {
        when(mockDatastreamType.getName()).thenReturn(FEDORA_RESOURCE);

        final boolean result = sequencer.execute(mockInputProperty, mockDatastreamNode, mockContext);

        verify(mockObjectNode, never()).setProperty(anyString(), anyString());
        verify(mockInputProperty, never()).getBinary();
        assertFalse("Sequencer should indicate that the output should not be saved", result);
    }
}
