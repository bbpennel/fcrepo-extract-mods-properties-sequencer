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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.fcrepo.kernel.Datastream;
import org.fcrepo.kernel.FedoraResource;
import org.fcrepo.kernel.services.DatastreamService;
import org.fcrepo.kernel.services.NodeService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration test with event listener. Not currently working
 * 
 * @author bbpennel
 */
@ContextConfiguration({"/spring-test/repo.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class MODSExtractionSequencerIT2 {

    @Inject
    private NodeService nodeService;

    @Inject
    private DatastreamService dsService;

    @Autowired
    private Repository repository;

    private ObservationManager observationManager;

    private Session session;

    // @Override
    // protected InputStream getRepositoryConfigStream() {
    // return resourceStream("mods-repo-config.json");
    // }
    //
    // @Override
    // public void afterEach() throws Exception {
    // if (observationManager == null) {
    // return;
    // }
    // super.afterEach();
    // }
    //
    // @Override
    // protected void addSequencingListeners( final JcrSession session ) throws
    // RepositoryException {
    //
    // }

    @Before
    public void setup() throws Exception {
        session = repository.login();
    }

    // @Test
    public void titleSetTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final String parentPath = "/" + UUID.randomUUID().toString();
        final SequencingListener listener = addSequenceListener(latch, parentPath);
        // addSequencingListeners(session);

        final Node rootNode = session.getRootNode();
        final Node parentNode = rootNode.addNode(UUID.randomUUID().toString());

        session.save();
        // final FedoraResource parent = new FedoraResourceImpl(parentNode);

        final FedoraResource parent = nodeService.findOrCreateObject(session, parentPath);

        session.save();

        final Datastream ds =
                dsService.createDatastream(session, parentPath + "/mods", "text/xml", "mods.xml",
                        this.getClass().getResourceAsStream("/mods.xml"));

        session.save();

        latch.await(5, TimeUnit.SECONDS);
        // this.getOutputNode(null);

        System.out.println("reached the end " + ds);

    }

    private SequencingListener addSequenceListener(final CountDownLatch latch, final String parentPath)
            throws Exception {
        observationManager = session.getWorkspace().getObservationManager();

        final SequencingListener listener = new SequencingListener(latch);
        observationManager.addEventListener(listener, org.modeshape.jcr.api.observation.Event.Sequencing.ALL,
                parentPath, true, null, null, false);

        return listener;
    }

    public class SequencingListener implements EventListener {

        private final CountDownLatch latch;

        private volatile String sequencedNodePath;

        private volatile boolean successfulSequencing;

        public SequencingListener(final CountDownLatch latch) {
            this.latch = latch;
        }

        public boolean isSequencingSuccessful() {
            return this.successfulSequencing;
        }

        public String getSequencedNodePath() {
            return sequencedNodePath;
        }

        @Override
        public void onEvent(final EventIterator events) {
            System.out.println("Got an event time");
            latch.countDown();
            while (events.hasNext()) {
                try {
                    final Event event = events.nextEvent();

                    System.out.println(event);
                } catch (final Exception e) {
                }
            }
        }
    }
}
