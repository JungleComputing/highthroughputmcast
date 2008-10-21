package mcast.p2p;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.Location;

import mcast.ht.Collective;
import mcast.ht.LocationPool;

import org.junit.Before;
import org.junit.Test;

public class LocationPoolTest {

    private IbisIdentifier[] ibises;
    private LocationPool pool;
    
    @Before
    public void setUp() throws Exception {
        ibises = new IbisIdentifier[5];
        ibises[0] = new DummyIbisIdentifier("pool", new Location("node001@das3@cs.vu.nl"));
        ibises[1] = new DummyIbisIdentifier("pool", new Location("node321@das3@tudelft@nl"));
        ibises[2] = new DummyIbisIdentifier("pool", new Location("node311@das3@tudelft@nl"));
        ibises[3] = new DummyIbisIdentifier("pool", new Location("node011@das3@cs.vu.nl"));
        ibises[4] = new DummyIbisIdentifier("pool", new Location("node080@das3@cs.vu.nl"));
        
        pool = new LocationPool("pool", ibises);
    }

    @Test
    public void testGetAllCollectives() {
        Collection<Collective> all = pool.getAllCollectives(); 
        assertEquals(2, all.size());
    }

    @Test
    public void testGetCollective() {
        Collective vu = pool.getCollective(ibises[3]);
        List<IbisIdentifier> vuMembers = vu.getMembers();
        assertEquals(3, vuMembers.size());

        Collective delft = pool.getCollective(ibises[2]);
        List<IbisIdentifier> delftMembers = delft.getMembers();
        assertEquals(2, delftMembers.size());
    }

    @Test
    public void testGetEverybody() {
        List<IbisIdentifier> everybody = pool.getEverybody();
        assertEquals(5, everybody.size());
    }

    @Test
    public void testGetName() {
        assertEquals("pool", pool.getName());
    }
    
}
