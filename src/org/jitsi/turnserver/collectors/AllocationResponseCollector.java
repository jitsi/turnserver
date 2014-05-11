/**
 * 
 */
package org.jitsi.turnserver.collectors;

import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;

/**
 * The class that would be handling to incoming Allocation responses.
 * 
 * @author Aakash Garg
 */
public class AllocationResponseCollector
    implements ResponseCollector
{
    /**
     * The <tt>Logger</tt> used by the <tt>AllocationresponseCollector</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger = Logger
        .getLogger(AllocationResponseCollector.class.getName());

    private final StunStack stunStack;

    /**
     * Creates a new AllocationresponseCollector
     * 
     * @param stunStack
     */
    public AllocationResponseCollector(StunStack stunStack)
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
        if (message.getMessageType() == Message.ALLOCATE_ERROR_RESPONSE)
        {
            ErrorCodeAttribute errorCodeAttribute =
                (ErrorCodeAttribute) message.getAttribute(Attribute.ERROR_CODE);
            switch (errorCodeAttribute.getErrorCode())
            {
            case ErrorCodeAttribute.BAD_REQUEST:
                // code for bad response error
                break;
            case ErrorCodeAttribute.UNAUTHORIZED:
                // code for unauthorised error code
                break;
            case ErrorCodeAttribute.FORBIDDEN:
                // code for forbidden error code
                break;
            case ErrorCodeAttribute.UNKNOWN_ATTRIBUTE:
                // code for Unknown Attribute error code
                break;
            case ErrorCodeAttribute.ALLOCATION_MISMATCH:
                // code for Allocation mismatch Error
                break;
            case ErrorCodeAttribute.STALE_NONCE:
                // code for Stale Nonce error code
                break;
            case ErrorCodeAttribute.WRONG_CREDENTIALS:
                // code for wrong credentials error code
                break;
            case ErrorCodeAttribute.UNSUPPORTED_TRANSPORT_PROTOCOL:
                // code for unsupported transport protocol
                break;
            case ErrorCodeAttribute.ALLOCATION_QUOTA_REACHED:
                // code for allocation quota reached
                break;
            case ErrorCodeAttribute.INSUFFICIENT_CAPACITY:
                // code for insufficient capacity
                break;
            }
        }
        else if (message.getMessageType() == Message.ALLOCATE_RESPONSE)
        {
            // code for doing processing of Allocation success response
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
