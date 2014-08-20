/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.socket;

/**
 * Represents the TCP Connect Event Listener.
 * 
 * @author Aakash Garg.
 * 
 */
public interface TcpConnectEventListener
{
    public void onConnect(TcpConnectEvent event);
    
}
