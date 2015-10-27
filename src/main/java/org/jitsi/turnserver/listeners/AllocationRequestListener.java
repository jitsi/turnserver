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

import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;
import org.jitsi.turnserver.stack.*;

/**
 * The class that would be handling and responding to incoming Allocation
 * requests that are Allocation and sends a success or error response.
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

    private final TurnStack turnStack;

    /**
     * The indicator which determines whether this
     * <tt>AllocationrequestListener</tt> is currently started.
     */
    private boolean started = false;

    /**
     * Creates a new AllocationRequestListener
     * 
     * @param turnStack
     */
    public AllocationRequestListener(StunStack stunStack)
    {
        if (stunStack instanceof TurnStack)
        {
            this.turnStack = (TurnStack) stunStack;
        }
        else
        {
            throw new IllegalArgumentException("This is not a TurnStack!");
        }
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
        if (message.getMessageType() == Message.ALLOCATE_REQUEST)
        {
	    logger.finest("Received a Allocation Request from "
		    + evt.getRemoteAddress());
           
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

            LifetimeAttribute lifetimeAttribute = 
                (LifetimeAttribute) message.getAttribute(Attribute.LIFETIME);
            
            EvenPortAttribute evenPort = 
                (EvenPortAttribute) message.getAttribute(Attribute.EVEN_PORT);
            
            if (lifetimeAttribute == null)
            {
                lifetimeAttribute =
                    AttributeFactory
                        .createLifetimeAttribute(
                            (int) (Allocation.DEFAULT_LIFETIME / 1000));
            }
            
            EvenPortAttribute evenPortAttribute =
                (EvenPortAttribute) message.getAttribute(Attribute.EVEN_PORT);
            
            TransportAddress clientAddress = evt.getRemoteAddress();
            TransportAddress serverAddress = evt.getLocalAddress();
            Transport transport = serverAddress.getTransport();
            FiveTuple fiveTuple =
                new FiveTuple(clientAddress, serverAddress, transport);
              
            Character errorCode = null;
            if(!this.turnStack.canHaveMoreAllocations())
            {
                errorCode = ErrorCodeAttribute.ALLOCATION_QUOTA_REACHED;
            }
            else if (requestedTransportAttribute == null)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
            }
            else if (requestedTransportAttribute.getRequestedTransport() 
                == RequestedTransportAttribute.TCP)
            {
                if (!this.turnStack.isTCPAllowed())
                    errorCode =
                        ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
                else if (reservationTokenAttribute != null)
                {
                    logger.finest("error : reservation token found in TCP message.");
                    errorCode =
                        ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
                }
                else if (evenPort != null)
                {
                    logger.finest("error : even port found in TCP message.");
                    errorCode =
                        ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
                }
                else if (dontFragmentAttribute != null)
                {
                    logger.finest("error : dont fragment found in TCP message.");
                    errorCode =
                        ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
                }
            }
            else if (requestedTransportAttribute.getRequestedTransport() 
                == RequestedTransportAttribute.UDP 
                && !this.turnStack.isUDPAllowed())
            {
                errorCode = ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL;
                logger.finest("UDP not alllowed on Allocation Requests.");
            }
            else if (reservationTokenAttribute != null
                && evenPortAttribute != null)
            {
                errorCode = ErrorCodeAttribute.BAD_REQUEST;
                logger
                    .finest("Both reservation Token and Even PortAttribute are found in Allocation request.");
            }
            
            if (turnStack.getServerAllocation(fiveTuple)!=null)
            {
                errorCode = ErrorCodeAttribute.ALLOCATION_MISMATCH; 
                logger.finest("Allocation not found for the "+fiveTuple);
            }
            // do other checks here
            
            if (errorCode == null)
            {
                if(evenPortAttribute==null)
                {
                    evenPortAttribute =
                        AttributeFactory.createEvenPortAttribute(false);
                }
                TransportAddress relayAddress = turnStack.getNewRelayAddress(
                    evenPortAttribute.isRFlag(), serverAddress.getTransport());
/*                logger.finest("Added a new Relay Address "+relayAddress);
                System.out.println("Added a new Relay Address "+relayAddress
                	+" for client "+evt.getRemoteAddress());
*/              
                Allocation allocation = null;
                synchronized(this)
                {
                    allocation =
                        new Allocation(relayAddress, fiveTuple,
                            lifetimeAttribute.getLifetime());
                    this.turnStack.addNewServerAllocation(allocation);
//                    System.out.println("Added a new allocation.");
                }
                logger.finest("Added a new Allocation with relay address :"
				+ allocation.getRelayAddress()+" for client "
				+evt.getRemoteAddress());
                
                response = MessageFactory.createAllocationResponse(
                     (Request) message,
                     allocation.getFiveTuple().getClientTransportAddress(), 
                     allocation.getRelayAddress(), 
                     (int) allocation.getLifetime());
                
                XorRelayedAddressAttribute relayedXorAddress 
                    = AttributeFactory.createXorRelayedAddressAttribute(
                            allocation.getRelayAddress(),
                            evt.getTransactionID().getBytes());
                response.putAttribute(relayedXorAddress);
                
                LifetimeAttribute lifetime 
                    = AttributeFactory.createLifetimeAttribute(
                        (int)(allocation.getLifetime()));
                response.putAttribute(lifetime);
                
                XorMappedAddressAttribute clientXorAddress 
                    = AttributeFactory.createXorMappedAddressAttribute(
                        clientAddress,
                        evt.getTransactionID().getBytes());
               response.putAttribute(clientXorAddress);
                
                if(evenPort!=null)
                {
                    // TODO : logic for process and creating Reservation Token.
                    byte[] token = {7,7,7,7};
                    ReservationTokenAttribute reservationToken
                        = AttributeFactory.createReservationTokenAttribute(
                            token);
                    response.putAttribute(reservationToken);
                    if(evenPort.isRFlag())
                    {
                        TransportAddress relayAddess
                            = allocation.getRelayAddress();
                        TransportAddress nextAddress 
                            = new TransportAddress(
                                relayAddress.getAddress(),
                                relayAddress.getPort()+1,
                                relayAddress.getTransport());
                        boolean isReserved
                            = this.turnStack.reservePort(nextAddress);
                        if(isReserved)
                        {
                            logger.log(
                                Level.FINEST,
                                nextAddress+" reserved by "+fiveTuple);
                        }
                        else
                        {
                            logger.log(
                                Level.FINEST,
                                nextAddress+" not reserved by "+fiveTuple);
                        }
                    }
                }               
            }
            else
            {
                System.err.println("Error Code " + (int)errorCode
                        + " on Allocation Request");
                logger.finest("Error Code " + (int)errorCode
                    + " on Allocation Request");
                response =
                    MessageFactory.createAllocationErrorResponse(errorCode);
            }
            
            try
            {
                logger
                    .fine("Trying to send response to "
                        + evt.getRemoteAddress() + " from "
                        + evt.getLocalAddress());
                turnStack.sendResponse(
                    evt.getTransactionID().getBytes(), response,
                    evt.getLocalAddress(), evt.getRemoteAddress());
                logger.finest("Response sent.");
            }
            catch (Exception e)
            {
        	System.err.println("Failed to send response");
                logger.log(
                    Level.INFO, "Failed to send " + response + " through "
                        + evt.getLocalAddress(), e);
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
            turnStack.addRequestListener(this);
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
        turnStack.removeRequestListener(this);
        started = false;
    }
}
