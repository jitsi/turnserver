/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.turnClient;

import java.io.*;
import java.util.logging.*  ;

import org.ice4j.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

import org.jitsi.turnserver.listeners.*;
import org.jitsi.turnserver.stack.*;

/**
 * Handles the incoming ChannelData message for Client from Server.
 * 
 * @author Aakash Garg
 * 
 */
public class ClientChannelDataEventHandler implements
	ChannelDataEventHandler {
    
    /**
     * The <tt>Logger</tt> used by the
     * <tt>ServerChannelDataEventHandler</tt> class and its instances for
     * logging output.
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
    public ClientChannelDataEventHandler()
    {
    }
    
    /**
     * parametrised contructor.
     * 
     * @param turnStack
     *            the turnStack for this class.
     */
    public ClientChannelDataEventHandler(StunStack turnStack) 
    {
	if (turnStack instanceof TurnStack)
        {
            this.turnStack = (TurnStack) turnStack;
        }
        else
        {
            throw new IllegalArgumentException("This is not a TurnStack!");
        }
    }

    /**
     * Sets the turnStack for this class.
     * 
     * @param turnStack
     *            the turnStack to set for this class.
     */
    public void setTurnStack(TurnStack turnStack)
    {
	this.turnStack = turnStack;
    }
    
    /**
     * Handles the ChannelDataMessageEvent.
     * 
     * @param evt
     *            the ChannelDataMessageEvent to handle/process.
     */
    @Override
    public void handleMessageEvent(ChannelDataMessageEvent evt) 
    {
	if (logger.isLoggable(Level.FINER))
        {
            logger.setLevel(Level.FINEST);
            logger.finer("Received ChannelData message " + evt);
        }
	ChannelData channelData = evt.getChannelDataMessage();
	char channelNo = channelData.getChannelNumber();
	byte[] data = channelData.getData();
	try {
	    String line = new String(data,"UTF-8");
	    System.out.println(line);
	} catch (UnsupportedEncodingException e) {
	    System.err.println("Unable to get back String.");
	}
/**	System.out.println("Received a ChannelData message for "
		+ (int) channelNo + " , message : " + Arrays.toString(data));
    **/
    }
}
