/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.listeners;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ice4j.StunException;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

import org.jitsi.turnserver.socket.*;
import org.jitsi.turnserver.stack.*;

/**
 * Class to handle events when Peer tries to establish a TCP connection to a
 * Server Socket (generally Relay Address).
 * 
 * @author Aakash Garg
 * 
 */
public class PeerTcpConnectEventListner
    implements TcpConnectEventListener
{

    /**
     * The <tt>Logger</tt> used by the <tt>FiveTuple</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(PeerTcpConnectEventListner.class.getName());

    private final TurnStack turnStack;

    public PeerTcpConnectEventListner(TurnStack turnStack)
    {
        this.turnStack = turnStack;
    }

    @Override
    public void onConnect(TcpConnectEvent event)
    {
        logger.setLevel(Level.FINEST);
        logger.finest("Received a connect event src:" + event.getLocalAdress()
            + ", dest:" + event.getRemoteAdress());
        Allocation allocation =
            this.turnStack.getServerAllocation(event.getLocalAdress());
        if (allocation == null)
        {
            logger.finest("Allocation not found for relay : "
                + event.getLocalAdress());
        }
        else if (allocation.isPermitted(event.getRemoteAdress()))
        {
            try
            {
                ConnectionIdAttribute connectionId =
                    AttributeFactory.createConnectionIdAttribute();
                logger.finest("Created ConnectionId - "
                    + connectionId.getConnectionIdValue() + " for client "
                    + allocation.getClientAddress());
                TransactionID tranID = TransactionID.createNewTransactionID();
                Indication connectionAttemptIndication =
                    MessageFactory.createConnectionAttemptIndication(
                        connectionId.getConnectionIdValue(),
                        event.getRemoteAdress(), tranID.getBytes());
                this.turnStack.addUnAcknowlededConnectionId(
                    connectionId.getConnectionIdValue(),
                    event.getRemoteAdress(), allocation);
                logger.finest("Sending Connection Attempt Indication.");
                this.turnStack.sendIndication(
                    connectionAttemptIndication, allocation.getClientAddress(),
                    allocation.getServerAddress());
            }
            catch (StunException e)
            {
                logger
                    .finest("Unable to send Connection Attempt Indiacation to "
                        + allocation.getClientAddress());
            }
        }
        else
        {
            logger.finest("permission not installed for - "
                + event.getRemoteAdress());
        }
        // this.turnStack.add
    }

}
