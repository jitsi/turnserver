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
 * The class that would be handling and responding to incoming Refresh requests that are
 * validated and sends a success or error response
 * 
 * @author Aakash Garg
 */
public class RefreshRequestListener 
		implements RequestListener 
{
	/**
	 * The <tt>Logger</tt> used by the <tt>RefreshRequestListener</tt> class
	 * and its instances for logging output.
	 */
	private static final Logger logger 
		= Logger.getLogger(RefreshRequestListener.class.getName());

	private final StunStack stunStack;

	/**
	 * The indicator which determines whether this
	 * <tt>RefreshRequestListener</tt> is currently started.
	 */
	private boolean started = false;

	/**
	 * Creates a new RefreshRequestListener
	 * 
	 * @param stunStack
	 */
	public RefreshRequestListener(StunStack stunStack) 
	{
		this.stunStack = stunStack;
	}

	/** (non-Javadoc)
         * @see org.ice4j.stack.RequestListener#processRequest(org.ice4j.StunMessageEvent)
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
            if(message.getMessageType()== Message.REFRESH_REQUEST)
            {
        	LifetimeAttribute lifetimeAttribute = 
        		(LifetimeAttribute) message.getAttribute(Attribute.LIFETIME);
        	int desiredLifetime = 0;
/*        	desiredLifetime = Math.min(lifetimeAttribute.getLifetime(), 
        					stunstack.DEFAULT_LIFETIME);
 */
        	Response response = null;
        	boolean allocationFound = false;
        	if(allocationFound)
        	{
        	    if(desiredLifetime==0)
        	    {
        		//delete allocation
        	    }
        	    else
        	    {
        		//update allocation's time-to-expire
        	    }
        	    response = MessageFactory.createRefreshResponse(desiredLifetime);
        	}
        	else
        	{
        	    	response = MessageFactory
        	    		.createRefreshErrorResponse(ErrorCodeAttribute.ALLOCATION_MISMATCH);
        	}
        	try 
        	{
        	    stunStack.sendResponse(evt.getTransactionID().getBytes(),
        		    response, evt.getLocalAddress(),
        		    evt.getRemoteAddress());
        	} catch (Exception e) 
        	{
        	    logger.log(Level.INFO, "Failed to send " 
        		    + response
        		    + " through " 
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
        * Starts this <tt>RefreshRequestListener</tt>. If it is not currently
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
	 * Stops this <tt>RefreshRequestListenerr</tt>. A stopped
	 * <tt>ValidatedRequestListenerr</tt> can be restarted by calling
	 * {@link #start()} on it.
	 */
	public void stop() 
	{
		stunStack.removeRequestListener(this);
		started = false;
	}
}
