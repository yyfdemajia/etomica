/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.liquidLJ;

import java.io.File;

import etomica.action.WriteConfigurationBinary;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IIntegrator;
import etomica.config.ConfigurationFileBinary;
import etomica.data.AccumulatorAverageCovariance;
import etomica.data.DataFork;
import etomica.data.DataPipe;
import etomica.data.DataProcessor;
import etomica.data.DataPumpListener;
import etomica.data.DataSourceScalar;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataGroup;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.graphics.SimulationGraphic;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.Potential0Lrc;
import etomica.space3d.Space3D;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;

/**
 * Simple Lennard-Jones molecular dynamics simulation in 3D
 */
 
public class LjMd3Dv2y {
    
    public static void main(String[] args) {

        // according to http://link.aip.org/link/doi/10.1063/1.2753149
        // triple point
        // T = 0.694
        // liquid density = 0.845435
        
        // Agrawal and Kofke:
        //      small      large
        // T    0.698    0.687(4)
        // p    0.0013   0.0011
        // rho  0.854    0.850
        
        // http://link.aip.org/link/doi/10.1063/1.4758698 says
        // T = 0.7085(5)
        // P = 0.002264(17)
        // rhoL = 0.8405(3)
        // rhoFCC = 0.9587(2)
        // rhoV = 0.002298(18)

        LjMd3DParams params = new LjMd3DParams();
        ParseArgs.doParseArgs(params, args);
        if (args.length==0) {
            params.graphics = false;
            params.numAtoms = 250;
            params.steps = 100000;
            params.v2 = 0.48;
            params.rcShort = 2.5*Math.pow(params.v2, 1.0/6.0);
            params.y = 1.5;
            params.hybridInterval = 100;
        }

        final int numAtoms = params.numAtoms;
        final double y = params.y;
        final double v2 = params.v2;
        final double density = 1.0/Math.sqrt(v2);
        final double temperature = 4/(y*v2*v2);
        long steps = params.steps;
        double tStep = 0.002*Math.sqrt(y)*v2 * params.tStepFac;
        double rcShort = params.rcShort;
        boolean graphics = params.graphics;
        final int hybridInterval = params.hybridInterval;
        int nAccBlocks = params.nAccBlocks;

    	int fastInterval = hybridInterval;
        int longInterval = (numAtoms / 200 / hybridInterval) * hybridInterval;
        if (longInterval == 0) longInterval = hybridInterval;

    	if (!graphics) {
            System.out.println("Running LJ MD with N="+numAtoms+" at y="+y+" v2="+v2);
    	    System.out.println("  T="+temperature+" density="+density);
    	    System.out.println("time step: "+tStep);
    	    System.out.println(steps+" steps ("+(steps*tStep)+" time units)");
    	    System.out.println("short cutoff: "+rcShort);
    	    System.out.println("hybrid MC interval: "+hybridInterval);
            System.out.println("full energy interval: "+longInterval);
    	}

    	double L = Math.pow(numAtoms/density, 1.0/3.0);
        final LjMd3D sim = new LjMd3D(numAtoms, temperature, density, Double.NaN, tStep, rcShort, 0.494*L, hybridInterval, null);

    	if (Double.parseDouble(String.format("%4.2f", v2)) != v2) {
    	    throw new RuntimeException(String.format("you're just trying to cause trouble, use v2=%4.2f", v2));
    	}
    	String configFilename = String.format("configN%d_V%4.2f_y%4.2f", numAtoms, v2, y);
        File inConfigFile = new File(configFilename+".pos");
        if (inConfigFile.exists()) {
            ConfigurationFileBinary configFile = new ConfigurationFileBinary(configFilename);
            configFile.initializeCoordinates(sim.box);
            System.out.println("Continuing from previous configuration");
        }
        else {
            // try 8x fewer atoms
            String tmpConfigFilename = String.format("configN%d_V%4.2f_y%4.2f", numAtoms/8, v2, y);
            inConfigFile = new File(tmpConfigFilename+".pos");
            if (inConfigFile.exists()) {
                System.out.println("bringing configuration up from N="+numAtoms/8);
                ConfigurationFileBinary configFile = new ConfigurationFileBinary(tmpConfigFilename);
                ConfigurationFileBinary.replicate(configFile, sim.box, new int[]{2,2,2}, Space3D.getInstance());
            }
            else {
                // try lower temperature, then higher temperature
                boolean success = false;
                for (int i=10; i>=-10; i--) {
                    if (i==0) continue;
                    tmpConfigFilename = String.format("configN%d_V%4.2f_Y%4.2f", numAtoms, v2, y+0.01*i);
                    inConfigFile = new File(tmpConfigFilename+".pos");
                    if (inConfigFile.exists()) {
                        System.out.println("bringing configuration from y="+(y+0.01*i));
                        success = true;
                        ConfigurationFileBinary configFile = new ConfigurationFileBinary(tmpConfigFilename);
                        configFile.initializeCoordinates(sim.box);
                        break;
                    }
                }
                if (!success) {
                    // try a different density (higher density first)
                    for (int i=-10; i>=10; i--) {
                        if (i==0) continue;
                        tmpConfigFilename = String.format("configN%d_V%4.2f_y%4.2f", numAtoms, v2+0.01*i, y);
                        inConfigFile = new File(tmpConfigFilename+".pos");
                        if (inConfigFile.exists()) {
                            System.out.println("bringing configuration from v^2="+(v2+0.01*i));
                            ConfigurationFileBinary configFile = new ConfigurationFileBinary(tmpConfigFilename);
                            ConfigurationFileBinary.rescale(configFile, sim.box, 1/Math.sqrt(v2+0.01*i), Space3D.getInstance());
                            break;
                        }
                    }
                }
            }
        }
        
        sim.potentialMasterList.getNeighborManager(sim.box).reset();

        if (!graphics) {
            long eqSteps = steps/10;
            if (eqSteps < 4000) {
                eqSteps = steps/4;
                if (eqSteps > 4000) eqSteps = 4000;
            }
            sim.ai.setMaxSteps(eqSteps);
            sim.getController().actionPerformed();
            sim.getController().reset();

            System.out.println("equilibration finished ("+eqSteps+" steps)");
        }

        final MeterPotentialEnergy meterEnergyFast = new MeterPotentialEnergy(sim.potentialMasterList);
        meterEnergyFast.setBox(sim.box);
        long bs = steps/(longInterval*nAccBlocks);

        double rcMax = 0.494*L;
        double fac = 1.2;
        int nCutoffs = 1 + (int)(Math.log(rcMax/rcShort)/Math.log(fac)); 
        final double[] cutoffs = new double[nCutoffs];
        cutoffs[0] = rcShort;
        for (int i=1; i<cutoffs.length; i++) {
            cutoffs[i] = cutoffs[i-1]*1.2;
        }
        MeterPotentialEnergyCutoff meterEnergyCut = new MeterPotentialEnergyCutoff(sim.potentialMasterLongCut, sim.getSpace(), cutoffs);
        meterEnergyCut.setBox(sim.box);
        final double[] uFacCut = new double[cutoffs.length];
        IData uCut = meterEnergyCut.getData();
        double uFast0 = meterEnergyFast.getDataAsScalar();
        for (int i=0; i<cutoffs.length; i++) {
            uFacCut[i] = uCut.getValue(i) - uFast0;
        }

        final ValueCache energyFastCache = new ValueCache(meterEnergyFast, sim.integrator);

        final MeterPUCut meterPU = new MeterPUCut(sim.getSpace(), cutoffs);
        meterPU.setBox(sim.box);
        meterPU.setIncludeLrc(false);
        meterPU.setPotentialMaster(sim.potentialMasterLongCut);
        meterPU.setTemperature(temperature);
        
        bs = steps/(fastInterval*nAccBlocks);

        DataProcessorReweight puReweight = new DataProcessorReweight(temperature, energyFastCache, uFacCut, sim.box, nCutoffs);
        DataFork puFork = new DataFork();
        DataPumpListener pumpPU = new DataPumpListener(meterPU, puFork, longInterval);
        puFork.addDataSink(puReweight);
        sim.integrator.getEventManager().addListener(pumpPU);
        final AccumulatorAverageCovariance accPU = new AccumulatorAverageCovariance(bs == 0 ? 1 : bs);
        puReweight.setDataSink(accPU);

    	if (graphics) {
            final String APP_NAME = "LjMd3D";
        	final SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, 3, sim.getSpace(), sim.getController());
    
            simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
            simGraphic.getController().getDataStreamPumps().add(pumpPU);

            simGraphic.makeAndDisplayFrame(APP_NAME);

            return;
    	}
    	
    	DataProcessorReweightRatio puReweightRatio = new DataProcessorReweightRatio(nCutoffs);
    	accPU.setBlockDataSink(puReweightRatio);
    	AccumulatorAverageCovariance accPUBlocks = new AccumulatorAverageCovariance(1, true);
    	puReweightRatio.setDataSink(accPUBlocks);

    	long t1 = System.currentTimeMillis();
        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();
        long t2 = System.currentTimeMillis();

        System.out.println();

        WriteConfigurationBinary writeConfig = new WriteConfigurationBinary(sim.getSpace());
        writeConfig.setFileName(configFilename+".pos");
        writeConfig.setBox(sim.box);
        writeConfig.actionPerformed();
        
        System.out.println("hybrid acceptance: "+sim.integrator.getHybridAcceptance());
        long numNbrUpdates = sim.potentialMasterList.getNeighborManager(sim.box).getNumUpdates();
        System.out.println(String.format("avg steps between nbr update: %3.1f",((double)steps)/numNbrUpdates));

        System.out.println();

        DataGroup dataPU = (DataGroup)accPU.getData();
        IData avgPU = dataPU.getData(accPU.AVERAGE.index);
        IData errPU = dataPU.getData(accPU.ERROR.index);
        IData covPU = dataPU.getData(accPU.BLOCK_COVARIANCE.index);
        IData corPU = dataPU.getData(accPU.BLOCK_CORRELATION.index);
        
        int n = 5*cutoffs.length;
        int rcStart = params.rcStart;

        int j = 0;
        for (int i=0; i<cutoffs.length; i++) {

            P2SoftSphericalTruncated p2t = new P2SoftSphericalTruncated(sim.getSpace(), sim.potential, cutoffs[i]);
            p2t.setBox(sim.box);
            Potential0Lrc p0lrc = p2t.makeLrcPotential(new IAtomType[]{sim.species.getAtomType(0), sim.species.getAtomType(0)});
            p0lrc.setBox(sim.box);
            double ulrc = p0lrc.energy(null);

            double avgW = avgPU.getValue(j+4);
            double errW = errPU.getValue(j+4);
            double corW = corPU.getValue(j+4);

            System.out.println(String.format("rc: %d  A-Afast: % 22.15e  %10.4e  % 5.2f  %5.3f", rcStart+i, (ulrc + uFacCut[i] - temperature*Math.log(avgW))/numAtoms, temperature*errW/avgW/numAtoms, corW, errW/avgW));

            j+=5;
        }

        System.out.println();
        
        dataPU = (DataGroup)accPUBlocks.getData();
        avgPU = dataPU.getData(accPUBlocks.AVERAGE.index);
        errPU = dataPU.getData(accPUBlocks.ERROR.index);
        covPU = dataPU.getData(accPUBlocks.BLOCK_COVARIANCE.index);
        corPU = dataPU.getData(accPUBlocks.BLOCK_CORRELATION.index);
        
        n = 4*cutoffs.length;

        j = 0;
        for (int i=0; i<cutoffs.length; i++) {

            P2SoftSphericalTruncated p2t = new P2SoftSphericalTruncated(sim.getSpace(), sim.potential, cutoffs[i]);
            p2t.setBox(sim.box);
            Potential0Lrc p0lrc = p2t.makeLrcPotential(new IAtomType[]{sim.species.getAtomType(0), sim.species.getAtomType(0)});
            p0lrc.setBox(sim.box);
            double ulrc = p0lrc.energy(null);

            ulrc /= numAtoms;
            double avgU = avgPU.getValue(j+0);
            double errU = errPU.getValue(j+0);
            double corU = corPU.getValue(j+0);
            System.out.println(String.format("rc: %d  U:       % 22.15e  %10.4e  % 5.2f", rcStart+i, ulrc + avgU, errU, corU));

            double avgP = avgPU.getValue(j+1);
            double errP = errPU.getValue(j+1);
            double corP = corPU.getValue(j+1);
            double vol = sim.box.getBoundary().volume();
            double plrc = -p0lrc.virial(null)/(3*vol);
            double puCor = covPU.getValue((j+0)*n+j+1) / Math.sqrt(covPU.getValue((j+0)*n+j+0) * covPU.getValue((j+1)*n+j+1));
            System.out.println(String.format("rc: %d  P:       % 22.15e  %10.4e  % 5.2f  % 7.4f", rcStart+i, plrc + avgP, errP, corP, puCor));

            double avgDADy = avgPU.getValue(j+2);
            double errDADy = errPU.getValue(j+2);
            double corDADy = corPU.getValue(j+2);
            System.out.println(String.format("rc: %d  DADy:    % 22.15e  %10.4e  % 5.2f", rcStart+i, ulrc*Math.pow(density,-4)/4 + avgDADy, errDADy, corDADy));

            double avgDADv2 = avgPU.getValue(j+3);
            double errDADv2 = errPU.getValue(j+3);
            double corDADv2 = corPU.getValue(j+3);
            
            // -(P/(temperature*density) - 1 - 4 * U / (temperature))*density*density/2;
            double DADv2LRC = (-plrc/(temperature*density) + 4*ulrc/temperature)*density*density/2;
            double dadCor = covPU.getValue((j+2)*n+j+3) / Math.sqrt(covPU.getValue((j+2)*n+j+2) * covPU.getValue((j+3)*n+j+3));
            System.out.println(String.format("rc: %d  DADv2:   % 22.15e  %10.4e  % 5.2f  % 7.4f", rcStart+i, DADv2LRC + avgDADv2, errDADv2, corDADv2, dadCor));
            System.out.println();

            j+=4;
        }


        System.out.println("time: "+(t2-t1)/1000.0+" seconds");
    }
    
    public static class DataProcessorCorrection extends DataProcessor {
        protected DataDoubleArray data;
        protected final int nMu;
        
        public DataProcessorCorrection(int nMu) {
            this.nMu = nMu;
        }

        public DataPipe getDataCaster(IEtomicaDataInfo inputDataInfo) {return null;}

        protected IEtomicaDataInfo processDataInfo(IEtomicaDataInfo inputDataInfo) {
            dataInfo = new DataInfoDoubleArray("foo", Null.DIMENSION, new int[]{((DataInfoGroup)inputDataInfo).getSubDataInfo(AccumulatorAverageCovariance.AVERAGE.index).getLength()-nMu});
            data = new DataDoubleArray(dataInfo.getLength());
            return dataInfo;
        }

        protected IData processData(IData inputData) {
            IData avg = ((DataGroup)inputData).getData(AccumulatorAverageCovariance.AVERAGE.index);
            double[] x = data.getData();
            int nValues = avg.getLength()/nMu;
            for (int i=0; i<nMu; i++) {
                double wAvg = avg.getValue((i+1)*nValues-1);
                for (int j=0; j<nValues-1; j++) {
                    x[j+i*(nValues-1)] = avg.getValue(j+i*nValues)/wAvg;
                }
            }
            return data;
        }
    }

    public static class ValueCache {
        protected long lastStep = -1;
        protected double lastValue;
        protected final DataSourceScalar dss;
        protected final IIntegrator integrator;
        public ValueCache(DataSourceScalar dss, IIntegrator integrator) {
            this.dss = dss;
            this.integrator = integrator;
        }
        public double getValue() {
            if (integrator.getStepCount() != lastStep) {
                lastStep = integrator.getStepCount();
                lastValue = dss.getDataAsScalar();
            }
            return lastValue;
        }
    }
    

    public static class DataProcessorReweightRatio extends DataProcessor {

        protected DataDoubleArray data;
        protected int nCutoffs;
        
        public DataProcessorReweightRatio(int nCutoffs) {
            this.nCutoffs = nCutoffs;
        }

        public DataPipe getDataCaster(IEtomicaDataInfo inputDataInfo) {
            return null;
        }

        protected IData processData(IData inputData) {
            double[] x = data.getData();
            int j = 0;
            int nData = inputData.getLength()/nCutoffs-1;
            for (int i=0; i<nCutoffs; i++) {
                double w = inputData.getValue(j+i+nData);
                for (int k=0; k<nData; k++) {
                    x[j+k] = inputData.getValue(j+i+k)/w;
                }
                j += nData;
            }
            if (data.isNaN()) {
                throw new RuntimeException("oops");
            }
            return data;
        }

        protected IEtomicaDataInfo processDataInfo(IEtomicaDataInfo inputDataInfo) {
            int nData = inputDataInfo.getLength()/nCutoffs-1;
            dataInfo = new DataInfoDoubleArray("whatever", Null.DIMENSION, new int[]{nData*nCutoffs});
            data = new DataDoubleArray(dataInfo.getLength());
            return dataInfo;
        }
    }

    public static class DataProcessorReweight extends DataProcessor {
        private final double temperature;
        private final ValueCache energyFastCache;
        private final double[] uFac;
        protected DataDoubleArray data;
        protected final IBox box;
        protected final int nCutoffs;

        public DataProcessorReweight(double temperature,
                ValueCache energyFastCache,
                double[] uFac, IBox box, int nCutoffs) {
            this.temperature = temperature;
            this.energyFastCache = energyFastCache;
            this.uFac = uFac;
            this.box = box;
            this.nCutoffs = nCutoffs;
        }

        public DataPipe getDataCaster(IEtomicaDataInfo inputDataInfo) {
            return null;
        }

        protected IEtomicaDataInfo processDataInfo(IEtomicaDataInfo inputDataInfo) {
            dataInfo = new DataInfoDoubleArray("whatever", Null.DIMENSION, new int[]{(inputDataInfo.getLength()+nCutoffs)});
            data = new DataDoubleArray(dataInfo.getLength());
            return dataInfo;
        }

        protected IData processData(IData inputData) {
            double uFast = energyFastCache.getValue();
            double[] x = data.getData();
            int j = 0;
            int nData = inputData.getLength()/nCutoffs;
            int n = box.getMoleculeList().getMoleculeCount();
            for (int i=0; i<nCutoffs; i++) {
                double dx = n*inputData.getValue(j) - (uFast+uFac[i]);
                double w = Math.exp(-dx/temperature);
                for (int k=0; k<nData; k++) {
                    x[j+i+k] = inputData.getValue(j+k)*w;
                }
                x[j+i+nData] = w;
                j += inputData.getLength()/nCutoffs;
            }
            if (data.isNaN()) {
                throw new RuntimeException("oops");
            }
            return data;
        }
    }

    public static class LjMd3DParams extends ParameterBase {
        public int numAtoms = 2048;
        public double y = 1.3;
        public double v2 = 0.8;
        public double tStepFac = 1;
        public long steps = 40000;
        public int hybridInterval = 20;
        public double rcShort = 2.5;
        public int rcStart = 0;
        public boolean graphics = false;
        public int nAccBlocks = 100;
    }
}
