/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ice4j.StunMessageEvent;
import org.ice4j.message.*;
import org.ice4j.stack.*;

/**
 * The class that would be handling and responding to incoming ConnectionBind
 * requests that are validated and sends a success or error response
 * 
 * @author Aakash Garg
 */
public class ConnectionBindRequestListener
    implements RequestListener
{

    /**
     * The <tt>Logger</tt> used by the <tt>ConnectionBindRequestListener</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(ConnectionBindRequestListener.class.getName());

    private final StunStack stunStack;

    /**
     * The indicator which determines whether this
     * <tt>ConnectionBindrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new ConnectionBindRequestListener
     * 
     * @param turnStack
     */
    public ConnectionBindRequestListener(StunStack stunStack)
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
        if (message.getMessageType() == Message.CONNECTION_BIND_REQUEST)
        {
            Response response = null;
            // processing logic
        }
        else
        {
            return;
        }
    }

    /**
     * Starts this <tt>ConnectionBindRequestListener</tt>. If it is not
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
     * Stops this <tt>ConnectionBindRequestListenerr</tt>. A stopped
     * <tt>ConnectionBindRequestListenerr</tt> can be restarted by calling
     * {@link #start()} on it.
     */
    public void stop()
    {
        stunStack.removeRequestListener(this);
        started = false;
    }

}
