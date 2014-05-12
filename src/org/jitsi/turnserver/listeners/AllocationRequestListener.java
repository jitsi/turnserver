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
 * The class that would be handling and responding to incoming Allocation
 * requests that are Allocation and sends a success or error response
 * 
 * @author Aakash Garg
 */
public class AllocationRequestListener
    implements RequestListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>AllocationRequestListener</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(AllocationRequestListener.class.getName());

    private final StunStack stunStack;

    /**
     * The indicator which determines whether this
     * <tt>AllocationrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new AllocationRequestListener
     * 
     * @param stunStack
     */
    public AllocationRequestListener(StunStack stunStack)
    {
        this.stunStack = stunStack;
    }

    @Override
    public void processRequest(StunMessageEvent evt)
        throws IllegalArgumentException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.finer("Received request " + evt);
        }
        Message message = evt.getMessage();
        if (message.getMessageType() == Message.ALLOCATE_REQUEST)
        {
            Response response = null;
            RequestedTransportAttribute requestedTransportAttribute =
                (RequestedTransportAttribute) message
                    .getAttribute(Attribute.REQUESTED_TRANSPORT);

            DontFragmentAttribute dontFragmentAttribute =
                (DontFragmentAttribute) message
                    .getAttribute(Attribute.DONT_FRAGMENT);

            ReservationTokenAttribute reservationTokenAttribute =
                (ReservationTokenAttribute) message
                    .getAttribute(Attribute.RESERVATION_TOKEN);

            EvenPortAttribute evenPortAttribute =
                (EvenPortAttribute) message.getAttribute(Attribute.EVEN_PORT);

            Character errorCode = null;
            if (requestedTransportAttribute == null)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
            }
            else if (requestedTransportAttribute.getRequestedTransport() != 17)
            {
                errorCode = ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
            }
            else if (reservationTokenAttribute != null
                && evenPortAttribute != null)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
            }
            // do other checks here
            else if (errorCode != null)
            {
                // do the allocation with 5-tuple
                TransportAddress mappedAddress = evt.getRemoteAddress();
                /*
                 * response = MessageFactory.createAllocationResponse(request,
                 * mappedAddress, relayedAddress, lifetime);
                 */
            }
            else
            {
                response =
                    MessageFactory.createAllocationErrorResponse(errorCode);
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
     * Starts this <tt>AllocationRequestListener</tt>. If it is not currently
     * running, does nothing.
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
     * Stops this <tt>AllocationRequestListenerr</tt>. A stopped
     * <tt>AllocationRequestListenerr</tt> can be restarted by calling
     * {@link #start()} on it.
     */
    public void stop()
    {
        stunStack.removeRequestListener(this);
        started = false;
    }
}
