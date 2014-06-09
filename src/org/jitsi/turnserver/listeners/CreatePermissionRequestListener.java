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
import org.jitsi.turnserver.stack.*;

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

    private final TurnStack turnStack;

    /**
     * The indicator which determines whether this
     * <tt>CreatePermissionrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new CreatePermissionRequestListener
     * 
     * @param turnStack
     */
    public CreatePermissionRequestListener(StunStack turnStack)
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
            logger.setLevel(Level.FINEST);
            logger.finer("Received create permission request " + evt);
        }
        Message message = evt.getMessage();
        if (message.getMessageType() == Message.CREATEPERMISSION_REQUEST)
        {
            System.out.println("Received create permission request ");
            System.out.println("Event tran : "+evt.getTransactionID());
            
            XorPeerAddressAttribute xorPeerAddressAttribute =
                (XorPeerAddressAttribute) message
                    .getAttribute(Attribute.XOR_PEER_ADDRESS);
            if(xorPeerAddressAttribute!=null)
            {
                xorPeerAddressAttribute.setAddress(
                    xorPeerAddressAttribute.getAddress(), 
                    evt.getTransactionID().getBytes());
            }
            // we should get multiple xor peer address attributes here.
            LifetimeAttribute lifetimeAttribute = 
                (LifetimeAttribute) message.getAttribute(Attribute.LIFETIME);
            
            Response response = null;
            TransportAddress clientAddress = evt.getRemoteAddress();
            TransportAddress serverAddress = evt.getLocalAddress();
            Transport transport = Transport.UDP;
            FiveTuple fiveTuple =
                new FiveTuple(clientAddress, serverAddress, transport);
            
            Allocation allocation 
                = this.turnStack.getServerAllocation(fiveTuple);
            
            Character errorCode = null;
            if (xorPeerAddressAttribute == null || allocation==null)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
            }
            else if (!TurnStack.isIPAllowed(
                        xorPeerAddressAttribute.getAddress()))
            {
                System.out.println("Peer Address requested " 
                        + xorPeerAddressAttribute.getAddress()
                        +" "+TurnStack.isIPAllowed(
                            xorPeerAddressAttribute.getAddress()));
                errorCode = ErrorCodeAttribute.FORBIDDEN;
            }
            else if (!allocation.canHaveMorePermisions())
            {
                errorCode = ErrorCodeAttribute.INSUFFICIENT_CAPACITY;
            }
            if(errorCode!=null)
            {
                System.out.println("Creating error response : "+(int)errorCode);
                response = MessageFactory.createCreatePermissionErrorResponse(
                                errorCode);
            }
            else
            {   
                System.out.println("Creating success response.");
                TransportAddress peerAddress 
                    = xorPeerAddressAttribute.getAddress();
                Permission permission = null;
                if(lifetimeAttribute!=null)
                {
                     permission = new Permission(peerAddress,
                                         lifetimeAttribute.getLifetime());
                }
                else
                {
                    permission = new Permission(peerAddress);
                }
                
                System.out.println("Peer Address requested " 
                    + xorPeerAddressAttribute.getAddress()
                    +" "+TurnStack.isIPAllowed(
                        xorPeerAddressAttribute.getAddress()));
                allocation.addNewPermission(permission);
                System.out.println("Added permission to allocation.");
                response = MessageFactory.createPermissionResponse();
            }
            try
            {
                turnStack.sendResponse(evt.getTransactionID().getBytes(),
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
            turnStack.addRequestListener(this);
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
        turnStack.removeRequestListener(this);
        started = false;
    }

}
