package mcast.ht.bittorrent;

/**
 * @author mathijs
 */
public interface InterestListener {

    public void interested(BitTorrentConnection connection);

    public void notInterested(BitTorrentConnection connection);

}
