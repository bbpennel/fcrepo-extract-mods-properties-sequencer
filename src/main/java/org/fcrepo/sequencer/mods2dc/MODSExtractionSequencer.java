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

import static org.fcrepo.kernel.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;

import java.io.IOException;
import java.util.Iterator;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Simple sequencer which finds the title in a MODS document binary datastream
 * and stores it as a dc:title property on
 * 
 * @author bbpennel
 */
public class MODSExtractionSequencer extends Sequencer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MODSExtractionSequencer.class);

    private final static String MODS_NS = "http://www.loc.gov/mods/v3";

    private XPathExpression titleXPath;

    /**
     * Extracts the title from the MODS datastream and assigns it as a dc:title
     * on the Fedora object which is the parent of the datastream.
     */
    @Override
    public boolean execute(final Property inputProperty, final Node outputNode, final Context context)
        throws Exception {

        if (!JCR_DATA.equals(inputProperty.getName()) || !isFedoraDatastream.apply(outputNode)) {
            return false;
        }

        LOGGER.debug("Executing MODS to DC sequencer");

        // Parse the policy content
        final Document modsDoc = binaryPropertyToDocument(inputProperty);

        final NodeList titleNodes = (NodeList) titleXPath.evaluate(modsDoc, XPathConstants.NODESET);

        if (titleNodes.getLength() == 0) {
            return false;
        }

        // Use the first title
        final String title = titleNodes.item(0).getTextContent();

        outputNode.getParent().setProperty("dc:title", title);

        LOGGER.debug("Setting dc:title to {}", title);

        return true;
    }

    private Document binaryPropertyToDocument(final Property inputProperty) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);

        DocumentBuilder db = null;
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        db = factory.newDocumentBuilder();

        final Binary inputBinary = inputProperty.getBinary();
        return db.parse(inputBinary.getStream());
    }

    @Override
    public void initialize(final NamespaceRegistry registry, final NodeTypeManager nodeTypeManager)
        throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        final NamespaceContext ctx = new NamespaceContext() {

            public String getNamespaceURI(final String prefix) {
                return MODS_NS;
            }

            public Iterator<?> getPrefixes(final String val) {
                return null;
            }

            public String getPrefix(final String uri) {
                return null;
            }
        };

        final XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(ctx);
        try {
            titleXPath = xpath.compile("/mods:mods/mods:titleInfo/mods:title/text()");
        } catch (final XPathExpressionException e) {
            LOGGER.error("Failed to compile xpath", e);
        }
    }
}
