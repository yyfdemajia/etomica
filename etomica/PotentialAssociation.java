package etomica;

/**
 * This potential is modeled after the square well potential.  This is designed to create an attraction between
 * specific atoms on a molecule.  It DOES NOT implement any hard-core collisions, so the atom on the molecule that 
 * this potential affects simply acts as an attractive site and nothing else.
 *
 * @author Rob Riggleman
 */
public class PotentialAssociation extends Potential implements Potential.Hard {

    private double wellDiameter, wellDiameterSquared;
    private double epsilon;
    private double r2;
    private double lastCollisionVirial, lastCollisionVirialr2;
    private final Space.Tensor lastCollisionVirialTensor;
    private final Space.Vector dr;
    
    public PotentialAssociation() {
        this(Simulation.instance, Default.POTENTIAL_CUTOFF, Default.POTENTIAL_WELL);
    }
    public PotentialAssociation(double wellDiameter, double epsilon) {
        this(Simulation.instance, wellDiameter, epsilon);
    }
    public PotentialAssociation(Simulation sim, double wellDiameter, double epsilon) {
        super(sim);
        setEpsilon(epsilon);
        setWellDiameter(wellDiameter);
        dr = sim.space().makeVector();
        lastCollisionVirialTensor = sim.space().makeTensor();
    }
    
   /**
    * Always returns false, since the potential has no hard core.
    */
    public boolean overlap(AtomPair pair) {return false;}
    
   /**
    * Implements the collision dynamics.  Does not deal with the hard cores, only the wells.  This
    * section is essentially the same as PotentialSquareWell without the hard core section.
    */
    public void bump(AtomPair pair) {
        double eps = 1e-6;
        r2 = pair.r2();
        double bij = pair.vDotr();
        //ke is kinetic energy from the components of velocity
        double reduced_m = 1/(pair.atom1().rm() + pair.atom2().rm());
        dr.E(pair.dr());
        double ke = bij*bij*reduced_m/(2*r2);
        double r2New;
        if (bij > 0.0) {    //Separating
            if (ke < epsilon) {    //Not enough energy to escape the well
                lastCollisionVirial = 2*bij*reduced_m;
                r2New = (1 - eps)*wellDiameterSquared;
            }
            else {  //Escaping the well
                lastCollisionVirial = reduced_m*(bij - Math.sqrt(bij*bij - 2.0*r2*epsilon/reduced_m));
                r2New = (1 + eps)*wellDiameterSquared;
            }
        }
        else {          //Approaching
            lastCollisionVirial = reduced_m*(bij + Math.sqrt(bij*bij + 2.0*r2*epsilon/reduced_m));  //might need to double check
            r2New = (1 - eps)*wellDiameterSquared;
        }
        lastCollisionVirialr2 = lastCollisionVirial/r2;
        pair.cPair.push(lastCollisionVirialr2);
        if(r2New != r2) pair.cPair.setSeparation(r2New);
    }       //end of bump
    
    
    public double lastCollisionVirial() {return lastCollisionVirial;}
    
    public Space.Tensor lastCollisionVirialTensor() {
        lastCollisionVirialTensor.E(dr, dr);
        lastCollisionVirialTensor.TE(lastCollisionVirialr2);
        return lastCollisionVirialTensor;
    }
    
   /**
    * Computes the next time of collision of the given atomPair assuming free flight.  Only computes the next
    * collision of the wells.  Takes into account both separation and convergence.
    */
    public double collisionTime(AtomPair pair) {
        double discr = 0.0;
        double bij = pair.vDotr();
        double r2 = pair.r2();
        double v2 = pair.v2();
        double tij = Double.MAX_VALUE;
        
        if (r2 < wellDiameterSquared) {         //check to see if already inside wells
            discr = bij*bij - v2 * (r2 - wellDiameterSquared );
            tij = (-bij + Math.sqrt(discr))/v2;
        }
        else {
            if (bij < 0.) { //Approaching
                discr = bij*bij - v2 * (r2 - wellDiameterSquared );
                if (discr > 0.) {
                    tij = (-bij - Math.sqrt(discr))/v2;
                }
            }
        }
        return tij;
    }
    
  /**
   * Returns -epsilon if less than well diameter, or zero otherwise.
   */
    public double energy(AtomPair pair) {
        return (pair.r2() < wellDiameterSquared) ?  -epsilon : 0.0;
    }
    
    /**
     * Always returns zero.
     */
    public double energyLRC(int n1, int n2, double V) {return 0.0;}
     
   /**
    * Accessor for well-diameter.
    * Since the well-diameter is not a multiplier in this potential as in square well, it is necessary
    * to be able to set this manually if so desired.
    */
    public double getWellDiameter() {return wellDiameter;}
    
   /**
    * Accessor for well-diameter.
    * Allows manual changing of the well diameter since it is not merely a multiple of the core-diameter
    * as in square well.
    */
    
    public void setWellDiameter(double w) {
        wellDiameter = w;
        wellDiameterSquared = w*w;
    }
    
   /**
    * Accessor method for depth of well.
    */
    public double getEpsilon() {return epsilon;}
    
   /**
    * Accessor method for depth of well.
    */
    public void setEpsilon(double s) {
        epsilon = s;
    }
}