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

package org.jitsi.turnserver.stack;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.socket.*;

import org.jitsi.turnserver.*;
import org.jitsi.turnserver.listeners.*;

/**
 * The class to run a Turn server.
 * 
 * @author Aakash Garg
 */
public class TurnServer
{
    private static Logger logger = Logger.getLogger(TurnServer.class.getName());

    private TransportAddress localAddress = null;

    // requested maximum length of the queue of incoming connections
    private int backlog = 50;

    private boolean started = false;

    private TurnStack turnStack = null;

    private IceUdpSocketWrapper turnUdpSocket;

    private final ServerPeerUdpEventHandler peerUdpHandler;

    private final ServerChannelDataEventHandler channelDataHandler;

    private IceSocketWrapper turnTcpServerSocket;

    public TurnServer(TransportAddress localUDPAddress)
    {
        this.localAddress = localUDPAddress;
        this.peerUdpHandler = new ServerPeerUdpEventHandler();
        this.channelDataHandler = new ServerChannelDataEventHandler();

        turnStack = new TurnStack(this.peerUdpHandler, this.channelDataHandler);
        System.out.println("Setting turnstack.");
        this.peerUdpHandler.setTurnStack(turnStack);
        this.channelDataHandler.setTurnStack(turnStack);

        logger.info("Server initialized Waiting to be started");
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        TransportAddress localAddress = null;
        if (args.length == 2)
        {
            localAddress =
                new TransportAddress(args[0], Integer.valueOf(args[1]),
                    Transport.UDP);
        }
        else
        {
            localAddress =
                new TransportAddress(InetAddress.getLocalHost(), 3478,
                    Transport.UDP);
        }
        TurnServer server = new TurnServer(localAddress);
        server.start();
        Thread.sleep(600 * 1000);
        if (server.isStarted())
        {
            server.shutDown();
        }
    }

    /**
     * Function to start the server
     * 
     * @throws IOException
     * @throws TurnException
     */
    public void start() throws IOException, TurnException
    {
        if (localAddress == null)
        {
            throw new RuntimeException("Local address not initialized");
        }

        AllocationRequestListener allocationRequestListner =
            new AllocationRequestListener(turnStack);
        ChannelBindRequestListener channelBindRequestListener =
            new ChannelBindRequestListener(turnStack);
        ConnectionBindRequestListener connectionBindRequestListener =
            new ConnectionBindRequestListener(turnStack);
        ConnectRequestListener connectRequestListener =
            new ConnectRequestListener(turnStack);
        CreatePermissionRequestListener createPermissionRequestListener =
            new CreatePermissionRequestListener(turnStack);
        RefreshRequestListener refreshRequestListener =
            new RefreshRequestListener(turnStack);
        BindingRequestListener bindingRequestListener =
            new BindingRequestListener(turnStack);

        SendIndicationListener sendIndListener =
            new SendIndicationListener(turnStack);
        sendIndListener.setLocalAddress(localAddress);

        allocationRequestListner.start();
        channelBindRequestListener.start();
        connectionBindRequestListener.start();
        connectRequestListener.start();
        createPermissionRequestListener.start();
        refreshRequestListener.start();
        bindingRequestListener.start();

        sendIndListener.start();

        System.out.println("Local address - " + localAddress.getHostAddress()
            + ":" + localAddress.getPort());
        // instance a server socket for TCP
        ServerSocket tcpServerSocket =
            new ServerSocket(localAddress.getPort(), backlog,
            localAddress.getAddress());
        // set reuse to allow binding the socket to the same address
        tcpServerSocket.setReuseAddress(true);
        System.out.println("Adding a TCP server socket - "
            + tcpServerSocket.getLocalSocketAddress());
        // create ICE socket wrapper for TCP
        turnTcpServerSocket = new IceTcpServerSocketWrapper(tcpServerSocket,
            turnStack.getComponent());
        // instance a datagram socket for UDP
        SafeCloseDatagramSocket udpServerSocket =
            new SafeCloseDatagramSocket(localAddress.getPort(),
            localAddress.getAddress());
        // set reuse to allow binding the datagram socket to the same address
        udpServerSocket.setReuseAddress(true);
        System.out.println("Adding a UDP server socket - "
            + udpServerSocket.getLocalSocketAddress());
        // create ICE socket wrapper for UDP
        turnUdpSocket = new IceUdpSocketWrapper(udpServerSocket);
        // add the TCP socket to the stack
        turnStack.addSocket(turnTcpServerSocket);
        // add the UDP socket to the stack
        turnStack.addSocket(turnUdpSocket);
        started = true;
        logger.info("Server started, listening on " + localAddress.getAddress()
            + ":" + localAddress.getPort());

    }

    /**
     * function to stop the server and free resources allocated by it.
     */
    public void shutDown()
    {
        logger.info("Stopping server at " + localAddress.getAddress() + ":"
            + localAddress.getPort());
        turnStack.removeSocket(localAddress);
        turnStack = null;
        turnUdpSocket.close();
        turnUdpSocket = null;
        this.turnTcpServerSocket.close();
        this.turnTcpServerSocket = null;
        
        localAddress = null;
        this.started = false;
        logger.info("Server stopped");
    }

    public boolean isStarted()
    {
        return started;
    }

    /**
     * Sets the incoming connection backlog.
     *
     * @param backlog
     */
    public void setBacklog(int backlog)
    {
        this.backlog = backlog;
    }

    @Override
    public void finalize() throws Throwable
    {
        // to free resources by default if shutdown is not invoked before the
        // object is destroyed
        shutDown();
    }
}
