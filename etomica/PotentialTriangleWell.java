package simulate;

/**
 * Hard core with an attractive tail that goes to zero linearly with r.
 * 
 * @author Jhumpa Adhikari
 */

public class PotentialTriangleWell extends Potential {

  private double coreDiameter, coreDiameterSquared;
  private double wellDiameter, wellDiameterSquared;
  private double lambda; //wellDiameter = coreDiameter * lambda ;lambda is well width
  private double epsilon;
  private double constant;
  private final Space.Vector force;

  public PotentialTriangleWell() {
    this(Simulation.instance,Default.ATOM_SIZE, Default.POTENTIAL_CUTOFF, Default.POTENTIAL_WELL);
  }
  public PotentialTriangleWell(double coreDiameter, double lambda, double epsilon) {
    this(Simulation.instance, coreDiameter, lambda, epsilon);
  }
  
  public PotentialTriangleWell(Simulation sim, double coreDiameter, double lambda, double epsilon) {
    super(sim);
    setCoreDiameter(coreDiameter);
    setLambda(lambda);
    setEpsilon(epsilon);
    force = sim.space.makeVector();
  }

  public boolean overlap(AtomPair pair) {return pair.r2() < coreDiameterSquared;}

    public double energy(AtomPair pair) {

        double r2 = pair.r2();
       
        if(r2 < coreDiameterSquared)
            return Double.MAX_VALUE;

        if(r2 > wellDiameterSquared)
            return 0.0;
            
        double r1 = Math.sqrt(r2);
            
        return (epsilon/(lambda - 1.0))*((r1/coreDiameter)- lambda);
    }
 

    public Space.Vector force(AtomPair pair){
        
        double r2 = pair.r2();
        if(r2 > wellDiameterSquared){
            force.E(0.0);
        }
        if(r2 < wellDiameterSquared){
            force.E(pair.dr());
            force.TE(constant/Math.sqrt(r2));//lambda > 1.0
            
        }
        return force;
    }
     
    public double energyLRC(int n1, int n2, double V) {return 0.0;}

    public double getCoreDiameter() {return coreDiameter;}
    public final void setCoreDiameter(double c) {
        coreDiameter = c;
        coreDiameterSquared = c*c;
        wellDiameter = coreDiameter*lambda;
        wellDiameterSquared = wellDiameter*wellDiameter;
        constant = epsilon/(coreDiameter*(1.0 - lambda));
    }

    public double getLambda() {return lambda;}
    public final void setLambda(double lam) {
        lambda = lam;
        wellDiameter = coreDiameter*lambda;
        wellDiameterSquared = wellDiameter*wellDiameter;
        constant = epsilon/(coreDiameter*(1.0 - lambda));
    }

    public double getEpsilon() {return epsilon;}
    public final void setEpsilon(double eps) {
        epsilon = eps;
        constant = epsilon/(coreDiameter*(1.0 - lambda));
    }
}

  