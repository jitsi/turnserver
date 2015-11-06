/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 *
 * Copyright @ 2015 Atlassian Pty Ltd
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

package org.jitsi.turnserver.client;

import static org.junit.Assert.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import org.jitsi.turnserver.*;
import org.jitsi.turnserver.stack.*;
import org.junit.*;
import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.socket.*;
import org.ice4j.stack.*;
import org.ice4j.stunclient.*;

/**
 * This tests clients against a local turnserver.
 *
 * @author Paul Gregoire
 */
public class ClientTest
{

    private static ExecutorService executor;

    // holder for reference to the running turnserver
    private static Future<?> future;

    private static TurnServer turnServer;

    private static TransportAddress serverAddr;

    // goggle stun for testing purposes
    TransportAddress googAddr = new TransportAddress("stun.l.google.com",
                    19302, Transport.UDP);

    @Before
    public void setUp() throws Exception
    {
        // instance an executor
        executor = Executors.newCachedThreadPool();
        // create an address for binding
        serverAddr =
            new TransportAddress(getLocalAddress(), 3478, Transport.UDP);
        // instance a turnserver
        turnServer = new TurnServer(serverAddr);
        // start the server
        future = executor.submit(new Runnable()
        {
            public void run()
            {
                try
                {
                    turnServer.start();
                }
                catch (TurnException e)
                {
                    e.printStackTrace();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                do
                {
                    try
                    {
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e)
                    {
                        // e.printStackTrace();
                    }
                } while (turnServer.isStarted());
            }
        });
    }

    @After
    public void tearDown() throws Exception
    {
        // stop the server
        turnServer.shutDown();
        // cancel the future
        future.cancel(true);
    }

    @Test
    public void testUDPClient() throws Exception
    {
        // create a client address for binding
        TransportAddress clientAddr =
            new TransportAddress(getLocalAddress(), 5678, Transport.UDP);
        // create a discover process
        NetworkConfigurationDiscoveryProcess addressDiscovery =
            new NetworkConfigurationDiscoveryProcess(new StunStack(),
                            clientAddr, serverAddr);
        // start discovery process
        addressDiscovery.start();
        // pull the report
        StunDiscoveryReport report = addressDiscovery.determineAddress();
        System.out.println(report);
        assertNotNull(report.getPublicAddress());
        // stop discovery process
        addressDiscovery.shutDown();
    }

    @Test
    public void testIceMediaStream() throws Exception
    {
        // instance a control-side
        Future<?> ctrl = executor.submit(new ControlClient());
        // instance an end point / subscriber
        Future<?> end = executor.submit(new EndpointClient());
        // sleep this thread
        Thread.sleep(3000L);
        // kill the clients
        ctrl.cancel(true);
        end.cancel(true);
    }

    /**
     * Utility method to provide a local IP address for current interfaces.
     * 
     * @return local address or default of 127.0.0.1
     */
    private String getLocalAddress()
    {
        Enumeration<NetworkInterface> ifaces;
        try
        {
            ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements())
            {
                NetworkInterface iface =
                    (NetworkInterface) ifaces.nextElement();
                Enumeration<InetAddress> iaddresses = iface.getInetAddresses();
                while (iaddresses.hasMoreElements())
                {
                    InetAddress iaddress =
                        (InetAddress) iaddresses.nextElement();
                    if (!iaddress.isLoopbackAddress()
                                    && !iaddress.isLinkLocalAddress()
                                    && !iaddress.isSiteLocalAddress())
                    {
                        String hostAddr = iaddress.getHostAddress();
                        return hostAddr != null ? hostAddr : iaddress
                                        .getHostName();
                    }
                }
            }
            String localhostAddr = InetAddress.getLocalHost().getHostAddress();
            return localhostAddr != null ? localhostAddr : InetAddress
                            .getLocalHost().getHostName();
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    class ControlClient implements Runnable
    {

        public void run()
        {
            // instance an ICE agent
            Agent agent = new Agent();
            agent.setControlling(true);
            agent.setTrickling(true);
            String user = agent.getLocalUfrag();
            String passwd = agent.getLocalPassword();
            System.out.printf("Control - user: %s passwd: %s\n", user, passwd);
            // add havesters
            // agent.addCandidateHarvester(new
            // StunCandidateHarvester(googAddr));
            agent.addCandidateHarvester(new TurnCandidateHarvester(serverAddr));
            System.out.printf("Agent state: %s\n", agent.getState());
            // add a stream
            IceMediaStream stream = agent.getStream("junit");
            assertNull(stream);
            stream = agent.createMediaStream("junit");
            assertNotNull(stream);
            // create a data component
            Component rtp = null;
            try
            {
                rtp =
                    agent.createComponent(stream, Transport.UDP, 6000, 6000,
                                    6016);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            for (CandidateHarvester harvester : agent.getHarvesters())
            {
                System.out.printf("Harvesting for rtp via %s\n", harvester);
                try
                {
                    Collection<LocalCandidate> candidates =
                        harvester.harvest(rtp);
                    if (candidates.isEmpty())
                    {
                        System.out.println("No candidates found");
                        continue;
                    }
                    System.out.printf("Harvested candidates: %s\n", candidates);
                    for (LocalCandidate candidate : candidates)
                    {
                        rtp.addLocalCandidate(candidate);
                    }
                    break;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            // start connection establishment
            agent.startConnectivityEstablishment();
            IceProcessingState state;
            while ((state = agent.getState()) != IceProcessingState.TERMINATED)
            {
                System.out.printf(
                                "Control connectivity establishment in process, state: %s\n",
                                state);
                try
                {
                    if (state == IceProcessingState.FAILED) { throw new Exception(
                                    "ICE connectivity failed"); }
                    // sleep until ICE terminates or fails
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    agent.free();
                    break;
                }
            }
            // get candidates
            CandidatePair rtpPair =
                agent.getStream("junit").getComponent(Component.RTP)
                                .getSelectedPair();
            System.out.printf("Control IP: %s\n", rtpPair.getRemoteCandidate()
                            .getTransportAddress());
            IceUdpSocketWrapper wrapper =
                new IceUdpSocketWrapper(rtpPair.getDatagramSocket());
            // wrapper.send(packet);
        }
    }

    class EndpointClient implements Runnable
    {

        public void run()
        {
            // instance an ICE agent
            Agent agent = new Agent();
            agent.setControlling(false);
            agent.setTrickling(true);
            String user = agent.getLocalUfrag();
            String passwd = agent.getLocalPassword();
            System.out.printf("Endpoint - user: %s passwd: %s\n", user, passwd);
            // add havesters
            // agent.addCandidateHarvester(new
            // StunCandidateHarvester(googAddr));
            agent.addCandidateHarvester(new TurnCandidateHarvester(serverAddr));
            System.out.printf("Agent state: %s\n", agent.getState());
            // add a stream
            IceMediaStream stream = agent.getStream("junit");
            assertNull(stream);
            stream = agent.createMediaStream("junit");
            assertNotNull(stream);
            // create a data component
            Component rtp = null;
            try
            {
                rtp =
                    agent.createComponent(stream, Transport.UDP, 7000, 7000,
                                    7016);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            for (CandidateHarvester harvester : agent.getHarvesters())
            {
                System.out.printf("Harvesting for rtp via %s\n", harvester);
                try
                {
                    Collection<LocalCandidate> candidates =
                        harvester.harvest(rtp);
                    if (candidates.isEmpty())
                    {
                        System.out.println("No candidates found");
                        continue;
                    }
                    System.out.printf("Harvested candidates: %s\n", candidates);
                    for (LocalCandidate candidate : candidates)
                    {
                        rtp.addLocalCandidate(candidate);
                    }
                    break;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            // start connection establishment
            agent.startConnectivityEstablishment();
            IceProcessingState state;
            while ((state = agent.getState()) != IceProcessingState.TERMINATED)
            {
                System.out.printf(
                                "Endpoint connectivity establishment in process, state: %s\n",
                                state);
                try
                {
                    if (state == IceProcessingState.FAILED) { throw new Exception(
                                    "ICE connectivity failed"); }
                    // sleep until ICE terminates or fails
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    agent.free();
                    break;
                }
            }
            // get candidates
            CandidatePair rtpPair =
                agent.getStream("junit").getComponent(Component.RTP)
                                .getSelectedPair();
            System.out.printf("Endpoint IP: %s\n", rtpPair.getRemoteCandidate()
                            .getTransportAddress());
            IceUdpSocketWrapper wrapper =
                new IceUdpSocketWrapper(rtpPair.getDatagramSocket());
            // wrapper.receive(packet);
        }
    }

}
