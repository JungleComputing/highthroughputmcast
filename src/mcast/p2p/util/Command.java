package mcast.p2p.util;

import java.io.IOException;

/**
 * @author mathijs
 */
public interface Command {

    public void execute() throws IOException;

}
