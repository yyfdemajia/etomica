package etomica;

/**
 * Lightweight version of AtomIteratorList.  Iterates uplist only, from
 * beginning to end of list.  Iterator functions correctly in situations where
 * elements are removed from list after they are returned by iterator.
 *
 * @author David Kofke
 */

/* History
 * 02/21/03 (DAK) added all(AtomList...) method
 * 08/23/04 (DAK, AS, KB) updated with overhaul of iterators
 * 
 */
public final class AtomIteratorListSimple implements AtomIterator {
    
	/**
	 * Constructs iterator with an empty list for iteration.
	 */
	public AtomIteratorListSimple() {
	    this(new AtomList());
	}
	/**
	 * Constructs iterator for iteration over the given list.
	 * Subsequent call to reset() is needed before beginning iteration.
	 * @param list
	 */
	public AtomIteratorListSimple(AtomList list) {
	    setList(list);
	}
    
	public boolean hasNext() {
		return next.atom != null;
	}
		
	/**
	 * Sets the list containing the atoms that will be returned by this iterator.
	 * Call to reset() is needed before beginning iteration.
	 */
    public void setList(AtomList newList) {
        list = (newList != null) ? newList : new AtomList();
        unset();
    }
 
    /**
     * Performs action on all atoms.
     */
    public void allAtoms(AtomsetActive action) {
    	final AtomLinker.Tab header = list.header;
        for (AtomLinker e = header.next; e != header; e = e.next) 
            if(e.atom != null) {
            	atoms[0] = e.atom;
            	action.actionPerformed(atoms);
            }
    }
    
    /**
     * Sets iterator so that it is ready to go up its entire list of iterates.
     */
    public void reset() {
        next = list.header.next;
        while(next.atom == null && next != list.header) next = next.next;
    }
        
    /**
     * Sets iterator such that hasNext() will return false.
     */
    public void unset() {next = list.header;}
    
    /**
     * Returns true if the given atom is in the list of iterates, false otherwise.
     */
	public boolean contains(Atom[] atom){
        return list.contains(atom[0]);
	}
	
	/**
	 * Returns the total number of iterates that can be returned by this iterator, for
	 * its current list basis.
	 */
	public int size() {return list.size();}

    /**
     * Returns the next atom in the list without advancing the iterator.
     */
    public Atom[] peek() {
        atoms[0] = next.atom;
        return atoms;
    }
    
	/**
	 * Returns the next atom in the list.
	 */    
    public Atom nextAtom() {
        return hasNext() ? nextLinker().atom : null;
    }
    
    public Atom[] next() {
    	if(!hasNext()) return null;
    	atoms[0] = nextLinker().atom;
    	return atoms;
    }
    
    public final int nBody() {return 1;}
    
    //returns current value of next, and advances next to its next value
    private AtomLinker nextLinker() {
        AtomLinker nextLinker = next;
        next = next.next;
        while(next.atom == null && next != list.header) {next = next.next;}
        return nextLinker;
    }//end of nextLinker
    	
    private AtomList list;
	private AtomLinker next;
	private final Atom[] atoms = new Atom[1];
    
//    public static void main(String[] args) {
//        Simulation sim = new Simulation();
//        Phase phase = new Phase();
//        SpeciesSpheresMono species = new SpeciesSpheresMono();
//        species.setNMolecules(10);
//        sim.elementCoordinator.go();
//        
//        boolean pauseForInput = true;
//        
//        AtomListRestorable list = new AtomListRestorable(phase.makeMoleculeIterator());
//        AtomIteratorListSimple iterator = new AtomIteratorListSimple(list);
//        
//        System.out.println("Original list");
//        iterator.reset();
//        while(iterator.hasNext()) System.out.println(iterator.next().toString());
//        if(pauseForInput) IteratorDirective.pauseForInput();
//        
//        System.out.println("Removing each element from list as iterated");
//        iterator.reset();
//        while(iterator.hasNext()) {
//            Atom atom = iterator.next();
//            System.out.println(atom.toString());
//            list.remove(atom);
//        }
//        if(pauseForInput) IteratorDirective.pauseForInput();
//        
//        System.out.println("Empty list");
//        iterator.reset();
//        while(iterator.hasNext()) System.out.println(iterator.next().toString());
//        if(pauseForInput) IteratorDirective.pauseForInput();
//    }//end main

}//end of AtomIteratorListSimple

