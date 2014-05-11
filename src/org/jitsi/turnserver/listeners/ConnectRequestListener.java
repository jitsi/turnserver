/**
 * 
 */
package org.jitsi.turnserver.listeners;

import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

/**
 * The class that would be handling and responding to incoming Connect requests
 * that are validated and sends a success or error response
 * 
 * @author Aakash Garg
 */
public class ConnectRequestListener
    implements RequestListener
{

    /**
     * The <tt>Logger</tt> used by the <tt>ConnectRequestListener</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(ConnectRequestListener.class.getName());

    private final StunStack stunStack;

    /**
     * The indicator which determines whether this
     * <tt>ValidatedrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new ConnectRequestListener
     * 
     * @param stunStack
     */
    public ConnectRequestListener(StunStack stunStack)
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
        if (message.getMessageType() == message.CONNECT_REQUEST)
        {
            XorPeerAddressAttribute xorPeerAddressAttribute =
                (XorPeerAddressAttribute) message
                    .getAttribute(Attribute.XOR_PEER_ADDRESS);
            // code for processing the connect request.
        }
        else
        {
            return;
        }

    }

    /**
     * Starts this <tt>ConnectRequestListener</tt>. If it is not currently
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
     * Stops this <tt>ConnectRequestListenerr</tt>. A stopped
     * <tt>ConnectRequestListenerr</tt> can be restarted by calling
     * {@link #start()} on it.
     */
    public void stop()
    {
        stunStack.removeRequestListener(this);
        started = false;
    }

}
