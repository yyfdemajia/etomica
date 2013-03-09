package etomica.virial;

import etomica.math.SpecialFunctions;

/**
 * This class calculates the sum of all biconnected clusters using Wheatley's
 * recursive formulation.
 * 
 * @author David Kofke and Andrew Schultz 
 */
public class ClusterWheatley implements ClusterAbstract {

    protected final int n;
    protected final MayerFunction f;
    
    protected final double[][] eValues;
    protected final double[] fQ, fC;
    protected final double[][] fA, fB;
    protected int cPairID = -1, lastCPairID = -1;
    protected double value, lastValue;
    protected double beta;

    
    public ClusterWheatley(int nPoints, MayerFunction f) {
        this.n = nPoints;
        this.f = f;
        eValues = new double[nPoints][nPoints];
        int nf = 1<<n;  // 2^n
        fQ = new double[nf];
        fC = new double[nf];
        for(int i=0; i<n; i++) {
            fQ[1<<i] = 1.0;
        }
        fA = new double[n][nf];
        fB = new double[n][nf];
    }

    public ClusterAbstract makeCopy() {
        ClusterWheatley c = new ClusterWheatley(n, f);
        c.setTemperature(1/beta);
        return c;
    }

    public int pointCount() {
        return n;
    }

    public double value(BoxCluster box) {
      CoordinatePairSet cPairs = box.getCPairSet();
      int thisCPairID = cPairs.getID();
      if (thisCPairID == cPairID) {
          return value;
      }
      if (thisCPairID == lastCPairID) {
          // we went back to the previous cluster, presumably because the last
          // cluster was a trial that was rejected.  so drop the most recent value/ID
          cPairID = lastCPairID;
          value = lastValue;
          return value;
      }

      // a new cluster
      lastCPairID = cPairID;
      lastValue = value;
      cPairID = thisCPairID;
      
      updateF(box);
      
      calcValue();
      if (Double.isNaN(value) || Double.isInfinite(value)) {
          updateF(box);
          calcValue();
          throw new RuntimeException("oops");
      }
      return value;
    }

    /*
     * Computation of sum of biconnected diagrams.
     */
    protected void calcValue() {
        int nf = 1<<n;
        
        // generate all partitions and compute product of e-bonds fo
        for (int i=3; i<nf; i++) {
            int j = i & -i;//lowest bit in i
            if (i==j) continue; // 1-point set
            int k = (i&~j); // k is the points in i other than j
            int jj = Integer.numberOfTrailingZeros(j); // jj = log2(j)
            fQ[i] = fQ[k];
            for (int l=jj+1; l<n; l++) {
                int ll = 1<<l;
                if ((ll&k)==0) continue;
                // l is a point in i, but is not j
                fQ[i] *= eValues[jj][l];
            }
        }

        //Compute the fC's
        for(int i=1; i<nf; i++) {
            fC[i] = fQ[i];
            int iLowBit = i & -i;
            for(int j=1; j<i; j++) {
                if ((j & iLowBit) == 0) continue;
                int jComp = i & ~j;
                if ((jComp | j) != i) continue;
                fC[i] -= fC[j] * fQ[jComp];//for fQ, flip the bits on j; use only those appearing in i
            }
        }

        // find fA1
        for (int i=1; i<nf; i++) {
            fA[0][i] = 0;
            fB[0][i] = fC[i];
            if((i & 1) == 0 || i == 1) continue;//if i doesn't contain 1, or is 1, fA and fB are done
            //at this point we know lowest bit in i is 1; add next lowest bit to it
            int ii = i - 1;//all bits in i but lowest
            int jBits = 1 | (ii & -ii);
            //at this point jBits has 1 and next lowest bit in i
            for (int j=3; j<i; j+=2) {//sum over partitions of i containing 1
                if ((j & jBits) != jBits) continue;//ensure jBits are in j
                int jComp = (i & ~j); //subset of i complementing j
                if (jComp==0 || (jComp | j) != i) continue;
                fA[0][i] += fB[0][j] * fC[jComp|1];
            }
            fB[0][i] -= fA[0][i];//remove from B graphs that contain articulation point at 0
        }
        
        for (int v=1; v<n; v++) {
            int vs1 = 1<<v;
            for (int i=1; i<nf; i++) {
                fA[v][i] = 0;
                fB[v][i] = fB[v-1][i];//no a.p. at v or below, starts with those having no a.p. at v-1 or below
                //rest of this is to generate A (diagrams having a.p. at v but not below), and subtract it from B
                if ((i & vs1) == 0) continue;//if i doesn't contain v, fA and fB are done
                int jBits = (i&-i); //lowest bit in i
                if (jBits != vs1) { //lowest bit is not v; add v to it
                    jBits |= vs1;
                }
                else if (jBits == i) { //lowest bit is only bit; fA and fB are done
                    continue;
                }
                else {
                    int ii = i & ~jBits;
                    jBits |= (ii & -ii);
                }
                //at this point jBits has (lowest bit + v) or (v + next lowest bit)
                for (int j=3; j<i; j++) {//sum over partitions of i
                    if ((j & jBits) != jBits) continue;//ensure jBits are in j
                    int jComp = i & ~j;//subset of i complementing j
                    if (jComp==0 || (jComp | j) != i) continue;
                    fA[v][i] += fB[v][j] * (fB[v][jComp|vs1] + fA[v][jComp|vs1]);
                }
                fB[v][i] -= fA[v][i];//remove from B graphs that contain articulation point at v
            }
        }

        value = (1-n)*fB[n-1][nf-1]/SpecialFunctions.factorial(n);
    }

    protected void updateF(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();

        f.setBox(box);
        // recalculate all f values for all pairs
        for(int i=0; i<n-1; i++) {
            for(int j=i+1; j<n; j++) {
                double x = f.f(aPairs.getAPair(i,j),cPairs.getr2(i,j), beta)+1;
                eValues[i][j] = x;
                eValues[j][i] = x;
            }
        }
    }

    public void setTemperature(double temperature) {
        beta = 1/temperature;
    }
}
