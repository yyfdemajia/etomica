package etomica;

 /**
  * An atom iterator of the elements from an AtomArrayList (in proper
  * sequence).
  */

public class AtomArrayIterator implements AtomIterator {
	/**
 	 * Index of element to be returned by subsequent call to next.
 	 */
 	private int cursor = 0;
 	private Atom atoms[] = new Atom[1];
 	private AtomArrayList list;

 	public AtomArrayIterator(AtomArrayList atomList) {
 		list = atomList;
 	}
 	public void setList(AtomArrayList atomList) {
 		list = atomList;
 	}
 	
 	public int nBody() {return 1;}
 	public void unset() {cursor = size();}
 
 	public boolean hasNext() {
 	    return cursor != size();
 	}
 
 	public Atom nextAtom() {
	    	Atom next = list.get(cursor);
	    	return next;
 	}
 	
 	public Atom[] next() {
 		atoms[0] = list.get(cursor);
 		return atoms;
 	}
 
 	public Atom[] peek() {
 		atoms[0] = list.get(cursor);
 		return atoms;
 	}
 	public int size() {
 		return size();
 	}
 	
 	public void allAtoms(AtomsetActive act) {
 		int arraySize = size();
 		for (int i=0; i<arraySize; i++) {
 			atoms[0] = list.get(cursor);
 			act.actionPerformed(atoms);
 		}
 	}
 	
 	public void reset() {
 		cursor = 0;
 	}
 	
 	public boolean contains(Atom[] atom) {
 		return contains(atom);
 	}
 
 }