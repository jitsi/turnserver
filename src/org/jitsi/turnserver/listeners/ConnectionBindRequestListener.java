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

import java.io.*;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

import org.jitsi.turnserver.stack.*;

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

    private final TurnStack turnStack;

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
        this.turnStack = (TurnStack)stunStack;
    }

    @Override
    public void processRequest(StunMessageEvent evt)
        throws IllegalArgumentException
    {
        if (logger.isLoggable(Level.FINER))
        {
            logger.setLevel(Level.FINEST);
        }
        Message message = evt.getMessage();
        if (message.getMessageType() == Message.CONNECTION_BIND_REQUEST)
        {
            Response response = null;
            Character errorCode = null;
            
            TransportAddress clientAddress = evt.getRemoteAddress();
            TransportAddress serverAddress = evt.getLocalAddress();
            Transport transport = evt.getLocalAddress().getTransport();
            logger.finer("Received ConnectBind request " + evt + ", from "
                + clientAddress + ", at " + serverAddress + " over "
                + transport);
            ConnectionIdAttribute connectionId = null;
            if (transport != Transport.TCP)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
                logger.finest("Transport is not TCP.");
            }
            else if (!message.containsAttribute(Attribute.CONNECTION_ID))
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
                logger.finest("ConnectionID not found");
            }
            else
            {
                connectionId = (ConnectionIdAttribute) message
                    .getAttribute(Attribute.CONNECTION_ID);
                logger.finest("Requested ConnectionId - "
                    + connectionId.getConnectionIdValue());
                if (!this.turnStack.isUnacknowledged(connectionId
                    .getConnectionIdValue()))
                {
                    errorCode = ErrorCodeAttribute.BAD_REQUEST;
                    logger.finest("ConnectionId-"
                        + connectionId.getConnectionIdValue()
                        + " not present.");
                }
            }
            
            if (errorCode != null)
            {
                logger
                    .finest("Creating Connection Bind Error Response, errorCode:"
                        + (int) errorCode);
                response =
                    MessageFactory
                        .createConnectionBindErrorResponse(
                            ErrorCodeAttribute.BAD_REQUEST);
            }
            else
            {
                // processing logic.
                FiveTuple clientDataConnTuple =
                    new FiveTuple(clientAddress, serverAddress, transport);
                this.turnStack.acknowledgeConnectionId(
                    connectionId.getConnectionIdValue(), clientDataConnTuple);
                
                logger.finest("Creating ConnectionBind Success Response");
                response = MessageFactory.createConnectionBindResponse();
            }
            try
            {
                logger.finest("Sending ConnectionBind Response to "
                    + clientAddress + " through " + serverAddress);
                this.turnStack.sendResponse(
                    evt.getTransactionID().getBytes(), response, serverAddress,
                    clientAddress);
            }
            catch (StunException e)
            {
                logger.finest("Unable to send ConnectionBind Response to "
                    + clientAddress + " through " + serverAddress);
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
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
            turnStack.addRequestListener(this);
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
        turnStack.removeRequestListener(this);
        started = false;
    }
}
