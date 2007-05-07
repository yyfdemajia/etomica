
package etomica.nbr;

import etomica.atom.AtomSet;
import etomica.species.Species;

/**
 * Filters atoms pairs to match a given pair of species.
 * 
 * @author andrew
 */
public class CriterionSpecies extends CriterionAdapter {

    public CriterionSpecies(NeighborCriterion criterion, 
            Species species0, Species species1) {
        super(criterion);
        this.species0 = species0;
        this.species1 = species1;
    }
    
    /**
     * Returns true if the species for the pair of atoms match 
     * the species given at construction (without regard to the
     * order of the pair), and if the wrapped criterion accept
     * also returns true.
     */
    public boolean accept(AtomSet pair) {
        Species atom0Species = pair.getAtom(0).getType().getSpecies();
        Species atom1Species = pair.getAtom(1).getType().getSpecies();
        if( (atom0Species == species0 && atom1Species == species1) 
               || (atom0Species == species1 && atom1Species == species0) ) {
            return subCriterion.accept(pair);
        }
        return false;
    }
    
    public Species[] getSpecies() {
        return new Species[]{species0,species1};
    }
    
    private static final long serialVersionUID = 1L;
    private final Species species0;
    private final Species species1;
}
