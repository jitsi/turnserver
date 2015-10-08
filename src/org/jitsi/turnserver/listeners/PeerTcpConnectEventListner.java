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
