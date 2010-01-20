package etomica.models.oneDHardRods;

import etomica.api.IRandom;
import etomica.data.DataInfo;
import etomica.data.DataSourceScalar;
import etomica.data.DataTag;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.units.Dimension;


public class MeterOverlapSameGaussian implements IEtomicaDataSource {
    DataTag tag;
    DataInfo dataInfo;
    DataSourceScalar dataSourceA, dataSourceB;
    double temperature;
    DataDoubleArray dda;
    
    MeterDifferentImageAdd meterAdd;
    
    /**
     * Put the system you are measuring in as the first DataSourceScalar
     * @param label
     * @param dimension
     * @param dataSourceA - denominator
     * @param dataSourceB - numerator
     * @param temperature
     */
    MeterOverlapSameGaussian(String label, Dimension dimension, DataSourceScalar dataSourceA,
            DataSourceScalar dataSourceB, double temperature){
        dataInfo = new DataInfoDoubleArray(label, dimension, new int[]{2});
        tag = new DataTag();
        dataInfo.addTag(tag);
        
        
        this.dataSourceA = dataSourceA;
        this.dataSourceB = dataSourceB;
        this.temperature = temperature;
        
        dda = new DataDoubleArray(2);
        
    }
    
    public DataDoubleArray getData(){
        double[] eAeB = dda.getData();
        
        double numerator = Math.exp(-dataSourceB.getDataAsScalar()/temperature);
        
        double[] gausses = ((MeterDifferentImageAdd)dataSourceB).getGaussian();
        double harmonic = 0.5 * (gausses[0] * gausses[0] + gausses[1] * gausses[1]);
        double denominator = Math.exp(-(dataSourceA.getDataAsScalar()+harmonic)/temperature);
        
        eAeB[1] = numerator / denominator;
        eAeB[0] = 1.0;
        
        return dda;
    }
    
    public IEtomicaDataInfo getDataInfo(){
        return dataInfo;
    }
    public DataTag getTag() {
        return tag;
    }
    
}