package etomica;

/**
 * Class for construction of iterators of molecules.  Iterators are given for looping
 * over single molecules and pairs of molecules.  Different pair iterators are provided
 * for looping pairs from the same species, and pairs from different species.  
 * Straightforward iterators are given by IteratorFactorySimple, which generates pairs
 * from the childlists of species agents.  Iteration based on cell lists is performed by 
 * iterators given by IteratorFactoryCell, in the etomica.nbr.cell package.
 *
 * @author David Kofke
 */

public abstract class IteratorFactory {

    /**
     * Selects an appropriate iterator for the given species array.  If array contains
     * only one element, an atom iterator is returned. If array contains two elements,
     * an atom-pair iterator is returned, as given by the makeIntraSpeciesPairIterator
     * method if both elements of the array are equal, or as given by the
     * makeInterSpeciesPairIterator method if the array elements are different.
     * @param species array used to determine type of iterator to return
     * @return an appropriate iterator for looping over molecules of the given species
     */
    public AtomsetIterator makeMoleculeIterator(Species[] species) {
        if (species == null || species.length == 0 || species.length > 2
                || species[0] == null || species[species.length-1] == null) {
            throw new IllegalArgumentException("null or invalid number of species.  Must specify either 1 or 2 species instances.");
        }
        if (species.length==1) {
            return new AtomIteratorBasis();
        }
        if (species[0] == species[1]) {
            return makeIntraSpeciesPairIterator(species);
        }
        return makeInterSpeciesPairIterator(species);
    }
    
    /**
     * creates a pair iterator which loops over all pairs in a neighbor list
     * between two groups
     * @return the pair iterator
     */
    public abstract AtomsetIterator makeInterSpeciesPairIterator(Species[] species);
    
    /**
     * creates a pair iterator which loops over all pairs in a neighbor list
     * within one group
     * @return the pair iterator
     */
    public abstract AtomsetIterator makeIntraSpeciesPairIterator(Species[] species);
    
    /**
     * Sequencer used for molecule-level atoms (those with a SpeciesAgent
     * as the parent). Special because if cell lists are used, they are
     * kept for these atoms.
      */
    public abstract AtomSequencer.Factory moleculeSequencerFactory();
    
    /**
     * Sequencer used for atoms on which concrete potentials (non-group) act.
     * Special because if neighbor lists are used, they are kept for these atoms.
     * @return
     */
    public abstract AtomSequencer.Factory interactionAtomSequencerFactory();

    /**
     * Sequencer used for molecule-level atoms on which concrete potentials 
     * (non-group) act.  Special because they must be able to handle both neighbor
     * and cell listing.
     * @return
     */
    public abstract AtomSequencer.Factory interactionMoleculeSequencerFactory();

}
