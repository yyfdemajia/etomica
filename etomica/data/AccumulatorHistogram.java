/*
 * History
 * Created on Aug 4, 2004 by kofke
 */
package etomica.data;

import etomica.data.types.CastToDoubleArray;
import etomica.data.types.DataArithmetic;
import etomica.data.types.DataFunction;
import etomica.data.types.DataGroup;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.units.Null;
import etomica.util.Histogram;
import etomica.util.HistogramCollapsing;

/**
 * Accumulator that keeps histogram of data.
 * <p>
 * Input Data must implement DataArithmetic.
 */
public class AccumulatorHistogram extends DataAccumulator {

    /**
     * Creates instance using HistogramSimple factory and specifying histograms
     * having 100 bins.
     */
    public AccumulatorHistogram() {
        this(HistogramCollapsing.FACTORY);
    }

    /**
     * Creates instance using given histogram factory with default nBins of 100.
     */
    public AccumulatorHistogram(Histogram.Factory factory) {
        this(factory, 100);
    }

    /**
     * Creates instance using the given histogram factory making histograms having
     * the given number of bins.
     */
    public AccumulatorHistogram(Histogram.Factory factory, int nBins) {
        this.nBins = nBins;
        histogramFactory = factory;
        tag = new DataTag();
    }

    public DataTag getTag() {
        return tag;
    }
    
    /**
     * Adds each value in the given Data to its own histogram.
     */
    protected void addData(Data inputData) {
        DataArithmetic values = (DataArithmetic)inputData;
        for (int i = nData-1; i >= 0; i--) {
            histogram[i].addValue(values.getValue(i));
        }
    }

    /**
     * Returns the set of histograms.
     */
    public Data getData() {
        // check to see if current data functions are the right length
        // histogram might change the number of bins on its own.
        boolean success = true;
        for (int i=0; i<nData; i++) {
            DataFunction dataFunction = (DataFunction)data.getData(i);
            // covertly call getHistogram() here.  We have to call it anyway
            // so that the array data gets updated.
            if (dataFunction.getData() != histogram[i].getHistogram() ||
                    xDataSources[i].getIndependentData(0).getData() != histogram[i].xValues()) {
                success = false;
            }
        }
        
        if (!success) {
            DataFunction[] dataFunctions = new DataFunction[nData];
            DataInfoFunction[] dataInfoFunctions = new DataInfoFunction[nData];
            // attempt to re-use old DataFunctions
            for (int i=0; i<nData; i++) {
                DataFunction dataFunction = (DataFunction)data.getData(i);
                DataInfoFunction dataInfoFunction = (DataInfoFunction)((DataInfoGroup)dataInfo).getSubDataInfo(i);
                if (dataFunction.getData() == histogram[i].getHistogram() &&
                        xDataSources[i].getIndependentData(0).getData() == histogram[i].xValues()) {
                    dataFunctions[i] = dataFunction;
                    dataInfoFunctions[i] = dataInfoFunction;
                }
                else {
                    DataInfoDoubleArray independentInfo = new DataInfoDoubleArray(binnedDataInfo.getLabel(),binnedDataInfo.getDimension(),new int[]{histogram[i].getNBins()});
                    dataFunctions[i] = new DataFunction(new int[]{histogram[i].getNBins()}, histogram[i].getHistogram());
                    xDataSources[i] = new DataSourceIndependentSimple(histogram[i].xValues(), independentInfo);
                    dataInfoFunctions[i] = new DataInfoFunction(binnedDataInfo.getLabel()+" Histogram", Null.DIMENSION, xDataSources[i]);
                }
            }
            // creating a new data instance might confuse downstream data sinks, but
            // we have little choice and they should deal.
            data = new DataGroup(dataFunctions);
            dataInfo = new DataInfoGroup("Histogram", binnedDataInfo.getDimension(), dataInfoFunctions);

            // if we have have our own DataSink, we need to notify it that something changed.
            if(dataSink != null) {
                dataSink.putDataInfo(dataInfo);
            }
        }
        
        return data;
    }
    
    /**
     * Sets up data and histograms, discarding any previous results.
     */
    protected DataInfo processDataInfo(DataInfo inputDataInfo) {
        binnedDataInfo = inputDataInfo;
        nData = ((DataInfoDoubleArray)inputDataInfo).getArrayLength();
        histogram = new Histogram[nData];
        for (int i = 0; i < nData; i++) {
            histogram[i] = histogramFactory.makeHistogram();
            histogram[i].setNBins(nBins);
        }
        setupData();
        return dataInfo;
    }
    
    /**
     * Returns caster that ensures accumulator will receive a DataDoubleArray.
     */
    public DataProcessor getDataCaster(DataInfo inputDataInfo) {
        if(inputDataInfo instanceof DataInfoDoubleArray) {
            return null;
        }
        return new CastToDoubleArray();
    }

    /**
     * Constructs the Data objects used by this class.
     */
    private void setupData() {
        DataFunction[] dataFunctions = new DataFunction[nData];
        DataInfoFunction[] dataInfoFunctions = new DataInfoFunction[nData];
        xDataSources = new DataSourceIndependentSimple[nData];
        for (int i=0; i<nData; i++) {
            DataInfoDoubleArray independentInfo = new DataInfoDoubleArray(binnedDataInfo.getLabel(),binnedDataInfo.getDimension(),new int[]{histogram[i].getNBins()});
            dataFunctions[i] = new DataFunction(new int[]{histogram[i].getNBins()}, histogram[i].getHistogram());
            xDataSources[i] = new DataSourceIndependentSimple(histogram[i].xValues(), independentInfo);
            dataInfoFunctions[i] = new DataInfoFunction(binnedDataInfo.getLabel()+" Histogram", Null.DIMENSION, xDataSources[i]);
            dataInfoFunctions[i].addTags(binnedDataInfo.getTags());
        }
        data = new DataGroup(dataFunctions);
        dataInfo = new DataInfoGroup("Histogram", binnedDataInfo.getDimension(), dataInfoFunctions);
        dataInfo.addTag(tag);
    }
    
    /**
     * @return the number of bins in each histogram.
     */
    public int getNBins() {
        return nBins;
    }

    /**
     * Sets the number of bins in each histogram. Calls setNBins method of
     * current histograms, which will discard data or modify themselves
     * depending on how they are defined.
     */
    public void setNBins(int nBins) {
        this.nBins = nBins;
        for (int i = 0; i < nData; i++) {
            histogram[i].setNBins(nBins);
        }
        setupData();
        if(dataSink != null) {
            dataSink.putDataInfo(dataInfo);
        }
    }

    /**
     * Zeros histograms, discarding any previous contributions. 
     */
    public void reset() {
        for (int i = 0; i < nData; i++)
            histogram[i].reset();
    }
    
    public Histogram[] getHistograms() {
        return histogram;
    }

    /**
     * Returns the DataInfo for the output Data.
     */
    public DataInfo getDataInfo() {
        return dataInfo;
    }
    
    protected Histogram[] histogram = new Histogram[0];
    protected DataSourceIndependentSimple[] xDataSources = new DataSourceIndependentSimple[0];
    private DataGroup data;
    private DataInfo binnedDataInfo;
    protected int nData;
    private Histogram.Factory histogramFactory;
    private int nBins;
    protected final DataTag tag;
}
