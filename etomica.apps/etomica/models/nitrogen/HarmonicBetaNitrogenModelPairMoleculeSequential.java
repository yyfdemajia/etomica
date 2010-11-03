package etomica.models.nitrogen;

import java.io.FileWriter;
import java.io.IOException;

import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.MoleculePair;
import etomica.box.Box;
import etomica.data.types.DataTensor;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisHcp;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveHexagonal;
import etomica.normalmode.BasisBigCell;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformablePeriodic;
import etomica.space.ISpace;
import etomica.space3d.Space3D;
import etomica.units.Degree;

/**
 * 
 * Beta-phase Nitrogen Harmonic Approximation
 * with only 3 translational degrees of freedom
 * 
 * @author Tai Boon Tan
 *
 */
public class HarmonicBetaNitrogenModelPairMoleculeSequential extends Simulation{
	
	public HarmonicBetaNitrogenModelPairMoleculeSequential(ISpace space, int numMolecule, double density) {
		super(space);
		this.space = space;
		
		potentialMaster = new PotentialMaster();
		
	  	double ratio = 1.631;
		double aDim = Math.pow(4.0/(Math.sqrt(3.0)*ratio*density), 1.0/3.0);
		double cDim = aDim*ratio;
		System.out.println("\naDim: " + aDim + " ;cDim: " + cDim);
		int nCell = (int)Math.pow(numMolecule/1.999999999, 1.0/3.0);
		
		Basis basisHCP = new BasisHcp();
		BasisBigCell basis = new BasisBigCell(space, basisHCP, new int[]{nCell,nCell,nCell});
        
		IVector[] boxDim = new IVector[3];
		boxDim[0] = space.makeVector(new double[]{nCell*aDim, 0, 0});
		boxDim[1] = space.makeVector(new double[]{-nCell*aDim*Math.cos(Degree.UNIT.toSim(60)), nCell*aDim*Math.sin(Degree.UNIT.toSim(60)), 0});
		boxDim[2] = space.makeVector(new double[]{0, 0, nCell*cDim});
		
		int[] nCells = new int[]{1,1,1};
		Boundary boundary = new BoundaryDeformablePeriodic(space, boxDim);
		Primitive primitive = new PrimitiveHexagonal(space, nCell*aDim, nCell*cDim);
		
		SpeciesN2 species = new SpeciesN2(space);
		addSpecies(species);
		
		box = new Box(space);
		addBox(box);
		box.setNMolecules(species, numMolecule);		
		
		coordinateDef = new CoordinateDefinitionNitrogen(this, box, primitive, basis, space, 0);
		coordinateDef.setIsBeta();
		coordinateDef.setOrientationVectorBeta(space);
		coordinateDef.initializeCoordinates(nCells);
		
		box.setBoundary(boundary);
		double rCScale = 0.475;
		double rC = aDim*nCell*rCScale;
		System.out.println("Truncation Radius (" + rCScale +" Box Length): " + rC);
		
		potential = new P2Nitrogen(space, rC);
		potential.setBox(box);

		potentialMaster.addPotential(potential, new ISpecies[]{species, species});

		int xSites = 2*nCell+1;
		int ySites = 4*nCell+1;
		int zSites = 2*nCell+1;
		pairMatrix = new double[xSites][ySites][zSites][4][4][3][3];
	}
	
	public double[][] get2ndDerivative(int molec0){
	
		int numMolecule = box.getMoleculeList().getMoleculeCount();
		int dofTrans = 3;
		double[][] array = new double[dofTrans][dofTrans*numMolecule];
		
		FindPairMoleculeIndexBetaN2 findPair = new FindPairMoleculeIndexBetaN2(space, coordinateDef);
		
		DataTensor transTensor = new DataTensor(space);
		MoleculePair pair = new MoleculePair();	
		/*
		 *	Constructing the upper diagonal of the matrix
		 *	(Skipping the molec1 == molec2) 
		 */
		IMolecule molecule0 = coordinateDef.getBox().getMoleculeList().getMolecule(molec0);
		pair.atom0 = molecule0;
		
		boolean isReverseOrder = false;
		for(int molec1=0; molec1<numMolecule; molec1++){
			if(molec0 == molec1) continue;
			
			if(molec0 > molec1){
				isReverseOrder = true;
			} else {
				isReverseOrder = false;
			}
			
			
			// Analytical calculation for 3x3 Translational second Derivative
			pair.atom1 = coordinateDef.getBox().getMoleculeList().getMolecule(molec1);
		
			int[] index = findPair.getPairMoleculesIndex(pair.atom0, pair.atom1, isReverseOrder);
			boolean isNewPair = findPair.getIsNewPair(index);
			
			if(isNewPair){
				transTensor.E(potential.secondDerivative(pair));
				for(int i=0; i<3; i++){
					for(int j=0; j<3; j++){
						array[i][molec1*dofTrans + j] = transTensor.x.component(i, j);
						pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec1*dofTrans + j];
					}
				}
				
				findPair.updateNewMoleculePair(index);
			} else {
				if(isReverseOrder){
					for(int i=0; i<3; i++){
						for(int j=0; j<3; j++){
							array[i][molec1*dofTrans + j] 
							                            = pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][j][i];
						}
					}
				} else {
					for(int i=0; i<3; i++){
						for(int j=0; j<3; j++){
							array[i][molec1*dofTrans + j] 
							                            = pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j];
						}
					}
					
				}
			}
		}
			
      	/*
      	 *  SELF-TERM
      	 *  
      	 *  The diagonal-element block are all the same
      	 *  The 3x3 translation block is found by summing all the interactions with molecules in the system
      	 *   however, the cross and rotation block have be determined numerically 
      	 */

		int[] index = findPair.getPairMoleculesIndex(molecule0, molecule0, false);
		boolean isNewPair = findPair.getIsNewPair(index);
		
		if(isNewPair){
			for(int molec1=0; molec1<numMolecule; molec1++){
    			if(molec0==molec1) continue; // we might double sum the elements in array[a][a] if we don't skip the pair
    			for(int i=0; i<3; i++){
    				for (int j=0; j<3; j++){
    					array[i][molec0*dofTrans + j] -= array[i][molec1*dofTrans + j] ;         				
	    				
    				}
    			}
    		}
			
			for(int i=0; i<3; i++){
   				for (int j=0; j<3; j++){
   					pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][j] = array[i][molec0*dofTrans + j];
   				}
			}
			
    	
		} else {
				
			for(int i=0; i<3; i++){
   				for (int j=0; j<3; j++){
   					array[i][molec0*dofTrans + j]= pairMatrix[index[0]][index[1]][index[2]][index[3]][index[4]][i][molec0*dofTrans + j];
   				}
			}
			
		}
		return array;
		
	}
	
	public void constructHessianMatrix(String fname, int nCell){
		
		int numMolecules = nCell*nCell*nCell*2;
		int interval = nCell*2;
		double[][][] array = new double[interval][3][coordinateDef.getCoordinateDim()];
		
		try {
			FileWriter fileWriter = new FileWriter(fname);
			
			for (int iMol=0; iMol<numMolecules; iMol+=interval){
				for(int i=0; i<interval; i++){
					array[i] = get2ndDerivative(iMol+i);
				}
				for(int i=0; i<interval; i++){
					for (int iRow=0; iRow< array[0].length; iRow++){
						for (int jCol=0; jCol< array[0][0].length; jCol++){
							double value = array[i][iRow][jCol];
//							if(Math.abs(value) < 1e-6){
//								value = 0.0;
//							}
							fileWriter.write(value+ " ");
						}
						fileWriter.write("\n");
					}
				}
			}
			
			fileWriter.close();
			
		} catch (IOException e) {
			
		}
	
	}
	
	public static void main (String[] args){
		
		int nCell = 4;
		int numMolecule = nCell*nCell*nCell*2;
		double density = 0.025;
		
		if(args.length > 0){
			nCell = Integer.parseInt(args[0]);
		}
		System.out.println("Running simulation to construct Hessian Matrix for beta-phase nitrogen");
		System.out.println("with numMolecule of "+numMolecule + " at density of " + density);
		
		HarmonicBetaNitrogenModelPairMoleculeSequential test = new HarmonicBetaNitrogenModelPairMoleculeSequential(Space3D.getInstance(3), numMolecule, density);
		
		long startTime = System.currentTimeMillis();
		String filename = new String ("beta"+numMolecule+"_2ndDer_d"+density+"_new");

		test.constructHessianMatrix(filename, nCell);
		
		long endTime = System.currentTimeMillis();
		System.out.println("Time taken (s): " + (endTime-startTime)/1000);
	}
	
	
	protected Box box;
	protected ISpace space;
	protected P2Nitrogen potential;
	protected CoordinateDefinitionNitrogen coordinateDef;
	protected PotentialMaster potentialMaster;
	protected double[][][][][][][] pairMatrix;
	private static final long serialVersionUID = 1L;
}
