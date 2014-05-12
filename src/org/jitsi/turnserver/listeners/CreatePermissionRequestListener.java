/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.listeners;

import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

/**
 * The class that would be handling and responding to incoming CreatePermission
 * requests that are CreatePermission and sends a success or error response
 * 
 * @author Aakash Garg
 */
public class CreatePermissionRequestListener
    implements RequestListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>CreatePermissionRequestListener</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(CreatePermissionRequestListener.class.getName());

    private final StunStack stunStack;

    /**
     * The indicator which determines whether this
     * <tt>CreatePermissionrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new CreatePermissionRequestListener
     * 
     * @param stunStack
     */
    public CreatePermissionRequestListener(StunStack stunStack)
    {
        this.stunStack = stunStack;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ice4j.stack.RequestListener#processRequest(org.ice4j.StunMessageEvent
     * )
     */
    @Override
    public void processRequest(StunMessageEvent evt)
        throws IllegalArgumentException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.finer("Received request " + evt);
        }
        Message message = evt.getMessage();
        if (message.getMessageType() == Message.CREATEPERMISSION_REQUEST)
        {
            XorPeerAddressAttribute xorPeerAddressAttribute =
                (XorPeerAddressAttribute) message
                    .getAttribute(Attribute.XOR_PEER_ADDRESS);
            // we should get multiple xor peer address attributes here
            Response response = null;
            boolean inSufficientCapacity = true;
            boolean peerAddressAllowed = true;
            if (xorPeerAddressAttribute == null)
            {
                response =
                    MessageFactory
                        .createCreatePermissionErrorResponse(ErrorCodeAttribute.BAD_REQUEST);
            }
            else if (inSufficientCapacity)
            {
                response =
                    MessageFactory
                        .createCreatePermissionErrorResponse(ErrorCodeAttribute.INSUFFICIENT_CAPACITY);
            }
            else if (peerAddressAllowed)
            {
                response =
                    MessageFactory
                        .createCreatePermissionErrorResponse(ErrorCodeAttribute.FORBIDDEN);
            }
            try
            {
                stunStack.sendResponse(evt.getTransactionID().getBytes(),
                    response, evt.getLocalAddress(), evt.getRemoteAddress());
            }
            catch (Exception e)
            {
                logger.log(Level.INFO, "Failed to send " + response
                    + " through " + evt.getLocalAddress(), e);
                // try to trigger a 500 response although if this one failed,
                throw new RuntimeException("Failed to send a response", e);
            }
        }
        else
        {
            return;
        }
    }

    /**
     * Starts this <tt>CreatePermissionRequestListener</tt>. If it is not
     * currently running, does nothing.
     */
    public void start()
    {
        if (!started)
        {
            stunStack.addRequestListener(this);
            started = true;
        }
    }

    /**
     * Stops this <tt>CreatePermissionRequestListenerr</tt>. A stopped
     * <tt>CreatePermissionRequestListenerr</tt> can be restarted by calling
     * {@link #start()} on it.
     */
    public void stop()
    {
        stunStack.removeRequestListener(this);
        started = false;
    }

}
