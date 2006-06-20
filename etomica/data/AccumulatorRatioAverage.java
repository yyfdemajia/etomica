package etomica.data;

import etomica.data.types.DataArithmetic;
import etomica.data.types.DataGroup;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.data.types.DataGroup.DataInfoGroupFactory;
import etomica.simulation.Simulation;
import etomica.units.Null;
import etomica.util.Arrays;
import etomica.util.Function;

/**
 * Accumulator for calculating ratio between two sums
 */
public class AccumulatorRatioAverage extends AccumulatorAverage {
    
    public AccumulatorRatioAverage(Simulation sim) {
        this(sim.getDefaults().blockSize);
    }
    
    public AccumulatorRatioAverage(int blockSize) {
        super(blockSize);
        ratioTag = new DataTag();
        ratioStandardDeviationTag = new DataTag();
        ratioErrorTag = new DataTag();
    }
    
    public Object getTag(StatType statType) {
        if (statType == StatType.RATIO) {
            return ratioTag;
        }
        if (statType == StatType.RATIO_STANDARD_DEVIATION) {
            return ratioStandardDeviationTag;
        }
        if (statType == StatType.RATIO_ERROR) {
            return ratioError;
        }
        return super.getTag(statType);
    }
    
    public Data getData() {
        if (sum == null) return null;
        if (count > 0) {
            super.getData();

            double average0 = average.getValue(0);
            if (average0 == 0) {
                ratio.E(Double.NaN);
                ratioError.E(Double.NaN);
                ratioStandardDeviation.E(Double.NaN);
                return dataGroup;
            }

            ratio.E(sum);
            ratio.TE(1/sum.getValue(0));

            double errorRatio0 = error.getValue(0)/average0;
            errorRatio0 *= errorRatio0;
            ratioError.E(error);
            ratioError.DE(average);
            ratioError.TE(ratioError);
            ratioError.PE(errorRatio0);
            ratioError.map(Function.Sqrt.INSTANCE);
            ratioError.TE(ratio);
            ratioError.map(Function.Abs.INSTANCE);

            double stdevRatio0 = standardDeviation.getValue(0)/average0;
            ratioStandardDeviation.E(standardDeviation);
            ratioStandardDeviation.DE(average);
            ratioStandardDeviation.TE(ratioStandardDeviation);
            ratioStandardDeviation.PE(stdevRatio0);
            ratioStandardDeviation.map(Function.Sqrt.INSTANCE);
            ratioStandardDeviation.TE(ratio);
            ratioStandardDeviation.map(Function.Abs.INSTANCE);
        }
        return dataGroup;
    }

    public void reset() {
        if (sum == null || ratio == null) {
            //no data has been added yet, so nothing to reset
            return;
        }
        super.reset();
        ratio.E(Double.NaN);
        ratioError.E(Double.NaN);
        ratioStandardDeviation.E(Double.NaN);
    }
    
    public DataInfo processDataInfo(DataInfo incomingDataInfo) {
        super.processDataInfo(incomingDataInfo);

        ratio = (DataArithmetic)incomingDataInfo.makeData();
        ratioError = (DataArithmetic)incomingDataInfo.makeData();
        ratioStandardDeviation = (DataArithmetic)incomingDataInfo.makeData();

        Data[] dataGroups = new Data[dataGroup.getNData()+3];
        int i;
        for (i=0; i<dataGroup.getNData(); i++) {
            dataGroups[i] = dataGroup.getData(i);
        }
        dataGroups[i++] = ratio;
        dataGroups[i++] = ratioError;
        dataGroups[i++] = ratioStandardDeviation;
        dataGroup = new DataGroup(dataGroups);
        reset();

        DataInfoGroupFactory groupFactory = (DataInfoGroupFactory)dataInfo.getFactory();
        DataInfo[] subDataInfo = groupFactory.getSubDataInfo();

        DataInfoFactory factory = incomingDataInfo.getFactory();
        String incomingLabel = incomingDataInfo.getLabel();
        factory.setLabel(incomingLabel+" ratio");
        DataInfo ratioInfo = factory.makeDataInfo();
        ratioInfo.addTag(ratioTag);
        factory.setLabel(incomingLabel+" ratio error");
        DataInfo ratioErrorInfo = factory.makeDataInfo();
        ratioErrorInfo.addTag(ratioErrorTag);
        factory.setLabel(incomingLabel+" ratio");
        DataInfo ratioStandardDeviationInfo = factory.makeDataInfo();
        ratioStandardDeviationInfo.addTag(ratioStandardDeviationTag);
        
        subDataInfo = (DataInfo[])Arrays.addObject(subDataInfo, ratioInfo);
        subDataInfo = (DataInfo[])Arrays.addObject(subDataInfo, ratioErrorInfo);
        subDataInfo = (DataInfo[])Arrays.addObject(subDataInfo, ratioStandardDeviationInfo);
        groupFactory.setSubDataInfo(subDataInfo);

        dataInfo = groupFactory.makeDataInfo();
        return dataInfo;
    }
    
    public static class StatType extends AccumulatorAverage.StatType {
        protected StatType(String label, int index) {super(label,index);}       
        public static final StatType RATIO = new StatType("Ratio",6);
        public static final StatType RATIO_ERROR = new StatType("Ratio error",7);
        public static final StatType RATIO_STANDARD_DEVIATION = new StatType("Ratio standard deviation",8);
        public static AccumulatorAverage.StatType[] choices() {
            AccumulatorAverage.StatType[] choices = AccumulatorAverage.StatType.choices();
            return new AccumulatorAverage.StatType[] {
                choices[0], choices[1], choices[2], choices[3], choices[4], choices[5],
                RATIO, RATIO_ERROR, RATIO_STANDARD_DEVIATION};
        }
    }

    //need separate fields because ratio values are calculated from the non-ratio values.
    protected DataArithmetic ratio, ratioStandardDeviation, ratioError;
    private final DataTag ratioTag, ratioStandardDeviationTag, ratioErrorTag;
}
