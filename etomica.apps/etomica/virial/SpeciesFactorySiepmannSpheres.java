package etomica.virial;

import etomica.api.IVector;
import etomica.config.ConformationChainZigZag;
import etomica.api.ISimulation;
import etomica.space.Space;
import etomica.api.ISpecies;

/**
 * SpeciesFactory that makes Siepmann's alkane model.
 */
public class SpeciesFactorySiepmannSpheres implements SpeciesFactory, java.io.Serializable {

    public SpeciesFactorySiepmannSpheres(Space space, int nA) {
        this.nA = nA;
        IVector vector1 = space.makeVector();
        vector1.setX(0, bondL);
        IVector vector2 = space.makeVector();
        vector2.setX(0, bondL*Math.cos(bondTheta));
        vector2.setX(1, bondL*Math.sin(bondTheta));
        conformation = new ConformationChainZigZag(space, vector1, vector2);
    }
    
    public ISpecies makeSpecies(ISimulation sim, Space _space) {
        SpeciesAlkane species = new SpeciesAlkane(sim, _space, nA);
        species.getMoleculeType().setConformation(conformation);
        return species;
    }
    
    private static final long serialVersionUID = 1L;
    protected static final double bondL = 1.54;
    protected static final double bondTheta = Math.PI*114/180;
    private final int nA;
    private final ConformationChainZigZag conformation;
}
