package etomica.association;

import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.atom.MoleculeArrayList;
import etomica.integrator.mcmove.MCMoveRotateMolecule3D;
import etomica.space.ISpace;

/**
 * Performs a rotation of a molecule that has an orientation coordinate.
 * @author Hye Min Kim
 */
public class MCMoveMoleculeRotateAssociated extends MCMoveRotateMolecule3D {
    
    private static final long serialVersionUID = 2L;
    protected AssociationManagerMolecule associationManager;
    protected final MoleculeArrayList smerList;
	protected int maxLength = Integer.MAX_VALUE;
	protected IAssociationHelperMolecule associationHelper;
	protected boolean debug;

    public MCMoveMoleculeRotateAssociated(IPotentialMaster potentialMaster, IRandom random,
    		            ISpace _space) {
        super(potentialMaster, random, _space);
        this.smerList = new MoleculeArrayList();
    }
    
    public void setAssociationManager(AssociationManagerMolecule associationManager, IAssociationHelperMolecule associationHelper){
		this.associationManager = associationManager;
		this.associationHelper = associationHelper;
    }
    
    public boolean doTrial() {//rotate any molecule
    	debug = false;
        if(box.getMoleculeList().getMoleculeCount()==0) {return false;}
        molecule = moleculeSource.getMolecule();
        associationHelper.populateList(smerList,molecule,true);
        energyMeter.setTarget(molecule);
        uOld = energyMeter.getDataAsScalar();
        double dTheta = (2*random.nextDouble() - 1.0)*stepSize;
        rotationTensor.setAxial(r0.getD() == 3 ? random.nextInt(3) : 2,dTheta);
        r0.E(positionDefinition.position(molecule));
        doTransform();
        return true;
    }
    
    public double getA(){
    	if (associationHelper.populateList(smerList,molecule,true)){//prevent invalid bonding
    		return 0.0;
    	}

    	if (smerList.getMoleculeCount() > maxLength) {
    		return 0.0;
		}
		return 1.0;
	}
}