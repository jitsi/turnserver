/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */package org.jitsi.turnserver.turnClient;

import java.io.IOException;
import java.net.InetAddress;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.socket.*;
import org.ice4j.stunclient.*;

import org.jitsi.turnserver.stack.TurnStack;

/**
 * Class to run Allocation Client.
 * 
 * @author Aakash Garg
 *
 */
public class TurnAllocationClient
{
    private static BlockingRequestSender requestSender;
    private static IceUdpSocketWrapper sock;
    private static TurnStack stunStack;
    private static TransportAddress localAddress;
    private static boolean started;

    /**
     * @param args
     * @throws IOException 
     * @throws StunException 
     */
    public static void main(String[] args) throws IOException, StunException
    {
        String[] temp = {"127.0.0.1","3478"};
        args = temp;
       Transport protocol = Transport.UDP;
        
        // uses args as server name and port
        TransportAddress localAddr =
            new TransportAddress(InetAddress.getLocalHost(), 5678, protocol);
        TransportAddress serverAddr =
            new TransportAddress(args[0], Integer.valueOf(
                args[1]).intValue(), protocol);
        localAddress = new TransportAddress("127.0.0.1",7777, protocol);
        start();
        StunMessageEvent evt = sendAllocationRequest(localAddr,serverAddr);
        evt = sendAllocationRequest(localAddr,serverAddr);
        shutDown();
    }

    public static StunMessageEvent sendAllocationRequest(
        TransportAddress localAddr, TransportAddress serverAddress)
        throws IOException
  {
        Request request = MessageFactory.createAllocateRequest();
        
        RequestedTransportAttribute requestedTransportAttribute =
            AttributeFactory.createRequestedTransportAttribute(
                RequestedTransportAttribute.UDP);
        
        System.out.println(requestedTransportAttribute.getRequestedTransport());
        request.putAttribute(requestedTransportAttribute);
        
        StunMessageEvent evt = null;
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                    request, serverAddress);
        }
        catch (StunException ex)
        {
            //this shouldn't happen since we are the ones that created the
            //request
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if(evt != null)
            System.out.println("TEST res="+evt.getRemoteAddress().toString()
                               +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to TEST.");
        return evt;
    }

    /**
     * Puts the discoverer into an operational state.
     * @throws IOException if we fail to bind.
     * @throws StunException if the stun4j stack fails start for some reason.
     */
    public static void start()
        throws IOException, StunException
    {
        stunStack = new TurnStack();
        sock = new IceUdpSocketWrapper(
            new SafeCloseDatagramSocket(localAddress));

        stunStack.addSocket(sock);

        requestSender = new BlockingRequestSender(stunStack, localAddress);

        started = true;
    }
    

    /**
     * Shuts down the underlying stack and prepares the object for garbage
     * collection.
     */
    public static void shutDown()
    {
        stunStack.removeSocket(localAddress);
        sock.close();
        sock = null;

        localAddress  = null;
        requestSender = null;

        started = false;
    }
    
}
