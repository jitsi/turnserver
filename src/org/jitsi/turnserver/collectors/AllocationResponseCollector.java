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

package org.jitsi.turnserver.collectors;

import java.io.IOException;
import java.util.logging.*;

import org.ice4j.*;
import org.ice4j.attribute.*;
import org.ice4j.message.*;
import org.ice4j.stack.*;
import org.jitsi.turnserver.stack.TurnStack;

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
     * @param turnStack
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
            NonceAttribute nonceAttr = (NonceAttribute) message
		    .getAttribute(Attribute.NONCE);
//	    System.out.println("Nonce : "+new Nonce(nonceAttr.getNonce()));
            Request request = MessageFactory.createAllocateRequest();
            TransactionID tran = TransactionID.createNewTransactionID();
            try {
		request.setTransactionID(tran.getBytes());
	    } catch (StunException e1) {
		System.err.println("Unable to set tran ID.");
	    }
            request.putAttribute(nonceAttr);
            String username = "JitsiGsocStudent";
	    UsernameAttribute usernameAttr = AttributeFactory
		    .createUsernameAttribute(username+":");
/*	    
	    byte[] key = this.stunStack.getCredentialsManager().getLocalKey(
		    username);
	    System.out.println("Username found "
		    + (this.stunStack.getCredentialsManager()
			    .checkLocalUserName(username)));
	    System.out.println("User "
		    + username
		    + " found : "
		    + TurnStack.toHexString(key));
	    
	    byte[] messageB = request.encode(stunStack);
*/	    MessageIntegrityAttribute msgInt = AttributeFactory
		    .createMessageIntegrityAttribute(username);
	    RequestedTransportAttribute reqTrans = AttributeFactory
		    .createRequestedTransportAttribute(
			    RequestedTransportAttribute.UDP);
	    try{
//		    msgInt.encode(stunStack, messageB, 0, messageB.length);		
	    }catch(Exception e)
	    {
		e.printStackTrace();
	    }
	    request.putAttribute(reqTrans);
	    request.putAttribute(usernameAttr);
	    request.putAttribute(msgInt);
	    try {
		this.stunStack.sendRequest(request, evt.getRemoteAddress(),
		    evt.getLocalAddress(), this);
	    }
	    catch (Exception e) {
		e.printStackTrace();
//		System.err.println(e.getMessage());
	    }
            if(errorCodeAttribute != null)
            {
		System.out.println("Error Code : "
			+ (int) errorCodeAttribute.getErrorCode());
            }
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
            System.out.println("Allocate Sucess Response.");
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
