package etomica.potential;

import etomica.api.IMoleculeList;
import etomica.api.IVectorMutable;
import etomica.potential.P3HydrogenManzhos.P3HydrogenManzhosMolecular;
import etomica.space.ISpace;

public class P3HydrogenGarberoglioMolecular extends P3HydrogenManzhosMolecular {
    protected double R0 = 5.0;
    protected double dR = 0.4;
    protected IVectorMutable[] pos = new IVectorMutable[3];
    public P3HydrogenGarberoglioMolecular(ISpace space) {
        super(space);
        for (int i=0; i<3; i++) {
            pos[i] = space.makeVector();
        }
        
    }
    protected double f(double r) {
        double x = 1/(Math.exp((r-R0)/dR)+1);
        return x;        
    }
    public double energy(IMoleculeList molecules) {
        double E1 = super.energy(molecules);        
        for (int i=0; i<molecules.getMoleculeCount(); i++) {                
            pos[i].Ev1Pv2(v[2*i], v[2*i+1]);
            pos[i].TE(0.5);
        }
        double r01 = Math.sqrt(pos[0].Mv1Squared(pos[1]));
        double r12 = Math.sqrt(pos[1].Mv1Squared(pos[2]));
        double r02 = Math.sqrt(pos[0].Mv1Squared(pos[2]));
        return E1*f(r01)*f(r12)*f(r02);
        
    }
    

}