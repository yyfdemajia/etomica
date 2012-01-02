package etomica.virial.GUI.components;


import etomica.api.IPotentialMolecular;



public class CollectionPotentialMolecularUnlike implements ICollectionPotential{
	
	public int[] speciesIndex;

	public IPotentialMolecular potentialMolecularInterNonBondedUnlike;
	
	public CollectionPotentialMolecularUnlike(int index1, int index2){
		speciesIndex = new int[2];
		speciesIndex[0] = index1;
		speciesIndex[1] = index2;
		
		
	}
	
	public void setPotentialMolecularUnlike(IPotentialMolecular potentialMolecularInterNonBondedUnlike){
		this.potentialMolecularInterNonBondedUnlike = potentialMolecularInterNonBondedUnlike;
		
	}
	
	public IPotentialMolecular getPotentialMolecularUnlike(){
		return this.potentialMolecularInterNonBondedUnlike;
	}

	public int[] getSpeciesIndex() {
		return this.speciesIndex;
	}

	public void setSpeciesIndex(int[] speciesIndex) {
		this.speciesIndex = speciesIndex;
	}
	
	public int getSpeciesIndex(int index) {
		return speciesIndex[index];
	}
	

	public void setSpeciesIndex(int speciesIndex1, int speciesIndex2) {
		this.speciesIndex[0] = speciesIndex1;
		this.speciesIndex[1] = speciesIndex2;
	}
	
}
