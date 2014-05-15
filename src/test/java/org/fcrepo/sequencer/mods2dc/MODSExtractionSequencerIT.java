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

import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.graph.NodeFactory.createURI;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.tika.io.IOUtils;
import org.fcrepo.integration.http.api.AbstractResourceIT;
import org.junit.Test;

import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

/**
 * @author bbpennel
 */
public class MODSExtractionSequencerIT extends AbstractResourceIT {

    @Test
    public void testTitleAssigned() throws Exception {

        HttpPost method = postObjMethod("");
        final HttpResponse response = client.execute(method);
        final String parentNodeLocation = getLocation(response);
        final String parentPid = parentNodeLocation.substring(parentNodeLocation.lastIndexOf('/') + 1);

        final String modsContent = IOUtils.toString(this.getClass().getResourceAsStream("/mods.xml"), "UTF-8");

        method = postDSMethod(parentPid, "mods", modsContent);
        client.execute(method);

        // Wait for sequencer to run
        Thread.sleep(3000);

        final GraphStore triples = getGraphStore(client, new HttpGet(serverAddress + parentPid));

        final Iterator<Quad> quads = triples.find(ANY, createURI(parentNodeLocation), ANY, createLiteral("Fedora 4"));
        triples.find(ANY, ANY, createURI("dc:title"), ANY).hasNext();

        final Quad quad = quads.next();
        final String assignedTitle = (String) quad.getObject().getLiteral().getValue();

        assertEquals("dc:title was not correctly assigned to the parent object", "Fedora 4", assignedTitle);

    }

    private String getLocation(final HttpResponse response) {
        final Header[] headers = response.getHeaders("Location");
        for (final Header header : headers) {
            if ("Location".equals(header.getName())) {
                return header.getValue();
            }
        }

        return null;
    }
}