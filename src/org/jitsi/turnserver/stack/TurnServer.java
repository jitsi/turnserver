/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.stack;

import java.io.*;
import java.net.*;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.socket.*;
import org.ice4j.stunclient.*;
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

    private boolean started = false;

    private TurnStack turnStack = null;

    private IceUdpSocketWrapper sock;

    private final ServerPeerUdpEventHandler peerUdpHandler;
    
    private final ServerChannelDataEventHandler channelDataHandler;
    
    public TurnServer(TransportAddress localUDPAddress)
    {
        this.localAddress = localUDPAddress;
        this.peerUdpHandler = new ServerPeerUdpEventHandler();
        this.channelDataHandler = new ServerChannelDataEventHandler();
        
        turnStack = new TurnStack(this.peerUdpHandler,this.channelDataHandler);
        
        this.peerUdpHandler.setTurnStack(turnStack);
        this.channelDataHandler.setTurnStack(turnStack);
        
        logger.info("Server initialized Waiting to be started");
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        //getPublicAddress();
        //getLocalAddress();
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

    public static void getLocalAddress()
    {
        System.out.print("Server public IP and port : ");
        System.out.println("127.0.0.1:3478");
    }
    
    public static void getPublicAddress() throws UnknownHostException, Exception
    {
        System.out.print("Server public IP and port : ");
        StunDiscoveryReport report =
            StunClient.getReport("stunserver.org", "3478",
                InetAddress.getLocalHost().getHostAddress(), "3478");
        System.out.println(report.getPublicAddress());
        
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
        sock =
            new IceUdpSocketWrapper(new SafeCloseDatagramSocket(localAddress));
        /*
        BindingRequestListener listner = new BindingRequestListener(turnStack);
        listner.start();
         */ 
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
        
        SendIndicationListener sendIndListener = 
        	new SendIndicationListener(turnStack);
        sendIndListener.setLocalAddress(localAddress);
        
        allocationRequestListner.start();
        channelBindRequestListener.start();
        connectionBindRequestListener.start();
        connectRequestListener.start();
        createPermissionRequestListener.start();
        refreshRequestListener.start();
        
        sendIndListener.start();
        
        turnStack.addSocket(sock);
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
        sock.close();
        sock = null;

        localAddress = null;
        this.started = false;
        logger.info("Server stopped");
    }

    public boolean isStarted()
    {
        return started;
    }

    @Override
    protected void finalize() throws Throwable
    {
        // to free resources by default if shutdown is not invoked before the
        // object is destructed
        if (isStarted())
        {
            shutDown();
        }
    }
}
