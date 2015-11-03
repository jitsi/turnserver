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
package org.jitsi.turnserver.turnClient;

import java.io.*;
import java.net.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.socket.*;
import org.ice4j.stack.*;
import org.ice4j.stunclient.*;

import org.jitsi.turnserver.collectors.*;
import org.jitsi.turnserver.listeners.*;
import org.jitsi.turnserver.stack.*;

/**
 * Class to run Allocation Client over TCP.
 * 
 * @author Aakash Garg
 * 
 */
public class TurnTcpAllocationClient
{
    private static BlockingRequestSender requestSender;

    private static IceTcpSocketWrapper sock;

    private static TurnStack turnStack;

    private static TransportAddress localAddress;

    private static TransportAddress serverAddress;

    private static boolean started;

    private static Socket tcpSocketToServer = null;

    /**
     * The instance that should be notified when an incoming TCP message has
     * been processed and ready for delivery
     */
    private PeerUdpMessageEventHandler peerUdpMessageEventHandler;

    /**
     * Puts the discoverer into an operational state.
     * 
     * @throws IOException if we fail to bind.
     * @throws StunException if the stun4j stack fails start for some reason.
     */
    public static void start(Transport protocol) throws IOException,
        StunException
    {
        sock = new IceTcpSocketWrapper(tcpSocketToServer);
        System.out.println("Adding an new TCP connection to : "
            + serverAddress.getHostAddress());

        localAddress =
            new TransportAddress(InetAddress.getLocalHost(),
                tcpSocketToServer.getLocalPort(), protocol);
        System.out.println("Client adress : " + localAddress);
        System.out.println("Server adress : " + serverAddress);

        ClientChannelDataEventHandler channelDataHandler =
            new ClientChannelDataEventHandler();
        turnStack = new TurnStack(null, channelDataHandler);
        channelDataHandler.setTurnStack(turnStack);
        
        turnStack.addSocket(sock);

        requestSender = new BlockingRequestSender(turnStack, localAddress);

        ConnectionAttemptIndicationListener connectionAttemptIndicationListener =
            new ConnectionAttemptIndicationListener(turnStack/*,requestSender*/);
        connectionAttemptIndicationListener.setLocalAddress(localAddress);
        connectionAttemptIndicationListener.start();
        
        started = true;
    }

    /**
     * @param args
     * @throws IOException
     * @throws StunException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, StunException,
        InterruptedException
    {
        String[] temp =
        { InetAddress.getLocalHost().toString(), "3478" };
        args = temp;
        Transport protocol = Transport.TCP;

        // uses args as server name and port
        serverAddress =
            new TransportAddress(InetAddress.getLocalHost(), Integer.valueOf(
                args[1]).intValue(), protocol);

        tcpSocketToServer = new Socket(serverAddress.getHostAddress(), 3478);
        System.out.println("Local port chosen : " + tcpSocketToServer.getLocalPort());

        start(protocol);
        StunMessageEvent evt = null;
        evt = sendAllocationRequest(
            localAddress, serverAddress);
        evt = sendCreatePermissionRequest(9999);
        // evt = sendCreatePermissionRequest(9999);
        // evt = sendCreatePermissionRequest(11000);
        // evt = sendConnectRequest(9999);

        TransportAddress peerAddr =
            new TransportAddress(InetAddress.getLocalHost(), 11000, protocol);

        Thread.sleep(600 * 1000);

        shutDown();
    }

    public static StunMessageEvent sendAllocationRequest(
        TransportAddress localAddr, TransportAddress serverAddress)
        throws IOException
    {
        Request request = MessageFactory.createAllocateRequest();

        RequestedTransportAttribute requestedTransportAttribute =
            AttributeFactory
                .createRequestedTransportAttribute(RequestedTransportAttribute.TCP);

        request.putAttribute(requestedTransportAttribute);
        System.out.println("Message type : " + (int) request.getMessageType());
        StunMessageEvent evt = null;
        try
        {
            AllocationResponseCollector allocResCollec =
                new AllocationResponseCollector(turnStack);
            /*
             * turnStack.sendRequest(request, serverAddress, localAddress,
             * allocResCollec);
             */
            evt = requestSender.sendRequestAndWaitForResponse(
                request, serverAddress);
            allocResCollec.processResponse((StunResponseEvent) evt);
        }
        catch (Exception ex)
        {
            // this shouldn't happen since we are the ones that created the
            // request
            ex.printStackTrace();
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if (evt != null)
            System.out.println("Allocation TEST res="
                + (int) (evt.getMessage().getMessageType()) + " - "
                + evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to Allocation TEST.");
        return evt;
    }

    public static StunMessageEvent sendCreatePermissionRequest(int peerPort)
        throws IOException, StunException
    {
        System.out.println();
        TransportAddress peerAddr =
            new TransportAddress(serverAddress.getAddress(), peerPort,
                Transport.TCP);
        TransactionID tran = TransactionID.createNewTransactionID();
        System.out.println("Create request for : " + peerAddr);
        Request request = MessageFactory.createCreatePermissionRequest(
            peerAddr, tran.getBytes());
        StunMessageEvent evt = null;
        System.out.println("Permission tran : " + tran);
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                request, serverAddress, tran);
        }
        catch (StunException ex)
        {
            // this shouldn't happen since we are the ones that created the
            // request
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if (evt != null)
            System.out.println("Permission TEST res="
                + (int) (evt.getMessage().getMessageType()) + " - "
                + evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to Permission TEST.");

        return evt;
    }

    public static StunMessageEvent sendConnectRequest(int peerPort)
        throws IOException, StunException
    {
        System.out.println();
        TransportAddress peerAddr =
            new TransportAddress(serverAddress.getAddress(), peerPort,
                Transport.TCP);
        TransactionID tran = TransactionID.createNewTransactionID();
        System.out.println("Connect request for : " + peerAddr);
        Request request = MessageFactory.createConnectRequest(
            peerAddr, tran.getBytes());
        request.setTransactionID(tran.getBytes());
        StunMessageEvent evt = null;
        System.out.println("Connect Req tran : " + tran);
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                request, serverAddress, tran);
        }
        catch (StunException ex)
        {
            // this shouldn't happen since we are the ones that created the
            // request
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if (evt != null)
            System.out.println("Connect request TEST res="
                + (int) (evt.getMessage().getMessageType()) + " - "
                + evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to Connect Request TEST.");

        return evt;
    }

    public static void doInteractiveComm() throws IOException, StunException
    {
        System.out.println("---->Interactve Communication started<---------");
        BufferedReader br =
            new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = br.readLine()) != null)
        {
            byte[] data = line.getBytes();
            TransactionID tran = TransactionID.createNewTransactionID();
            TransportAddress peerAddress =
                new TransportAddress(InetAddress.getLocalHost(), 11000,
                    Transport.TCP);
            Indication ind = MessageFactory.createSendIndication(
                peerAddress, data, tran.getBytes());
            turnStack.sendIndication(
                ind, serverAddress, localAddress);
        }
    }

    /**
     * Shuts down the underlying stack and prepares the object for garbage
     * collection.
     */
    public static void shutDown()
    {
        turnStack.removeSocket(localAddress);
        sock.close();
        sock = null;

        localAddress = null;
        requestSender = null;

        started = false;
    }

}
