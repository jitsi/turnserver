/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */package org.jitsi.turnserver.turnClient;

import java.io.*;
import java.net.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.socket.*;
import org.ice4j.stack.*;
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
    private static TransportAddress serverAddress;
    private static boolean started;

    /**
     * @param args
     * @throws IOException 
     * @throws StunException 
     */
    public static void main(String[] args) throws IOException, StunException
    {
        String[] temp = {InetAddress.getLocalHost().toString(),"3478"};
        args = temp;
       Transport protocol = Transport.UDP;
        
        // uses args as server name and port
        localAddress =
            new TransportAddress(InetAddress.getLocalHost(), 5678, protocol);
        serverAddress =
            new TransportAddress(InetAddress.getLocalHost(), Integer.valueOf(
                args[1]).intValue(), protocol);
        System.out.println("Client adress : "+localAddress);
        System.out.println("Server adress : "+serverAddress);
        start();
        StunMessageEvent evt = null;
        evt = sendAllocationRequest(localAddress,serverAddress);
        evt = sendCreatePermissionRequest(9999);
        evt = sendCreatePermissionRequest(9999);
        evt = sendCreatePermissionRequest(10000);
        
        TransportAddress peerAddr 
            = new TransportAddress(InetAddress.getLocalHost(), 11000, protocol);
        evt = sendChannelBindRequest((char) 0x4000,peerAddr);
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
            System.out.println("Allocation TEST res="
                               +(int)(evt.getMessage().getMessageType())
                               +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to Allocation TEST.");
        return evt;
    }
    
    public static StunMessageEvent sendCreatePermissionRequest(int peerPort)
        throws IOException, StunException
    {
        System.out.println();
        TransportAddress peerAddr = new TransportAddress(
                                        serverAddress.getAddress(),
                                        peerPort,
                                        Transport.UDP);
        TransactionID tran = TransactionID.createNewTransactionID();
        System.out.println("Create request for : "+peerAddr);
        Request request  
            = MessageFactory.createCreatePermissionRequest(
                peerAddr,
                tran.getBytes());
        StunMessageEvent evt = null;
        System.out.println("Permission tran : "+tran);
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                    request, serverAddress,tran);
        }
        catch (StunException ex)
        {
            //this shouldn't happen since we are the ones that created the
            //request
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if(evt != null)
            System.out.println("Permission TEST res="
                               +(int)(evt.getMessage().getMessageType())
                               +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to Permission TEST.");
       
        return evt;
    }
    
    public static StunMessageEvent sendChannelBindRequest(
                    char channelNo, 
                    TransportAddress peerAddress)
        throws IOException, StunException
    {
        System.out.println();
        System.out.println("ChannelBind request for : "+peerAddress
                    +" on "+(int)channelNo);
        TransactionID tran = TransactionID.createNewTransactionID();
        Request request
            = MessageFactory.createChannelBindRequest(
                                                        channelNo,
                                                        peerAddress,
                                                        tran.getBytes());
        char cNo =
            ((ChannelNumberAttribute) (request
                .getAttribute(Attribute.CHANNEL_NUMBER))).getChannelNumber();
        TransportAddress pAddr =
            ((XorPeerAddressAttribute) (request
                .getAttribute(Attribute.XOR_PEER_ADDRESS))).getAddress();
        
        XorMappedAddressAttribute mappedAddr =
            AttributeFactory.createXorMappedAddressAttribute(
                localAddress, tran.getBytes());
        //mappedAddr.setAddress(mappedAddr.getAddress(), tran.getBytes());
        System.out.println(">"+mappedAddr.getAddress());
        request.putAttribute(mappedAddr);
        System.out.println("input mappedAddress : "+mappedAddr.getAddress());
        
        XorMappedAddressAttribute retMapAddr
            = (XorMappedAddressAttribute) (request
            .getAttribute(Attribute.XOR_MAPPED_ADDRESS));
        
        TransportAddress mAddr =
            (retMapAddr).getAddress();
        System.out.println("output mappedAddress : "+mAddr.getHostAddress());
        
        System.out.println("Retrived ChannelBind request is : "+pAddr
            +" on "+(int)cNo);

        StunMessageEvent evt = null;
        System.out.println("ChannelBind tran : "+tran);
        try
        {
            evt = requestSender.sendRequestAndWaitForResponse(
                    request, serverAddress,tran);
        }
        catch (StunException ex)
        {
            //this shouldn't happen since we are the ones that created the
            //request
            System.out.println("Internal Error. Failed to encode a message");
            return null;
        }

        if(evt != null)
            System.out.println("ChannelBind TEST res="
                               +evt.getRemoteAddress().toString()
                               +" - "+ evt.getRemoteAddress().getHostAddress());
        else
            System.out.println("NO RESPONSE received to ChannelBind TEST.");
       
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
