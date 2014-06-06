/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.stack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

/**
 * The entry point to the TurnServer stack. The class is used to start, stop and
 * configure the stack.
 * 
 * @author Aakash Garg
 */
public class TurnStack
    extends StunStack
{

    /**
     * The <tt>Logger</tt> used by the <tt>turnStack</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger 
        = Logger.getLogger(TurnStack.class.getName());

    /**
     * Represents the Allocations stored for Server Side.
     */
    private final HashMap<FiveTuple,Allocation> serverAllocations
        = new HashMap<FiveTuple,Allocation>();

    /**
     * Represents the Allocations stored for Client Side.
     */
    private final HashMap<FiveTuple,Allocation> clientAllocations
        = new HashMap<FiveTuple,Allocation>();

    /**
     * The <tt>Thread</tt> which expires the <tt>TurnServerAllocation</tt>s of
     * this <tt>TurnStack</tt> and removes them from {@link #serverAllocations}
     * .
     */
    private Thread serverAllocationExpireThread;
    
    /**
     * Default Constructor. Initialises the NetAccessManager and
     */
    public TurnStack()
    {
        super();
    }

    /**
     * Called to notify this provider for an incoming message. method overridden
     * to modify the logic of the Turn Stack.
     * 
     * @param ev the event object that contains the new message.
     */
    @Override
    public void handleMessageEvent(StunMessageEvent ev)
    {
        Message msg = ev.getMessage();
        System.out.println("Received an event");
/*
        if (!TurnStack.isTurnMessage(msg))
            return; // oops not a Turn message.
   */     logger.setLevel(Level.FINEST);
        if (logger.isLoggable(Level.FINEST))
        {
            logger.finest("Received a message on " + ev.getLocalAddress()
                + " of type:" + (int) msg.getMessageType());
        }

        // request
        if (msg instanceof Request)
        {
            logger.finest("parsing request");

            TransactionID serverTid = ev.getTransactionID();
            TurnServerTransaction sTran =
                (TurnServerTransaction) getServerTransaction(serverTid);

            if (sTran != null)
            {
                // requests from this transaction have already been seen
                // retransmit the response if there was any
                logger.finest("found an existing transaction");

                try
                {
                    sTran.retransmitResponse();
                    logger.finest("Response retransmitted");
                }
                catch (Exception ex)
                {
                    // we couldn't really do anything here .. apart from logging
                    logger.log(
                        Level.WARNING, "Failed to retransmit a Turn response",
                        ex);
                }

                if (!Boolean
                    .getBoolean(StackProperties.PROPAGATE_RECEIVED_RETRANSMISSIONS))
                {
                    return;
                }
            }
            else
            {
                logger.finest("existing transaction not found");
                sTran =
                    new TurnServerTransaction(this, serverTid,
                        ev.getLocalAddress(), ev.getRemoteAddress());

                // if there is an OOM error here, it will lead to
                // NetAccessManager.handleFatalError that will stop the
                // MessageProcessor thread and restart it that will lead again
                // to an OOM error and so on... So stop here right now
                try
                {
                    sTran.start();
                }
                catch (OutOfMemoryError t)
                {
                    logger.info("Turn transaction thread start failed:" + t);
                    return;
                }
                startNewServerTransactionThread(
                    serverTid, sTran);
            }

            // validate attributes that need validation.
            try
            {
                validateRequestAttributes(ev);
            }
            catch (Exception exc)
            {
                // validation failed. log get lost.
                logger.log(
                    Level.FINE, "Failed to validate msg: " + ev, exc);
                return;
            }

            try
            {
                fireMessageEventFormEventDispatcher(ev);
            }
            catch (Throwable t)
            {
                Response error;

                logger.log(
                    Level.INFO, "Received an invalid request.", t);
                Throwable cause = t.getCause();

                if (((t instanceof StunException) && ((StunException) t)
                    .getID() == StunException.TRANSACTION_ALREADY_ANSWERED)
                    || ((cause instanceof StunException) && ((StunException) cause)
                        .getID() == StunException.TRANSACTION_ALREADY_ANSWERED))
                {
                    // do not try to send an error response since we will
                    // get another TRANSACTION_ALREADY_ANSWERED
                    return;
                }

                if (t instanceof IllegalArgumentException)
                {
                    error = MessageFactory.createBindingErrorResponse(
                        ErrorCodeAttribute.BAD_REQUEST, t.getMessage());
                }
                else
                {
                    error =
                        MessageFactory.createBindingErrorResponse(
                            ErrorCodeAttribute.SERVER_ERROR,
                            "Oops! Something went wrong on our side :(");
                }

                try
                {
                    sendResponse(
                        serverTid.getBytes(), error, ev.getLocalAddress(),
                        ev.getRemoteAddress());
                }
                catch (Exception exc)
                {
                    logger.log(
                        Level.FINE, "Couldn't send a server error response",
                        exc);
                }
            }
        }
        // response
        else if (msg instanceof Response)
        {
            TransactionID tid = ev.getTransactionID();
            StunClientTransaction tran =
                removeTransactionFromClientTransactions(tid);
            if (tran != null)
            {
                tran.handleResponse(ev);
            }
            else
            {
                // do nothing - just drop the phantom response.
                logger
                    .fine("Dropped response - no matching client tran found for"
                        + " tid " + tid + "\n");
            }
        }
        // indication
        else if (msg instanceof Indication)
        {
            fireMessageEventFormEventDispatcher(ev);
        }
    }

    private void validateRequestAttributes(StunMessageEvent ev)
    {
        
    }

    /**
     * Returns the Allocation with the specified <tt>fiveTuple</tt> or
     * <tt>null</tt> if no such Allocation exists.
     * 
     * @param fiveTuple the fiveTuple of the Allocation we are looking for.
     * 
     * @return the {@link Allocation} we are looking for.
     */

    public Allocation getServerAllocation(FiveTuple fiveTuple)
    {
        Allocation allocation = null;

        synchronized (this.serverAllocations)
        {
            allocation = this.serverAllocations.get(fiveTuple);
        }
        /*
         * If a Allocation is expired, do not return it. It will be
         * removed from serverAllocations soon.
         */
        if ((allocation != null) && allocation.isExpired())
            allocation = null;
        return allocation;
    }
    
    /**
     * Returns the Allocation with the specified <tt>fiveTuple</tt> or
     * <tt>null</tt> if no such Allocation exists.
     * 
     * @param fiveTuple the fiveTuple of the Allocation we are looking for.
     * 
     * @return the {@link Allocation} we are looking for.
     */

    public Allocation getClientAllocation(FiveTuple fiveTuple)
    {
        Allocation allocation;

        synchronized (clientAllocations)
        {
            allocation = clientAllocations.get(fiveTuple);
        }
        /*
         * If a Allocation is expired, do not return it. It will be
         * removed from serverAllocations soon.
         */
        if ((allocation != null) && allocation.isExpired())
            allocation = null;
        return allocation;
    }

    /**
     * Adds a new server allocation to this TurnStack.
     * 
     * @param allocation the allocation to be added to this TurnStack.
     */
    public void addNewServerAllocation(Allocation allocation)
    {
        synchronized(this.serverAllocations)
        {
            this.serverAllocations.put(allocation.getFiveTuple(), allocation);
            if(allocation.isExpired())
            {   // check if meanwhile other thread has put the same allocation.
                allocation.start();
            }
            maybeStartServerAllocationExpireThread();
        }
    }
    
    /**
     * Initialises and starts {@link #serverTransactionExpireThread} if
     * necessary.
     */
    public void maybeStartServerAllocationExpireThread()
    {
        synchronized (serverAllocations)
        {
            if (!serverAllocations.isEmpty()
                && (serverAllocationExpireThread == null))
            {
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        runInServerAllocationExpireThread();
                    }
                };

                t.setDaemon(true);
                t.setName(getClass().getName()
                    + ".serverAllocationExpireThread");

                boolean started = false;

                serverAllocationExpireThread = t;
                try
                {
                    t.start();
                    started = true;
                }
                finally
                {
                    if (!started && (serverAllocationExpireThread == t))
                        serverAllocationExpireThread = null;
                }
            }
        }
    }

    /**
     * Runs in {@link #serverAllocationExpireThread} and expires the
     * <tt>Allocation</tt>s of this <tt>TurnStack</tt> and removes
     * them from {@link #serverAllocations}.
     */
    private void runInServerAllocationExpireThread()
    {
        try
        {
            long idleStartTime = -1;

            do
            {
                synchronized (serverAllocations)
                {
                    try
                    {
                        serverAllocations.wait(Allocation.MAX_LIFETIME);
                    }
                    catch (InterruptedException ie)
                    {
                    }

                    /*
                     * Is the current Thread still designated to expire the
                     * Allocations of this TurnStack?
                     */
                    if (Thread.currentThread() != serverAllocationExpireThread)
                        break;

                    long now = System.currentTimeMillis();

                    /*
                     * Has the current Thread been idle long enough to merit
                     * disposing of it?
                     */
                    if (serverAllocations.isEmpty())
                    {
                        if (idleStartTime == -1)
                            idleStartTime = now;
                        else if (now - idleStartTime > 60 * 1000)
                            break;
                    }
                    else
                    {
                        // Expire the Allocations of this TurnStack.

                        idleStartTime = -1;

                        for (Iterator<Allocation> i =
                            serverAllocations.values().iterator(); i.hasNext();)
                        {
                            Allocation allocation = i.next();

                            if (allocation == null)
                            {
                                i.remove();
                            }
                            else if (allocation.isExpired(now))
                            {
                                System.out.println("allocation "+allocation+" expired");
                                i.remove();
                                allocation.expire();
                            }
                        }
                    }
                }
            }
            while (true);
        }
        finally
        {
            synchronized (serverAllocations)
            {
                if (serverAllocationExpireThread == Thread.currentThread())
                    serverAllocationExpireThread = null;
                /*
                 * If serverAllocationExpireThread dies unexpectedly and yet it
                 * is still necessary, resurrect it.
                 */
                if (serverAllocationExpireThread == null)
                    maybeStartServerAllocationExpireThread();
            }
        }
    }

    
    /**
     * Method to check if the given message method is of Turn method.
     * 
     * @param message
     * @return true if message is of Turn method else false.
     */
    public static boolean isTurnMessage(Message message)
    {
        char method = message.getMessageType();
        method = (char) (method & 0xfeef); // ignore the class
        boolean isTurnMessage = false;
        switch (method)
        {
        // Turn Specific Methods
        case Message.TURN_METHOD_ALLOCATE:
        case Message.TURN_METHOD_CHANNELBIND:
        case Message.TURN_METHOD_CREATEPERMISSION:
        case Message.TURN_METHOD_DATA:
        case Message.TURN_METHOD_REFRESH:
        case Message.TURN_METHOD_SEND:
            // Turn TCP support Methods
        case Message.TURN_METHOD_CONNECT:
        case Message.TURN_METHOD_CONNECTION_BIND:
        case Message.TURN_METHOD_CONNECTION_ATTEMPT:
            isTurnMessage = true;
        default:
            isTurnMessage = false;
        }
        return isTurnMessage;

    }

}
