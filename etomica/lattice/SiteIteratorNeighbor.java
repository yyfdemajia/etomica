package etomica.lattice;
import etomica.*;
import etomica.action.AtomAction;
import etomica.action.AtomActionAdapter;

/**
 * Iterates over the neighbors of a particular site, as specified by 
 * the site's neighborManager.
 */
public class SiteIteratorNeighbor implements AtomIterator {
    
    private NeighborManager neighborManager;
    private final AtomIteratorList iterator = new AtomIteratorList();
    private boolean upListNow, doGoDown;
    private Atom next;
    private final IteratorDirective localDirective = new IteratorDirective(IteratorDirective.BOTH);
    
    public SiteIteratorNeighbor() {
        iterator.setSkipFirstAtom(true);
        localDirective.setSkipFirst(true);
    }
        
	public void all(Atom basis, IteratorDirective dummy, final AtomAction action) {
		if(basis == null || !(basis instanceof Site) || action == null) return;
//		iterator.all(((Site)basis).neighborManager().neighbors(), dummy, action);
		throw new RuntimeException("Method all not implemented in SiteIteratorNeighbor");
	}
   public boolean hasNext() {return iterator.hasNext();}
    
    public Atom reset() {
        return reset(IteratorDirective.BOTH);
    }

    public Atom reset(IteratorDirective.Direction direction) {
        return iterator.reset(neighborManager.tab, direction);
    }
    
    public void unset() {iterator.unset();}
    
    public Atom first() {
        throw new RuntimeException("method first() not implemented in SiteIteratorNeighbor");
    }
    public Atom next() {return iterator.next();}

    public void allAtoms(AtomActionAdapter act) {
        iterator.allAtoms(act);
    }
    
    public int size() {return neighborManager.neighborCount();}    
    
    public void setBasis(NeighborManager manager) {
        neighborManager = manager;
        iterator.setList(neighborManager.neighbors());
    }
    
    public void setBasis(Site site) {
        setBasis(site.neighborManager());
    }
    
    public void setBasis(Atom atom) {
        if(atom instanceof Site) setBasis(((Site)atom).neighborManager());
        else throw new IllegalArgumentException("Error in SiteIteratorNeighbor.setBasis:  Must specify a Site instance to set basis");
    }
    public Atom getBasis() {
        throw new RuntimeException("method SiteIteratorNeighbor.getBasis() not yet implemented");
    }
    public Atom reset(IteratorDirective d) {
        throw new RuntimeException("method SiteIteratorNeighbor.reset(IteratorDirective) not yet implemented");
    }
    public boolean contains(Atom a) {
        throw new RuntimeException("method SiteIteratorNeighbor.contains(Atom) not yet implemented");
    }
}//end of SiteIteratorNeighbor