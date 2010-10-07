/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: SampMessageHandler.java,v 1.6 2010-10-07 14:59:56 bourgesl Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2010/10/05 14:52:31  bourgesl
 * removed SampException in several method signatures
 *
 * Revision 1.4  2010/10/05 10:17:56  bourgesl
 * fixed warnings / javadoc
 * fixed exception handling / logs
 * fixed member visibility
 *
 * Revision 1.3  2010/10/05 09:44:19  bourgesl
 * fixed warnings / javadoc
 *
 * Revision 1.2  2010/09/24 12:06:16  lafrasse
 * Added mType facilities and SampCapability support.
 *
 * Revision 1.1  2010/09/14 14:29:49  lafrasse
 * First SAMP manager implementation.
 *
 ******************************************************************************/
package fr.jmmc.mcs.interop;

import org.astrogrid.samp.client.AbstractMessageHandler;

/**
 * SampMessageHandler class.
 *
 * Conveniently provides automatic registration of your message handler.
 *
 * @author lafrasse
 */
public abstract class SampMessageHandler extends AbstractMessageHandler
{

    /* members */
    /** Store the handled mType */
    private String _mType = null;

    /**
     * Generic constructor taking its mType as a String (e.g for private message)
     * 
     * @param mType single MType which this handler can process
     */
    public SampMessageHandler(final String mType)
    {
        super(mType);

        _mType = mType;

        SampManager.registerCapability(this);
    }

    /** 
     * Dedicated constructor taking a public or private SAMP capability
     * @param capability public or private SAMP capability 
     */
    public SampMessageHandler(final SampCapability capability)
    {
        this(capability.mType());
    }

    /**
     * Return the currently handled mType as a String
     * @return the currently handled mType as a String 
     */
    public final String handledMType()
    {
        return _mType;
    }

    /*
     * TODO : implement default processCall implementation
     */
/*
    public Map processCall(final HubConnection c, final String senderId, final Message msg) {
      return null;
    }
 */
}
