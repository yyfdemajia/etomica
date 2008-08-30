package etomica.junit.atom.iterator;

import etomica.action.AtomsetAction;
import etomica.action.AtomsetActionAdapter;
import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.atom.AtomArrayList;
import etomica.atom.iterator.ApiIntraspeciesAA;
import etomica.junit.UnitTestUtil;


/**
 * Unit test for ApiIntraspeciesAA
 *
 * @author David Kofke
 *
 */

public class ApiIntraspeciesAATest extends IteratorTestAbstract {

    public void testIterator() {
        
        int[] n0 = new int[] {10, 1, 0};
        int nA0 = 5;
        int[] n1 = new int[] {5, 1, 6};
        ISimulation sim = UnitTestUtil.makeStandardSpeciesTree(n0, nA0, n1);
        
        ISpecies[] species = new ISpecies[sim.getSpeciesManager().getSpeciesCount()];
        for(int i = 0; i < sim.getSpeciesManager().getSpeciesCount(); i++) {
        	species[i] = sim.getSpeciesManager().getSpecies(i);
        }
        boxTest(sim.getBox(0), species);
        boxTest(sim.getBox(1), species);
        boxTest(sim.getBox(2), species);
        
        
        //test new iterator gives no iterates
        ApiIntraspeciesAA api = new ApiIntraspeciesAA(species[0]);
        testNoIterates(api);

        //test documented exceptions
        boolean exceptionThrown = false;
        try {
            new ApiIntraspeciesAA((ISpecies)null);
        } catch(NullPointerException e) {exceptionThrown = true;}
        assertTrue(exceptionThrown);

        
    }
    
    /**
     * Performs tests on different species combinations in a particular box.
     */
    private void boxTest(IBox box, ISpecies[] species) {
        speciesTestForward(box, species, 0);
        speciesTestForward(box, species, 1);
    }

    /**
     * Test iteration in various directions with different targets.  Iterator constructed with
     * index of first species less than index of second.
     */
    private void speciesTestForward(IBox box, ISpecies[] species, int species0Index) {
        ApiIntraspeciesAA api = new ApiIntraspeciesAA(species[species0Index]);
        AtomsetAction speciesTest = new SpeciesTestAction(species[species0Index], species[species0Index]);

        api.setBox(box);
        IAtom[] molecules0 = ((AtomArrayList)box.getMoleculeList(species[species0Index])).toArray();
        
        int count = molecules0.length * (molecules0.length - 1) / 2;
        
        countTest(api, count);
        api.allAtoms(speciesTest);

        //test null box throws an exception
        boolean exceptionThrown = false;
        try {
            api.setBox(null);
        }
        catch (RuntimeException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    private class SpeciesTestAction extends AtomsetActionAdapter {
        final ISpecies species0, species1;
        public SpeciesTestAction(ISpecies species0, ISpecies species1) {
            this.species0 = species0;
            this.species1 = species1;
        }
        public void actionPerformed(IAtomSet atomSet) {
            assertTrue(((IMolecule)atomSet.getAtom(0)).getType() == species0);
            assertTrue(((IMolecule)atomSet.getAtom(1)).getType() == species1);
        }
    }
    
}
