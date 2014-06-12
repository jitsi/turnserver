package org.jitsi.turnserver.stack;

import java.util.*;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

import org.jitsi.turnserver.listeners.*;

/**
 * Class to handle UDP messages coming from Peer. The class first checks if
 * there is a non-expired ChannelBind for the peer if yes it then sends a
 * ChannelData message to Client. If no it then finds if there is a non-expired
 * permission if yes then it sends a DataIndicatio to Client. All the mesages
 * sent to client here are from the address on which the allocation request was
 * received or the serverAddress of fiveTuple of corresponding Allocation.
 * 
 * @author Aakash Garg
 */
public class ServerPeerUdpEventHandler implements PeerUdpMessageEventHandler 
{
    /**
     * The <tt>Logger</tt> used by the <tt>PeerUdpMessageEventHandler</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(CreatePermissionRequestListener.class.getName());

    /**
     * The turnStack to call.
     */
    private TurnStack turnStack;

    /**
     * Default constructor.
     */
    public ServerPeerUdpEventHandler()
    {
    }
    
    /**
     * Parametrized constructor.
     * @param turnStack the turnStack to set for this class.
     */
    public ServerPeerUdpEventHandler(StunStack turnStack) {
	if (turnStack instanceof TurnStack)
        {
            this.turnStack = (TurnStack) turnStack;
        }
        else
        {
            throw new IllegalArgumentException("This is not a TurnStack!");
        }
    }

    public void setTurnStack(TurnStack turnStack)
    {
	this.turnStack = turnStack;
    }
    
    /**
     * Handles the PeerUdpMessageEvent.
     * @param evt the PeerUdpMessageEvent to handle/process.
     */
    @Override
    public void handleMessageEvent(PeerUdpMessageEvent evt) 
    {
	if (logger.isLoggable(Level.FINER))
        {
            logger.setLevel(Level.FINEST);
            logger.finer("Received Peer UdP message message " + evt);
        }
	
	byte[] data = evt.getBytes();    
	TransportAddress relayAddress = evt.getLocalAddress();
	TransportAddress remoteAddress = evt.getRemoteAddress();
	logger.finest("Received a UDP message on: " + relayAddress
		+ ", data: " + Arrays.toString(data));
	Allocation allocation = this.turnStack
		.getServerAllocation(relayAddress);
	if(allocation == null)
	{
	    // received on an address for which no allocation exists.
	    return;
	}
	else if(allocation.getChannel(remoteAddress) != 0x1000)
	{
	    char channelNo = allocation.getChannel(remoteAddress);
	    ChannelData channelData = new ChannelData();
	    channelData.setChannelNumber(channelNo);
	    channelData.setData(data);
	    try {
		logger.finest("Sending a ChannelData message "+channelData
			+" from "+allocation.getServerAddress()+" to "
			+allocation.getClientAddress());
		
		this.turnStack.sendChannelData(channelData,
		    allocation.getClientAddress(),
		    allocation.getServerAddress());
	    } catch (StunException ex) {
		logger.finer(ex.getMessage());
	    }
	}
	else if(allocation.isPermitted(remoteAddress))
	{
	    TransactionID tranID = TransactionID.createNewTransactionID();
	    Indication dataInd = MessageFactory.createDataIndication(
		    remoteAddress, data, tranID.getBytes());
	    try {
		logger.finest("Sending a ChannelData message "+dataInd
			+" from "+allocation.getServerAddress()+" to "
			+allocation.getClientAddress());
		
		this.turnStack.sendIndication(dataInd,
		    allocation.getClientAddress(),
		    allocation.getServerAddress());
	    } catch (StunException e) {
		logger.finer(e.getMessage());
	    }
	}
    }

}
