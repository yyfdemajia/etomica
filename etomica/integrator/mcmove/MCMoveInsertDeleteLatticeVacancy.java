package etomica.integrator.mcmove;

import java.util.ArrayList;
import java.util.List;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IIntegrator;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.space.ISpace;
import etomica.space.IVectorRandom;
import etomica.util.IEvent;
import etomica.util.IListener;

/**
 * Looks for atoms without a full set of first nearest neighbors and attempts
 * insertions adjacent to one of those atoms.  Hoops are jumped through to
 * ensure detailed balance.
 * 
 * Neighbor lists are used to find insertion points and candidates for
 * deletion.  Internal lists are constructed (relatively expensive) and then
 * reconstructed when a move is accepted or when some other part of the
 * simulation moves atoms (this part is somewhat hardcoded; enjoy).
 *
 * @author Andrew Schultz
 */
public class MCMoveInsertDeleteLatticeVacancy extends MCMoveInsertDeleteBiased implements IListener {

    protected final IVectorRandom dest;
    protected final IVectorMutable dr;
    protected IIntegrator integrator;
    protected long lastStepCount;
    protected boolean dirty;
    protected double maxDistance, maxInsertDistance;
    protected double nbrDistance;
    protected List<Integer> insertCandidates, deleteCandidates;
    protected int[] numNeighbors, numNeighborCandidatesOnDelete, deleteCandidateTimes, numDeleteCandidateNbrs;
    protected int totalDeleteCandidateTimes;
    protected PotentialMasterList potentialMaster;
    protected int numNewDeleteCandidates;
    protected int forced = 0;
    protected double oldLnA, oldB, newLnA;
    protected final IVectorMutable oldPosition;
    protected IVectorMutable[] nbrVectors;
    protected final ISpace space;
    protected double oldBoxSize;

    public MCMoveInsertDeleteLatticeVacancy(IPotentialMaster potentialMaster, 
            IRandom random, ISpace _space, IIntegrator integrator, double maxDistance, int fixedN, int maxDN) {
        super(potentialMaster, random, _space, fixedN, maxDN);
        this.space = _space;
        this.potentialMaster = (PotentialMasterList)potentialMaster;
        dest = (IVectorRandom)_space.makeVector();
        dr = _space.makeVector();
        this.integrator = integrator;
        this.maxDistance = maxDistance;
        maxInsertDistance = 0.05;
        insertCandidates = new ArrayList<Integer>();
        deleteCandidates = new ArrayList<Integer>();
        numNeighbors = new int[0];
        numNeighborCandidatesOnDelete = new int[0];
        lastStepCount = -1;
        dirty = true;
        lnbias = new double[0];
        oldPosition = _space.makeVector();
    }
    
    public void setBox(IBox box) {
        super.setBox(box);
        // cubic
        oldBoxSize = box.getBoundary().getBoxSize().getX(0);
    }
    
    public void makeFccVectors(double nbrDistance) {
        this.nbrDistance = nbrDistance;
        double maxInsertNbrDistance = nbrDistance + maxInsertDistance;
        if (maxInsertNbrDistance > maxDistance) {
            throw new RuntimeException("nbrDistance must be greater than maxInsert distance");
        }
        nbrVectors = new IVectorMutable[12];
        double s = nbrDistance/Math.sqrt(2);
        for (int i=0; i<12; i++) {
            nbrVectors[i] = space.makeVector();
            boolean even = i%2 == 0;

            if (i < 4) {
                nbrVectors[i].setX(0, i<2 ? -s : s);
                nbrVectors[i].setX(1, even ? -s : s);
            }
            else if (i < 8) {
                nbrVectors[i].setX(1, i<6 ? -s : s);
                nbrVectors[i].setX(2, even ? -s : s);
            }
            else {
                nbrVectors[i].setX(0, i<10 ? -s : s);
                nbrVectors[i].setX(2, even ? -s : s);
            }
        }
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }
    
    public double getMaxDistance() {
        return maxDistance;
    }
    
    public void setMaxInsertDistance(double maxInsertDistance) {
        this.maxInsertDistance = maxInsertDistance;
    }
    
    public void reset() {
        dirty = true;
    }

    public void setLnBias(int n, double nBias) {
        forced = 0;
        super.setLnBias(n, nBias);
    }
    
    public boolean doTrial() {
        if (integrator.getStepCount() != lastStepCount) {
            forced = 0;
        }
        int numAtoms = box.getLeafList().getAtomCount();
        if (forced==1) {
            insert = !insert;
            System.out.println("forcing "+(insert ? "insertion" : "deletion"));
            forced=2;
        }
        else if (fixedN==numAtoms && maxDN==0) {
            insert = !insert;
        }
        else {
            insert = (random.nextInt(2) == 0);
        }
//        insert = false;
        numNewDeleteCandidates = 0;
        if (dirty || lastStepCount != integrator.getStepCount()) findCandidates();
        if(insert) {
            if(!reservoir.isEmpty()) testMolecule = reservoir.remove(reservoir.getMoleculeCount()-1);
            else testMolecule = species.makeMolecule();
            IAtom testAtom = testMolecule.getChildList().getAtom(0);

            int nInsertCandidates = insertCandidates.size();
            if (nInsertCandidates == 0) return false;
            IAtom partner = box.getLeafList().getAtom(insertCandidates.get(random.nextInt(nInsertCandidates)));
//            System.out.println("inserting next to "+partner);
            uOld = 0;
            int iLatVec = random.nextInt(nbrVectors.length);
            dest.setRandomInSphere(random);
            dest.TE(maxInsertDistance);
            dest.PE(nbrVectors[iLatVec]);
            testAtom.getPosition().E(partner.getPosition());
            testAtom.getPosition().PE(dest);
            if (forced==2) {
                testAtom.getPosition().E(oldPosition);
            }
            box.addMolecule(testMolecule);
            energyMeter.setTarget(testMolecule);
            uNew = energyMeter.getDataAsScalar();

            // inserting testAtom might change some deleteCandidates
            // some existing deleteCandidates with nbrs=12 might have 13 now
            // we also need to see how many times testAtom shows up as a neighbor
            // of a deleteCandidate

            IVector pi = testAtom.getPosition();
            IAtomList nbrs = potentialMaster.getNeighborManager(box).getUpList(testAtom)[0];
            int nTestNbrs = 0, nTestNbrsDeletion = 0;
            double maxInsertNbrDistance = nbrDistance + maxInsertDistance;
            double minInsertNbrDistance = nbrDistance - maxInsertDistance;
            for (int j=0; j<nbrs.getAtomCount(); j++) {
                IAtom jAtom = nbrs.getAtom(j);
                int jj = jAtom.getLeafIndex();
                dr.Ev1Mv2(pi, jAtom.getPosition());
                box.getBoundary().nearestImage(dr);
                double r2 = dr.squared();
                if (r2 < maxDistance*maxDistance) {
                    nTestNbrs++;
                    boolean deleteCand = false;
                    if (r2 < maxInsertNbrDistance*maxInsertNbrDistance && r2 > minInsertNbrDistance*minInsertNbrDistance) {
                        for (int k=0; k<nbrVectors.length; k++) {
                            double s2 = dr.Mv1Squared(nbrVectors[k]);
                            if (s2 < maxInsertDistance*maxInsertDistance) {
                                deleteCand = true;
                                break;
                            }
                        }
                        if (deleteCand) {
                            nTestNbrsDeletion++;
                        }
                    }
                    if (numNeighbors[jj] == 12) {
                        // jj now has 13 neighbors
                        // by inserting testAtom, jj's old neighbors can no longer be deleted
                        numNewDeleteCandidates-=numDeleteCandidateNbrs[jj];
                    }
                    else if (numNeighbors[jj] < 12 && deleteCand) {
                        // testAtom would be a deleteCandidate due to jj
                        numNewDeleteCandidates++;
                    }                        
                }
            }
            nbrs = potentialMaster.getNeighborManager(box).getDownList(testAtom)[0];
            for (int j=0; j<nbrs.getAtomCount(); j++) {
                IAtom jAtom = nbrs.getAtom(j);
                int jj = jAtom.getLeafIndex();
                dr.Ev1Mv2(pi, jAtom.getPosition());
                box.getBoundary().nearestImage(dr);
                double r2 = dr.squared();
                if (r2 < maxDistance*maxDistance) {
                    nTestNbrs++;
                    boolean deleteCand = false;
                    if (r2 < maxInsertNbrDistance*maxInsertNbrDistance && r2 > minInsertNbrDistance*minInsertNbrDistance) {
                        for (int k=0; k<nbrVectors.length; k++) {
                            double s2 = dr.Mv1Squared(nbrVectors[k]);
                            if (s2 < maxInsertDistance*maxInsertDistance) {
                                deleteCand = true;
                                break;
                            }
                        }
                        if (deleteCand) {
                            nTestNbrsDeletion++;
                        }
                    }
                    if (numNeighbors[jj] == 12) {
                        // by inserting testAtom, jj's old neighbors can no longer be deleted
                        numNewDeleteCandidates-=numDeleteCandidateNbrs[jj];
                    }
                    else if (numNeighbors[jj] < 12 && deleteCand) {
                        // jj now has 1 more neighbor
                        // testAtom would be a deleteCandidate due to jj
                        numNewDeleteCandidates++;
                    }                        
                }
            }
            
            // it could be that testAtom itself has 12 or fewer neighbors
            // if so, those neighbors would now be delete candidates
            if (nTestNbrs<=12) {
                numNewDeleteCandidates += nTestNbrsDeletion;
                
            }
        } else {//delete
            if(box.getNMolecules(species) == 0) {
                testMolecule = null;
                return false;
            }
            int nDeleteCandidates = deleteCandidates.size();
            if (nDeleteCandidates == 0) return false;
            int irand = random.nextInt(totalDeleteCandidateTimes);
            int icount = 0;
            int ip = -1;
            for (int i : deleteCandidates) {
                icount += deleteCandidateTimes[i];
                if (icount > irand) {
                    ip = i;
                    break;
                }
            }
            if (forced==2) {
                ip = numAtoms-1;
            }

            IAtom testAtom = box.getLeafList().getAtom(ip);
            testMolecule = testAtom.getParentGroup();
            //delete molecule only upon accepting trial
            energyMeter.setTarget(testMolecule);
            uOld = energyMeter.getDataAsScalar();
            
            // by deleting testAtom, we might turn other atoms into "insertCandidates"
            // numNeighborCandidates is how many of our neighbors have 12 nbrs.
            uNew = 0;
            
        }
        return true;
    }

    protected void findCandidates() {
        double newBoxSize = box.getBoundary().getBoxSize().getX(0);
        if (Math.abs(newBoxSize-oldBoxSize)/oldBoxSize > 1e-14) {
            for (int i=0; i<12; i++) {
                nbrVectors[i].TE(newBoxSize/oldBoxSize);
            }
            nbrDistance *= newBoxSize/oldBoxSize;
            oldBoxSize = newBoxSize;
        }
        
        NeighborListManager nbrManager = potentialMaster.getNeighborManager(box);
        IBoundary boundary = box.getBoundary();
        int numAtoms = box.getLeafList().getAtomCount();
        if (numNeighbors.length < numAtoms) {
            numNeighbors = new int[numAtoms];
            numNeighborCandidatesOnDelete = new int[numAtoms];
            numDeleteCandidateNbrs = new int[numAtoms];
            deleteCandidateTimes = new int[numAtoms];
        }
        insertCandidates.clear();
        deleteCandidates.clear();
        totalDeleteCandidateTimes = 0;
        for (int i=0; i<numAtoms; i++) {
            numNeighbors[i] = numNeighborCandidatesOnDelete[i] = numDeleteCandidateNbrs[i] = deleteCandidateTimes[i] = 0;
        }
        for (int i=0; i<numAtoms; i++) {
            IAtom iAtom = box.getLeafList().getAtom(i);
            IVector pi = iAtom.getPosition();
            IAtomList nbrsUp = nbrManager.getUpList(iAtom)[0];
            for (int j=0; j<nbrsUp.getAtomCount(); j++) {
                dr.Ev1Mv2(pi, nbrsUp.getAtom(j).getPosition());
                boundary.nearestImage(dr);
                double r2 = dr.squared();
                if (r2 < maxDistance*maxDistance) {
                    numNeighbors[i]++;
                    numNeighbors[nbrsUp.getAtom(j).getLeafIndex()]++;
                }
            }
        }
        double maxInsertNbrDistance = nbrDistance + maxInsertDistance;
        double minInsertNbrDistance = nbrDistance - maxInsertDistance;
        for (int i=0; i<numAtoms; i++) {
            if (numNeighbors[i] < 13) {
                // the neighbors of i may be candidates for deletion.  after deleting
                // one of its neighbors, i would have <12 neighbors
                IAtom iAtom = box.getLeafList().getAtom(i);
                IVector pi = iAtom.getPosition();
                IAtomList nbrs = nbrManager.getUpList(iAtom)[0];
                for (int j=0; j<nbrs.getAtomCount(); j++) {
                    IAtom jAtom = nbrs.getAtom(j);
                    int jj = jAtom.getLeafIndex();
                    dr.Ev1Mv2(pi, jAtom.getPosition());
                    boundary.nearestImage(dr);
                    double r2 = dr.squared();
                    if (numNeighbors[i] == 12 && r2 < maxDistance*maxDistance) {
                        // if we delete jj then i becomes an "insertCandidate"
                        numNeighborCandidatesOnDelete[jj]++;
                    }
                    if (r2 < maxInsertNbrDistance*maxInsertNbrDistance && r2 > minInsertNbrDistance*minInsertNbrDistance) {
                        boolean success = false;
                        for (int k=0; k<nbrVectors.length; k++) {
                            double s2 = dr.Mv1Squared(nbrVectors[k]);
                            if (s2 < maxInsertDistance*maxInsertDistance) {
                                success = true;
                                break;
                            }
                        }
                        if (success) {
                            // we need to know how many times jj shows up as a delete candidate
                            deleteCandidateTimes[jj]++;
                            numDeleteCandidateNbrs[i]++;
                            totalDeleteCandidateTimes++;
                            if (deleteCandidateTimes[jj] > 1) continue;
                            deleteCandidates.add(jj);
                        }
                    }
                }
                nbrs = nbrManager.getDownList(iAtom)[0];
                for (int j=0; j<nbrs.getAtomCount(); j++) {
                    IAtom jAtom = nbrs.getAtom(j);
                    int jj = jAtom.getLeafIndex();
                    dr.Ev1Mv2(pi, jAtom.getPosition());
                    boundary.nearestImage(dr);
                    double r2 = dr.squared();
                    if (numNeighbors[i] == 12 && r2 < maxDistance*maxDistance) {
                        // if we delete jj then i becomes an "insertCandidate"
                        numNeighborCandidatesOnDelete[jj]++;
                    }
                    if (r2 < maxInsertNbrDistance*maxInsertNbrDistance && r2 > minInsertNbrDistance*minInsertNbrDistance) {
                        boolean success = false;
                        for (int k=0; k<nbrVectors.length; k++) {
                            double s2 = dr.Mv1Squared(nbrVectors[k]);
                            if (s2 < maxInsertDistance*maxInsertDistance) {
                                success = true;
                                break;
                            }
                        }
                        if (success) {
                            // we need to know how many times jj shows up as a delete candidate
                            deleteCandidateTimes[jj]++;
                            numDeleteCandidateNbrs[i]++;
                            totalDeleteCandidateTimes++;
                            if (deleteCandidateTimes[jj] > 1) continue;
                            deleteCandidates.add(jj);
                        }
                    }
                }
                if (numNeighbors[i] < 12) {
                    // we will attempt to insert next to i
                    insertCandidates.add(i);
                    // deleting i will remove an insert candidate
                    numNeighborCandidatesOnDelete[i]--;
                }
            }
        }
        dirty = false;
        lastStepCount = integrator.getStepCount();
    }

    public double getA() {
        double lna = getLnBiasDiff();

        double shellV = nbrVectors.length*4.0/3.0*Math.PI*maxInsertDistance*maxInsertDistance*maxInsertDistance;
        double c = 0;
        if (insert) {
            c = insertCandidates.size()*shellV/(totalDeleteCandidateTimes + numNewDeleteCandidates);
        }
        else {
            // our candidate for deletion was listed deleteCandidateTimes times
            int newInsertCandidates = insertCandidates.size() + numNeighborCandidatesOnDelete[testMolecule.getIndex()];
            c = totalDeleteCandidateTimes/(shellV*newInsertCandidates);
        }
        if (forced > 0) {
            double x = lna + Math.log(c);
            System.out.println("**** lnA = "+x+"    "+lna+"    "+Math.log(c));
            System.out.println("*old lnA = "+oldLnA);
            if (Math.abs(x+oldLnA) > 1e-6) {
                throw new RuntimeException("oops");
            }
        }
        oldLnA = lna+Math.log(c);
        if (false) {
            System.out.println(insert+" lnbias "+lna+" log(c) "+Math.log(c)+" log(a) "+oldLnA);
        }
        return Math.exp(lna)*c;
    }

    public double getB() {
        double b = uOld - uNew;
        if (false) System.out.println(insert+" b = "+b);
        if (forced==2) {
            System.out.println("**** b = "+b);
            System.out.println("*old b = "+oldB);
            if (Math.abs((b+oldB)/(1+b-oldB)) > 1e-6) {
                throw new RuntimeException("oops");
            }
            forced = -1;
            return b;
        }
        else if (forced == -1) {
            forced = 0;
            return -Double.POSITIVE_INFINITY;
        }
        oldB = b;
        return b;
    }

    public void myAcceptNotify() {
        if (!insert) {
            oldPosition.E(testMolecule.getChildList().getAtom(0).getPosition());
        }
        super.myAcceptNotify();
        dirty = true;
    }
    
    public void actionPerformed(IEvent event) {
        if (event instanceof MCMoveTrialCompletedEvent && ((MCMoveEvent)event).getMCMove() != this && ((MCMoveTrialCompletedEvent)event).isAccepted()) {
            dirty = true;
        }
    }

}