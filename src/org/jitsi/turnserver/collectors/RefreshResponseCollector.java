/**
 * 
 */
package org.jitsi.turnserver.collectors;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ice4j.ResponseCollector;
import org.ice4j.StunResponseEvent;
import org.ice4j.StunTimeoutEvent;
import org.ice4j.message.Message;
import org.ice4j.stack.StunStack;

/**
 * The class that would be handling and responding to incoming Refresh
 * responses.
 * 
 * @author Aakash Garg
 */
public class RefreshResponseCollector 
		implements ResponseCollector 
{
    /**
     * The <tt>Logger</tt> used by the <tt>RefreshresponseCollector</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
	    .getLogger(RefreshResponseCollector.class.getName());

    private final StunStack stunStack;

    /**
     * Creates a new RefreshResponseCollector
     * 
     * @param stunStack
     */
    public RefreshResponseCollector(StunStack stunStack) 
    {
	this.stunStack = stunStack;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ice4j.ResponseCollector#processResponse(org.ice4j.StunResponseEvent)
     */
    @Override
    public void processResponse(StunResponseEvent evt) 
    {
	if (logger.isLoggable(Level.FINER))
	{
		logger.finer("Received response " + evt);
	}
	Message message = evt.getMessage();
	if(message.getMessageType()== Message.REFRESH_ERROR_RESPONSE)
	{
	    //delete allocation
	}
	else if(message.getMessageType()==Message.REFRESH_RESPONSE)
	{
	    //update allocation
	}
	else
	{
	    return;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ice4j.ResponseCollector#processTimeout(org.ice4j.StunTimeoutEvent)
     */
    @Override
    public void processTimeout(StunTimeoutEvent event) 
    {

    }

}
