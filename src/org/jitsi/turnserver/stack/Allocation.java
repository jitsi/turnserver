/*
 * TurnServer, the OpenSource Java Solution for TURN protocol. Maintained by the
 * Jitsi community (http://jitsi.org).
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package org.jitsi.turnserver.stack;

import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.ice4j.*;

/**
 * This class is an implementation of Allocations in TURN server.
 * 
 * @author Aakash Garg
 *
 */
public class Allocation
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(Allocation.class.getName());

    static InetAddress ipAddress;
    static int nextPortNo=5000;
    
    /**
     * represents the relay address associated with this Allocation.
     */
    private final TransportAddress relayAddress;
    
    /**
     * Represents the FiveTuple associated with this Allocation.
     */
    private final FiveTuple fiveTuple;
    
    /**
     * Represents the username associated with this Allocation.
     */
    private final String username;
    
    /**
     * represents the password associated with this Allocation.
     */
    private final String password;
    
    /**
     * The time in milliseconds when the Allocation will expire.
     */
    private long expirationTime = -1;

    /**
     * Determines whether or not the Allocation has expired.
     */
    private boolean expired = false;

    /**
     * The maximum lifetime allowed for a Permission.
     */
    public static final long MAX_LIFETIME = 10 * 60 * 1000;

    /**
     * Represents the set of permissions installed for this Allocation.
     */
    private final HashSet<Permission> permissions
        = new HashSet<Permission>();
    
    /**
     * Represents the Channel Bindings associated with this Allocation.
     */
    private final HashMap<Character,ChannelBind> channelBindings
        = new HashMap<Character,ChannelBind>();
    
    /**
     * Constructor to instantiate an Allocation without a username and password.
     * 
     * @param relayAddress the realyAddress associated with this Allocation.
     * @param fiveTuple the fiveTuple associated with this Allocation.
     */
    public Allocation(  TransportAddress relayAddress, 
                        FiveTuple fiveTuple)
    {
        this(relayAddress,fiveTuple,null,null);
    }

    /**
     * Constructor to instantiate an Allocation without a username and password
     * with the lifetime value.
     * 
     * @param relayAddress the realyAddress associated with this Allocation.
     * @param fiveTuple the fiveTuple associated with this Allocation.
     * @param lifetime the lifetime for this Allocation.
     */
    public Allocation(  TransportAddress relayAddress, 
                        FiveTuple fiveTuple, 
                        long lifetime)
    {
        this(relayAddress,fiveTuple,null,null,lifetime);
    }
    
    /**
     * Constructor to instantiate an Allocation with given relayAddress,
     * fiveTuple, username, passowrd and with default lifetime value.
     * 
     * @param relayAddress the realyAddress associated with this Allocation.
     * @param fiveTuple the fiveTuple associated with this Allocation.
     * @param username the username associated with this Allocation.
     * @param password the password associated with this Allocation.
     */
    public Allocation(  TransportAddress relayAddress, 
                        FiveTuple fiveTuple,
                        String username, 
                        String password)
    {
        this(relayAddress,fiveTuple,username,password,Allocation.MAX_LIFETIME);
    }

    /**
     * Constructor to instantiate an Allocation with given relayAddress,
     * fiveTuple, username, passowrd and with default lifetime value.
     * 
     * @param relayAddress the realyAddress associated with this Allocation.
     * @param fiveTuple the fiveTuple associated with this Allocation.
     * @param username the username associated with this Allocation.
     * @param password the password associated with this Allocation.
     * @param lifetime the lifetime for this allocation.
     */
    public Allocation(  TransportAddress relayAddress, 
                        FiveTuple fiveTuple,
                        String username, 
                        String password, 
                        long lifetime)
    {
        this.relayAddress = relayAddress;
        this.fiveTuple = fiveTuple;
        this.username = username;
        this.password = password;
        this.setLifetime(lifetime);
    }

    /**
     * returns the fiveTuple associated with this Allocation.
     */
    public FiveTuple getFiveTuple()
    {
        return this.fiveTuple;
    }
    
    /**
     * Returns the relayAddress associated with this Allocation.
     */
    public TransportAddress getRelayAddress()
    {
        return this.relayAddress;
    }
    
    /**
     * Returns the lifetime associated with this Allocation.
     * If the allocation is expired it returns 0. 
     */
    public long getLifetime()
    {
        if(!isExpired())
        {
            return (this.expirationTime-System.currentTimeMillis());
        }
        else
        {
            return 0;
        }
    }
    
    /**
     *  Sets the time to expire in milli-seconds for this allocation.
     *  Max lifetime can be Allocation.MAX_LIFEIME.
     *  
     *  @param lifetime the lifetime for this Allocation.
     */
    public void setLifetime(long lifetime)
    {
        synchronized(this)
        {
            this.expirationTime = System.currentTimeMillis()
                + Math.min(lifetime*1000, Allocation.MAX_LIFETIME);
        }
    }

    /**
     * Start the Allocation. This launches the countdown to the moment the
     * Allocation would expire.
     */
    public synchronized void start()
    {
        synchronized(this)
        {
            if (expirationTime == -1)
            {
                expired = false;
                expirationTime = MAX_LIFETIME + System.currentTimeMillis();
            }
            else
            {
                throw new IllegalStateException(
                        "Allocation has already been started!");
            }
        }
    }
    
    /**
     * Determines whether this <tt>Allocation</tt> is expired now.
     *
     * @return <tt>true</tt> if this <tt>Allocation</tT> is expired
     * now; otherwise, <tt>false</tt>
     */
    public boolean isExpired()
    {
        return isExpired(System.currentTimeMillis());
    }
    
    /**
     * Expires the Allocation. Once this method is called the Allocation is
     * considered terminated.
     */
    public synchronized void expire()
    {
        expired = true;
        /*
         * TurnStack has a background Thread running with the purpose of
         * removing expired Allocations.
         */
    }
    
    /**
     * Determines whether this <tt>Allocation</tt> will be expired at
     * a specific point in time.
     *
     * @param now the time in milliseconds at which the <tt>expired</tt> state
     * of this <tt>Allocation</tt> is to be returned
     * @return <tt>true</tt> if this <tt>Allocation</tt> will be
     * expired at the specified point in time; otherwise, <tt>false</tt>
     */
    public synchronized boolean isExpired(long now)
    {
        if (expirationTime == -1)
            return false;
        else if (expirationTime < now)
            return true;
        else
            return expired;
    }
    
    /**
     * Adds a new Permission for this Allocation.
     * @param permission the permisson to be added to this allocation.
     */
    public void addNewPermission(Permission permission)
    {
        this.permissions.add(permission);
    }
    
    /**
     * Binds a new Channel to this Allocation.
     * 
     * @param channelBind the channelBind to be added to this allocation.
     * @throws IllegalArgumentException if the channelNo of the channelBind to
     *             be added is already occupied.
     */
    public void bindNewChannel(ChannelBind channelBind)
    {
        if(this.channelBindings.containsKey(channelBind.getChannelNo()))
        {
            throw new IllegalArgumentException("Channel already occuied!");
        }
        else
        {
            synchronized(this.channelBindings)
            {
                this.channelBindings.put(   channelBind.getChannelNo(),
                                            channelBind);
            }
        }
    }
    
    /**
     * Removes the channelBind associated with this channlNo from this allocation.
     * 
     * @param channelNo the channelNo for which the ChannelBind to delete.
     * @return the ChannnelBindingf associated with this channelNo.
     */
    public ChannelBind removeChannelBinding(char channelNo)
    {
        ChannelBind channelBind = null;
        synchronized(this.channelBindings)
        {
            channelBind = this.channelBindings.remove(channelNo);
        }
        return channelBind;
    }
    
    @Override
    public int hashCode()
    {
        return this.fiveTuple.hashCode();
    }

    /**
     * Since an Allocation is uniquely identified by its relay address or five
     * tuple hence we only compare these members.
     */
    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof Allocation))
        {
            return false;
        }
        Allocation allocation = (Allocation) o;
        if(!this.fiveTuple.equals(allocation.fiveTuple))
        {
            return false;
        }
        if(!this.relayAddress.equals(allocation.relayAddress))
        {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString()
    {
        return this.getRelayAddress().toString();
    }
    
    public static TransportAddress getNewRelayAddress()
    {
        try
        {
            Allocation.ipAddress = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return (new TransportAddress(ipAddress,nextPortNo++,Transport.UDP));
    }
}
