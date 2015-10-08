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

import org.jitsi.turnserver.collectors.AllocationResponseCollector;
import org.jitsi.turnserver.listeners.*;
import org.jitsi.turnserver.stack.*;

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
    private static TurnStack turnStack;
    private static TransportAddress localAddress;
    private static TransportAddress serverAddress;
    private static boolean started;
    
    /**
     * The instance that should be notified when an incoming UDP message has
     * been processed and ready for delivery
     */
    private PeerUdpMessageEventHandler peerUdpMessageEventHandler;

    /**
     * The instance that should be notified when an incoming ChannelData message
     * has been processed and ready for delivery
     */
    private ChannelDataEventHandler channelDataEventHandler;

    /**
     * @param args
     * @throws IOException 
     * @throws StunException 
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws IOException, StunException,
	    InterruptedException
    {
        String[] temp = {InetAddress.getLocalHost().getHostAddress(),"3478"};
//        String[] temp = {"176.31.40.85","3478"};
        args = temp;
       Transport protocol = Transport.UDP;
        
        // uses args as server name and port
        localAddress =
            new TransportAddress(InetAddress.getLocalHost(), 5678, protocol);
        serverAddress =
            new TransportAddress(args[0], Integer.valueOf(
                args[1]).intValue(), protocol);
        System.out.println("Client adress : "+localAddress);
        System.out.println("Server adress : "+serverAddress);
        start();
        StunMessageEvent evt = null;
        evt = sendAllocationRequest(localAddress,serverAddress);
        evt = sendCreatePermissionRequest(9999);
        evt = sendCreatePermissionRequest(9999);
        evt = sendCreatePermissionRequest(11000);
        
        TransportAddress peerAddr 
            = new TransportAddress(InetAddress.getLocalHost(), 11000, protocol);
        
        evt = sendChannelBindRequest((char) 0x4000,peerAddr);
        sendChannelDataMessage();
        System.out.println("Starting interactive communication.");
        doInteractiveComm();
/*        System.out.println("Thread will now sleep.");
        Thread.sleep(600*1000);
*/        
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
	    AllocationResponseCollector allocResCollec 
	    	= new AllocationResponseCollector(turnStack);
/*	    turnStack.sendRequest(request, serverAddress, localAddress,
		    allocResCollec);
*/            evt = requestSender.sendRequestAndWaitForResponse(
                    request, serverAddress);
		allocResCollec.processResponse((StunResponseEvent) evt);
        }
        catch (Exception ex)
        {
            //this shouldn't happen since we are the ones that created the
            //request
            ex.printStackTrace();
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
    
    public static void sendChannelDataMessage() throws StunException,
	    IOException
    {
	byte[] message = {0xa,0xb};
	ChannelData channelData = new ChannelData();
	channelData.setChannelNumber((char)0x4000);
	channelData.setData(message);
	turnStack.sendChannelData(channelData, serverAddress, localAddress);
	System.out.println("ChannelData message sent.");
    }
    
    /**
     * Puts the discoverer into an operational state.
     * @throws IOException if we fail to bind.
     * @throws StunException if the stun4j stack fails start for some reason.
     */
    public static void start()
        throws IOException, StunException
    {
	ClientChannelDataEventHandler channelDataHandler 
		= new ClientChannelDataEventHandler();
	turnStack = new TurnStack(null, channelDataHandler);
        channelDataHandler.setTurnStack(turnStack);
        sock = new IceUdpSocketWrapper(
            new SafeCloseDatagramSocket(localAddress));
        turnStack.addSocket(sock);

        DataIndicationListener dataIndListener = 
        	new DataIndicationListener(turnStack);
        dataIndListener.setLocalAddress(localAddress);
        dataIndListener.start();
        
        requestSender = new BlockingRequestSender(turnStack, localAddress);

        started = true;
    }
    
    
    public static void doInteractiveComm() throws IOException, StunException
    {
	BufferedReader br 
		= new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Started interaction start typing message");
	String line = br.readLine();
	System.out.println("My first message : "+line);
	while(line!=null)
	{
	    byte[] data = line.getBytes();
	    TransactionID tran = TransactionID.createNewTransactionID();
	    TransportAddress peerAddress = new TransportAddress(
		    InetAddress.getLocalHost(), 11000, Transport.UDP);
	    Indication ind = MessageFactory.createSendIndication(peerAddress,
		    data, tran.getBytes());
	    System.out.println("Trying to send message to server");
	    turnStack.sendIndication(ind, serverAddress, localAddress);
	    System.out.println("message sent");
	    
	    System.out.println("Type a new message : ");
	    line = br.readLine();
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

        localAddress  = null;
        requestSender = null;

        started = false;
    }
    
}
