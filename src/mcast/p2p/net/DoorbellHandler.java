package mcast.p2p.net;

import ibis.ipl.IbisIdentifier;

public interface DoorbellHandler {

    /**
     * Answers the doorbell of this host.
     * 
     * @param applicant
     *                the host that rang the doorbell
     * @return the value that the applicant's ring method will return.
     */
    public boolean answerDoorbell(IbisIdentifier applicant);

}
