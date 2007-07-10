package etomica.data.meter;

import etomica.EtomicaInfo;
import etomica.atom.IAtom;
import etomica.data.Data;
import etomica.data.DataSourceAtomic;
import etomica.data.DataSourceScalar;
import etomica.data.IDataInfo;
import etomica.box.Box;
import etomica.species.Species;
import etomica.units.Quantity;

/**
 * Meter for recording the total number of molecules in the box
 */
public class MeterNMolecules extends DataSourceScalar implements DataSourceAtomic {
    
    private static final long serialVersionUID = 1L;
    private Species species;
    
    public MeterNMolecules() {
        super("Molecules",Quantity.DIMENSION);
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Number of molecules in a box");
        return info;
    }

    public void setSpecies(Species s) {species = s;}
    public Species getSpecies() {return species;}

    public double getDataAsScalar() {
        if (box == null) throw new IllegalStateException("must call setBox before using meter");
        return (species == null) ? box.moleculeCount(): box.getNMolecules(species);
    }
    
    public Data getData(IAtom atom) {
        data.x = (species == null || atom.getType().getSpecies() == species) ? 1 : 0;
        return data;
    }
    
    public IDataInfo getAtomDataInfo() {
        return dataInfo;
    }
    
    /**
     * @return Returns the box.
     */
    public Box getBox() {
        return box;
    }
    /**
     * @param box The box to set.
     */
    public void setBox(Box box) {
        this.box = box;
    }

    private Box box;
}
