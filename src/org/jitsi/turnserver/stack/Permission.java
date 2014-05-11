package org.jitsi.turnserver.stack;

import org.ice4j.*;

/**
 * This class is an implementation of Permissions in TURN protocol.
 * 
 * @author Aakash Garg
 * 
 */
public class Permission
{
    /**
     * The maximum lifetime allowed for a Permission.
     */
    public static final long MAX_LIFETIME = 300 * 1000;

    /**
     * The IP address of the peer for which to create Permission.
     */
    private TransportAddress ipAddress;

    /**
     * Represents the current lifetime of the Permission will be decreased by
     * the PermissionExpireThread.
     */
    private long lifetime = -1;

    /**
     * Default Constructor.
     */
    public Permission()
    {
    }

    /**
     * @param ipAddress contains the peer IP address and transport protocol to
     *            be assigned. The port value is ignored.
     * @param lifetime the lifetime of permission.
     */
    public Permission(TransportAddress ipAddress, long lifetime)
    {
        this.setIpAddress(ipAddress);
        this.setLifetime(lifetime);
    }

    /**
     * @param ipAddress contains the peer IP address in String format.
     * @param lifetime the lifetime of permission.
     */
    public Permission(String ipAddress, long lifetime)
    {
        this.setIpAddress(ipAddress);
        this.setLifetime(lifetime);
    }

    /**
     * @return the ipAddress of the Permission as a TransportAddress.
     */
    public TransportAddress getIpAddress()
    {
        return ipAddress;
    }

    /**
     * @return the ipAddress as a String.
     */
    public String getIpAddressString()
    {
        return this.getIpAddress().getHostAddress();
    }

    /**
     * @param ipAddress the ipAddress of the peer for which to create
     *            Permission.
     */
    public void setIpAddress(TransportAddress ipAddress)
    {
        this.ipAddress =
            new TransportAddress(ipAddress.getHostAddress(), 0,
                ipAddress.getTransport());
    }

    /**
     * @param ipAddress the ipAddress as String of the peer for which to create
     *            Permission.
     */
    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = new TransportAddress(ipAddress, 0, Transport.UDP);
    }

    /**
     * @return the current lifetime of the permission.
     */
    public long getLifetime()
    {
        return lifetime;
    }

    /**
     * @param lifetime of permission. It is calculated as minimumm of lifetime
     *            and Permission.MAX_LIFETIME.
     */
    public void setLifetime(long lifetime)
    {
        this.lifetime = Math.min(lifetime, Permission.MAX_LIFETIME);
    }

    /*
     * The permission is uniquely identified by its IP address, so hashCode is
     * calculated on the IP address only.
     */
    @Override
    public int hashCode()
    {
        return ipAddress.getHostAddress().hashCode();
    }

    /*
     * Two Permissions are equal if their associated IP address, lifetime and
     * transport protocol are same.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof Permission))
        {
            return false;
        }
        Permission other = (Permission) obj;
        if (ipAddress == null)
        {
            if (other.ipAddress != null)
            {
                return false;
            }
        }
        else if (ipAddress.getHostAddress().compareTo(
            other.ipAddress.getHostAddress()) != 0)
        {
            return false;
        }
        if (lifetime != other.lifetime)
        {
            return false;
        }
        return true;
    }
}
