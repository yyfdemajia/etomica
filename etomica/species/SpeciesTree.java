package etomica.species;
import java.lang.reflect.Constructor;

import etomica.EtomicaElement;
import etomica.EtomicaInfo;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomFactoryTree;
import etomica.atom.AtomTypeGroup;
import etomica.atom.AtomTypeSphere;
import etomica.simulation.Simulation;
import etomica.space.CoordinateFactory;
import etomica.space.CoordinateFactorySphere;

/**
 * Species in which molecules are formed as an arbitrarily shaped tree.
 * 
 * @author David Kofke
 */

public class SpeciesTree extends Species implements EtomicaElement {

    /**
     * Constructs with nA = {1}, such that each molecule is a group
     * containing just one atom (which is not the same as SpeciesSpheresMono,
     * for which each molecule is a single atom, not organized under a group).
     */
    public SpeciesTree(Simulation sim) {
        this(sim, new int[] {1});
    }

    /**
     * Constructor specifing tree structure through array of integers.
     * Each element of array indicates the number of atoms at the corresponding
     * level.  For example, nA = {2,4} will define a species in which each
     * molecule has 2 subgroups, each with 4 atoms (such as ethane, which
     * can be organized as CH3 + CH3)
     */
    public SpeciesTree(Simulation sim, int[] nA) {
        this(sim, nA, Species.makeAgentType(sim));
    }
    
    //TODO extend to permit specification of Conformation[], perhaps AtomSequencerFactory[]
    private SpeciesTree(Simulation sim, int[] nA, AtomTypeGroup agentType) {
        super(sim, new AtomFactoryTree(sim.getSpace(), agentType, nA), agentType);
        AtomTypeSphere atomType = new AtomTypeSphere(sim);
        //getLeafType will return the an AtomTypeGroup because leaf factory is not yet set
        atomType.setParentType((AtomTypeGroup)((AtomFactoryTree)factory).getLeafType());
        CoordinateFactory coordFactory = new CoordinateFactorySphere(sim);
        ((AtomFactoryTree)factory).setLeafFactory(new AtomFactoryMono(coordFactory, atomType));
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Species with molecules formed as an arbitrarily specified tree");
        return info;
    }
    
    public SpeciesSignature getSpeciesSignature() {
        Constructor constructor = null;
        try {
            constructor = this.getClass().getConstructor(new Class[]{Simulation.class});
        }
        catch(NoSuchMethodException e) {
            System.err.println("you have no constructor.  be afraid");
        }
        return new SpeciesSignature(getName(),constructor,new Object[]{new Integer(1)});
    }
    
    private static final long serialVersionUID = 1L;
}


