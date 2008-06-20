package etomica.virial.simulations;

import javax.swing.JLabel;
import javax.swing.JPanel;

import etomica.api.IAction;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.ISpecies;
import etomica.atom.iterator.ApiBuilder;
import etomica.atom.iterator.ApiIndexList;
import etomica.atom.iterator.Atomset3IteratorIndexList;
import etomica.atom.iterator.Atomset4IteratorIndexList;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.IDataInfo;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.potential.P2LennardJones;
import etomica.potential.P3BondAngle;
import etomica.potential.P4BondTorsion;
import etomica.potential.PotentialGroup;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.CompoundDimension;
import etomica.units.CompoundUnit;
import etomica.units.Dimension;
import etomica.units.DimensionRatio;
import etomica.units.Kelvin;
import etomica.units.Liter;
import etomica.units.Mole;
import etomica.units.Pixel;
import etomica.units.Quantity;
import etomica.units.Unit;
import etomica.units.UnitRatio;
import etomica.units.Volume;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;
import etomica.util.Constants.CompassDirection;
import etomica.virial.ClusterAbstract;
import etomica.virial.MCMoveClusterTorsionMulti;
import etomica.virial.MayerEGeneral;
import etomica.virial.MayerEHardSphere;
import etomica.virial.MayerGeneral;
import etomica.virial.MayerHardSphere;
import etomica.virial.SpeciesAlkane;
import etomica.virial.SpeciesFactorySiepmannSpheres;
import etomica.virial.cluster.Standard;

/**
 * Mayer sampling simulation for alkanes using the TraPPE force field.
 *   M.G. Martin and J.I. Siepmann, "Transferable Potentials for Phase
 *   Equilibria. 1. United-Atom Description of n-Alkanes," J. Phys. Chem. B
 *   102, 2569-2577 (1998)
 */
public class VirialAlkane {


    public static void main(String[] args) {
        VirialSiepmannSpheresParam params = new VirialSiepmannSpheresParam();
        if (args.length > 0) {
            ReadParameters paramReader = new ReadParameters(args[0], params);
            paramReader.readParameters();
        }
        final int nPoints = params.nPoints;
        int nSpheres = params.nSpheres;
        double temperature = params.temperature;
        long steps = params.numSteps;
        double sigmaCH2 = 3.95;
        double sigmaCH3 = 3.75;
        double sigmaHSRef = sigmaCH3 + 0.5*nSpheres;
        final double[] HSB = new double[8];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);
        HSB[7] = Standard.B7HS(sigmaHSRef);
        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B2HS: "+HSB[2]);
        System.out.println("B3HS: "+HSB[3]+" = "+(HSB[3]/(HSB[2]*HSB[2]))+" B2HS^2");
        System.out.println("B4HS: "+HSB[4]+" = "+(HSB[4]/(HSB[2]*HSB[2]*HSB[2]))+" B2HS^3");
        System.out.println("B5HS: "+HSB[5]+" = 0.110252 B2HS^4");
        System.out.println("B6HS: "+HSB[6]+" = 0.03881 B2HS^5");
        System.out.println("B7HS: "+HSB[7]+" = 0.013046 B2HS^6");
		
        Space space = Space3D.getInstance();
        
        MayerHardSphere fRef = new MayerHardSphere(space,sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(space,sigmaHSRef);
        PotentialGroup pTargetGroup = new PotentialGroup(2, space);
        System.out.println("Siepman "+nSpheres+"-mer chains B"+nPoints+" at "+temperature+"K");
        temperature = Kelvin.UNIT.toSim(temperature);
        double epsilonCH2 = Kelvin.UNIT.toSim(46.0);
        double epsilonCH3 = Kelvin.UNIT.toSim(98.0);
        double epsilonCH2CH3 = Math.sqrt(epsilonCH2*epsilonCH3);
        P2LennardJones p2CH2 = new P2LennardJones(space, sigmaCH2, epsilonCH2);
        P2LennardJones p2CH3 = new P2LennardJones(space, sigmaCH3, epsilonCH3);
        P2LennardJones p2CH2CH3 = new P2LennardJones(space, 0.5*(sigmaCH2+sigmaCH3), epsilonCH2CH3);
        
        MayerGeneral fTarget = new MayerGeneral(pTargetGroup);
        MayerEGeneral eTarget = new MayerEGeneral(pTargetGroup);
        ClusterAbstract targetCluster = Standard.virialCluster(nPoints, fTarget, nPoints>3, eTarget, true);
        targetCluster.setTemperature(temperature);
        
        ClusterAbstract refCluster = Standard.virialCluster(nPoints, fRef, nPoints>3, eRef, true);
        refCluster.setTemperature(temperature);

        System.out.println((steps*1000)+" steps ("+steps+" blocks of 1000)");
        final SimulationVirialOverlap sim = new SimulationVirialOverlap(space,new SpeciesFactorySiepmannSpheres(space, nSpheres),
                          temperature,refCluster,targetCluster, nSpheres > 2);
//        sim.integratorOS.setAdjustStepFreq(false);
//        sim.integratorOS.setStepFreq0(1);

        SpeciesAlkane species = (SpeciesAlkane)sim.species;
        IAtomTypeLeaf typeCH3 = species.getChildType(0);
        IAtomTypeLeaf typeCH2 = species.getChildType(1);
        pTargetGroup.addPotential(p2CH2, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH2, typeCH2}));
        // CH2 on molecule1 to CH3 on molecule2
        pTargetGroup.addPotential(p2CH2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH2, typeCH3}));
        pTargetGroup.addPotential(p2CH2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH3, typeCH2}));
        pTargetGroup.addPotential(p2CH3, ApiBuilder.makeIntergroupTypeIterator(new IAtomTypeLeaf[]{typeCH3, typeCH3}));
        
        sim.integratorOS.setNumSubSteps(1000);

        // create the intramolecular potential here, add to it and add it to
        // the potential master if needed
        PotentialGroup pIntra = sim.integrators[1].getPotential().makePotentialGroup(1);
        if (nSpheres > 2) {
            P3BondAngle p3 = new P3BondAngle(space);
            p3.setAngle(Math.PI*114.0/180.0);
            p3.setEpsilon(Kelvin.UNIT.toSim(62500));
            int[][] triplets = new int[nSpheres-2][3];
            for (int i=0; i<nSpheres-2; i++) {
                triplets[i][0] = i;
                triplets[i][1] = i+1;
                triplets[i][2] = i+2;
            }
            pIntra.addPotential(p3, new Atomset3IteratorIndexList(triplets));
            // integrators share a common potentialMaster.  so just add to one
            sim.integrators[1].getPotential().addPotential(pIntra,new ISpecies[]{sim.species});
            
            sim.mcMoveWiggle[0].setBondLength(1.54);
            sim.mcMoveWiggle[1].setBondLength(1.54);
        }
        MCMoveClusterTorsionMulti[] torsionMoves = null;
        if (nSpheres > 3) {
            P4BondTorsion p4 = new P4BondTorsion(space, 0, Kelvin.UNIT.toSim(355.03), Kelvin.UNIT.toSim(-68.19), Kelvin.UNIT.toSim(791.32));
            int[][] quads = new int[nSpheres-3][4];
            for (int i=0; i<nSpheres-3; i++) {
                quads[i][0] = i;
                quads[i][1] = i+1;
                quads[i][2] = i+2;
                quads[i][3] = i+3;
            }
            pIntra.addPotential(p4, new Atomset4IteratorIndexList(quads));
            torsionMoves = new MCMoveClusterTorsionMulti[2];
            torsionMoves[0] = new MCMoveClusterTorsionMulti(sim.integrators[1].getPotential(), space, sim.getRandom(), 1.0, nPoints, p4, 40);
            torsionMoves[0].setTemperature(temperature);
            sim.integrators[0].getMoveManager().addMCMove(torsionMoves[0]);
            torsionMoves[1] = new MCMoveClusterTorsionMulti(sim.integrators[1].getPotential(), space, sim.getRandom(), 1.0, nPoints, p4, 40);
            torsionMoves[1].setTemperature(temperature);
            sim.integrators[1].getMoveManager().addMCMove(torsionMoves[1]);
        }
        if (nSpheres > 4) {
            pIntra.addPotential(p2CH3,new ApiIndexList(new int[][]{{0,nSpheres-1}}));
        }
        if (nSpheres > 5) {
            int[][] pairs = new int[2*(nSpheres-5)][2];
            for (int i=0; i<nSpheres-5; i++) {
                pairs[2*i][0] = 0;
                pairs[2*i][1] = nSpheres-2-i;
                pairs[2*i+1][0] = nSpheres-1;
                pairs[2*i+1][1] = i+1;
            }
            pIntra.addPotential(p2CH2CH3,new ApiIndexList(pairs));
        }
        if (nSpheres > 6) {
            int[][] pairs = new int[(nSpheres-6)*(nSpheres-5)/2][2];
            int k = 0;
            for (int i=1; i<nSpheres-5; i++) {
                for (int j=i+4; j<nSpheres-1; j++) {
                    pairs[k][0] = i;
                    pairs[k][1] = j;
                    k++;
                }
            }
            pIntra.addPotential(p2CH2,new ApiIndexList(pairs));
        }

        if (false) {
            double size = (nSpheres+5)*1.5;
            sim.box[0].getBoundary().setDimensions(space.makeVector(new double[]{size,size,size}));
            sim.box[1].getBoundary().setDimensions(space.makeVector(new double[]{size,size,size}));
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space);
            simGraphic.getDisplayBox(sim.box[0]).setPixelUnit(new Pixel(300.0/size));
            simGraphic.getDisplayBox(sim.box[1]).setPixelUnit(new Pixel(300.0/size));
            simGraphic.getDisplayBox(sim.box[0]).setShowBoundary(false);
            simGraphic.getDisplayBox(sim.box[1]).setShowBoundary(false);
            
            ((IAtomTypeSphere)typeCH2).setDiameter(sigmaCH2);
            ((IAtomTypeSphere)typeCH3).setDiameter(sigmaCH3);
            simGraphic.makeAndDisplayFrame();

            sim.integratorOS.setNumSubSteps(1000);
            sim.setAccumulatorBlockSize(1000);
                
            // if running interactively, set filename to null so that it doens't read
            // (or write) to a refpref file
            sim.getController().removeAction(sim.ai);
            sim.getController().addAction(new IAction() {
                public void actionPerformed() {
                    sim.initRefPref(null, 10);
                    sim.equilibrate(null, 20);
                    sim.ai.setMaxSteps(Long.MAX_VALUE);
                }
            });
            sim.getController().addAction(sim.ai);
            if ((Double.isNaN(sim.refPref) || Double.isInfinite(sim.refPref) || sim.refPref == 0)) {
                throw new RuntimeException("Oops");
            }
            
            final DisplayTextBox averageBox = new DisplayTextBox("Average", Quantity.SIM_UNIT);
            final DisplayTextBox errorBox = new DisplayTextBox("Error", Quantity.SIM_UNIT);
            JLabel jLabelPanelParentGroup = new JLabel("B"+nPoints+" (L/mol)^"+(nPoints-1));
            final JPanel panelParentGroup = new JPanel(new java.awt.BorderLayout());
            panelParentGroup.add(jLabelPanelParentGroup,CompassDirection.NORTH.toString());
            panelParentGroup.add(averageBox.graphic(), java.awt.BorderLayout.WEST);
            panelParentGroup.add(errorBox.graphic(), java.awt.BorderLayout.EAST);
            simGraphic.getPanel().controlPanel.add(panelParentGroup, SimulationPanel.getVertGBC());
            
            IAction pushAnswer = new IAction() {
                public void actionPerformed() {
                    double ratio = sim.dsvo.getDataAsScalar() * HSB[nPoints];
                    double error = sim.dsvo.getError() * HSB[nPoints];
                    data.x = ratio;
                    averageBox.putData(data);
                    data.x = error;
                    errorBox.putData(data);
                }
                
                DataDouble data = new DataDouble();
            };
            IDataInfo dataInfo = new DataDouble.DataInfoDouble("B"+nPoints, new CompoundDimension(new Dimension[]{new DimensionRatio(Volume.DIMENSION, Quantity.DIMENSION)}, new double[]{nPoints-1}));
            Unit unit = new CompoundUnit(new Unit[]{new UnitRatio(Liter.UNIT, Mole.UNIT)}, new double[]{nPoints-1});
            averageBox.putDataInfo(dataInfo);
            averageBox.setLabel("average");
            averageBox.setUnit(unit);
            errorBox.putDataInfo(dataInfo);
            errorBox.setLabel("error");
            errorBox.setPrecision(2);
            errorBox.setUnit(unit);
            sim.integratorOS.addIntervalAction(pushAnswer);
            sim.integratorOS.setActionInterval(pushAnswer, 1);
            
            return;
        }
        
        // if running interactively, don't use the file
        String refFileName = args.length > 0 ? "refpref"+nPoints+"_"+temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
        sim.initRefPref(refFileName, steps/40);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, steps/20);
        
        sim.setAccumulatorBlockSize((int)steps);
        
        System.out.println("equilibration finished");
        System.out.println("MC Move step sizes (ref)    "+sim.mcMoveTranslate[0].getStepSize()+" "
                +sim.mcMoveRotate[0].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[0].getStepSize())));
        System.out.println("MC Move step sizes (target) "+sim.mcMoveTranslate[1].getStepSize()+" "
                +sim.mcMoveRotate[1].getStepSize()+" "
                +(sim.mcMoveWiggle==null ? "" : (""+sim.mcMoveWiggle[1].getStepSize())));
        if (nSpheres > 3) {
            System.out.println("Torsion move acceptance "+torsionMoves[0].getTracker().acceptanceRatio()+" "+
                    torsionMoves[0].getTracker().acceptanceRatio());
        }
        
        IAction progressReport = new IAction() {
            public void actionPerformed() {
                System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                double ratio = sim.dsvo.getDataAsScalar();
                double error = sim.dsvo.getError();
                System.out.println("abs average: "+ratio*HSB[nPoints]+", error: "+error*HSB[nPoints]);
            }
        };
//        sim.integratorOS.addIntervalAction(progressReport);
//        sim.integratorOS.setActionInterval(progressReport, (int)(steps/10));

        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();

        System.out.println("final reference step frequency "+sim.integratorOS.getStepFreq0());
        System.out.println("actual reference step frequency "+sim.integratorOS.getActualStepFreq0());
        
        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("abs average: "+ratio*HSB[nPoints]+", error: "+error*HSB[nPoints]);
        DataGroup allYourBase = (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("hard sphere ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("hard sphere   average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("hard sphere overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints()-sim.dsvo.minDiffLocation()-1);
        System.out.println("chain ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("chain average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("chain overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
	}

    /**
     * Inner class for parameters
     */
    public static class VirialSiepmannSpheresParam extends ParameterBase {
        public int nPoints = 2;
        public int nSpheres = 8;
        public double temperature = 500.0;   // Kelvin
        public long numSteps = 10000;
    }
}
