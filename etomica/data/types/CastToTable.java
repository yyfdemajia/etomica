package etomica.data.types;

import java.io.Serializable;

import etomica.data.Data;
import etomica.data.DataInfo;
import etomica.data.DataProcessor;
import etomica.data.DataSourceIndependent;
import etomica.data.DataTag;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.data.types.DataInteger.DataInfoInteger;
import etomica.data.types.DataTable.DataInfoTable;
import etomica.data.types.DataTensor.DataInfoTensor;
import etomica.data.types.DataVector.DataInfoVector;
import etomica.space.Tensor;

/**
 * 
 * A DataProcessor that converts a Data instance into a DataTable. Casting for various
 * types of Data is performed as follows:
 * <ul>
 * <li><u>DataDoubleArray</u>. A one-dimensional array will be put into a
 * single column, and a two-dimensional array will be arranged into a
 * corresponding set of multiple columns; attempts to cast higher-dimensional
 * arrays will results in an IllegalArgumentException.
 * 
 * <li><u>DataDouble</u>. Casts to a 1-column, 1-row table.
 * 
 * <li><u>DataInteger</u>. Casts to a 1-column, 1-row table.
 * 
 * <li><u>DataVector</u>. Places vector values in a single column.
 * 
 * <li><u>DataTensor</u>. Arranges elements into multiple columns, like a
 * matrix.
 * 
 * <li><u>DataFunction</u>. Handles only a 1-dimensional DataFunction. Casts
 * to a table of two columns, containing the independent and dependent data,
 * respectively.
 * </ul>
 * Attempts to process a different type will result in an
 * IllegalArgumentException when encountered in the processDataInfo method.
 * <p>
 * @author Andrew Schultz and David Kofke
 *  
 */

/*
 * History Created on Jul 28, 2005 by kofke
 */
public class CastToTable extends DataProcessor implements Serializable {

    /**
     * Sole constructor.
     */
    public CastToTable() {
    }

    public DataTag getTag() {
        // we have no tag
        return null;
    }

    /**
     * Prepares processor to perform cast. Given DataInfo is examined to see
     * what data type will be given to processor.
     * 
     * @throws IllegalArgumentException
     *             if DataInfo is not one of the acceptable types, as described
     *             in general comments for this class
     */
    public DataInfo processDataInfo(DataInfo inputDataInfo) {
        int nRows, nColumns;
        String[] rowHeaders = null;
        DataInfo[] columnInfo = new DataInfo[]{inputDataInfo};
        if (inputDataInfo instanceof DataInfoFunction) {
            int[] arrayShape = ((DataInfoFunction)inputDataInfo).getArrayShape();
            if (arrayShape.length != 1) {
                throw new IllegalArgumentException("DataFunction must be 1-dimensional");
            }
            nColumns = 2;
            nRows = arrayShape[0];
            columnInfo = new DataInfo[2];
            xDataSource = ((DataInfoFunction)inputDataInfo).getXDataSource();
            columnInfo[0] = xDataSource.getIndependentDataInfo(0);
            columnInfo[1] = inputDataInfo;
            inputType = 6;
        }
        else if (inputDataInfo instanceof DataInfoDoubleArray) {
            int[] arrayShape = ((DataInfoDoubleArray) inputDataInfo)
                    .getArrayShape();
            if (arrayShape.length > 2) {
                throw new IllegalArgumentException(
                        "Cannot cast to table a data set with dimension greater than 2: "
                                + arrayShape.length);
            }
            if (arrayShape.length == 1) {
                inputType = 0;
                nColumns = 1;
                nRows = arrayShape[0];
            } else {
                inputType = 1;
                nColumns = arrayShape[0];
                nRows = arrayShape[1];
                columnInfo = new DataInfo[nColumns];
                for (int i=0; i<nColumns; i++) {
                    columnInfo[i] = inputDataInfo;
                }
            }
        } else if (inputDataInfo instanceof DataInfoDouble) {
            inputType = 2;
            nColumns = 1;
            nRows = 1;
        } else if (inputDataInfo instanceof DataInfoInteger) {
            inputType = 3;
            nColumns = 1;
            nRows = 1;
        } else if (inputDataInfo instanceof DataInfoVector) {
            inputType = 4;
            nColumns = 1;
            nRows = ((DataInfoVector)inputDataInfo).getSpace().D();
        } else if (inputDataInfo instanceof DataInfoTensor) {
            inputType = 5;
            int D = ((DataInfoTensor)inputDataInfo).getSpace().D();
            nColumns = D;
            nRows = D;
            columnInfo = new DataInfo[nColumns];
            for (int i=0; i<nColumns; i++) {
                columnInfo[i] = inputDataInfo;
            }
        } else {
            throw new IllegalArgumentException("Cannot cast to DataTable from "
                    + inputDataInfo.getClass());
        }
        outputData = new DataTable(nColumns, nRows);
        if (inputType == 0 || inputType == 6) {
            // we'll actually wrap the incoming DataDoubleArray(s) in a DataTable
            outputData = null;
        }
        return new DataInfoTable("Table", columnInfo, nRows, rowHeaders);

    }

    /**
     * Copies input Data to a DataTable and returns it (the DataTable).
     * 
     * @throws ClassCastException
     *             if input Data is not of the type indicated by the most recent
     *             call to processDataInfo
     *  
     */
    protected Data processData(Data data) {
        switch (inputType) {
        case 0: //DataDoubleArray
            if (outputData == null) {
                outputData = new DataTable(new DataDoubleArray[]{(DataDoubleArray)data});
            }
        case 1: //2D DataDoubleArray
            int nColumns = outputData.getNData();
            for (int i = 0; i < nColumns; i++) {
                ((DataDoubleArray) data).assignColumnTo(i,
                        ((DataDoubleArray)outputData.getData(i)).getData());
            }
            break;
        case 2: //DataDouble
            outputData.E(((DataDouble) data).x);
            break;
        case 3: //DataInteger
            ((DataDoubleArray)outputData.getData(0)).getData()[0] = ((DataInteger) data).x;
            break;
        case 4: //DataVector
            ((DataVector) data).x.assignTo(((DataDoubleArray)outputData.getData(0)).getData());
            break;
        case 5: //DataTensor
            Tensor x = ((DataTensor) data).x;
            for (int i = 0; i < x.D(); i++) {
                for (int j = 0; j < x.D(); j++) {
                    ((DataDoubleArray)outputData.getData(i)).getData()[j] = x.component(i, j);
                }
            }
            break;
        case 6: //DataFunction
            if (outputData == null) {
                outputData = new DataTable(new DataDoubleArray[]{xDataSource.getIndependentData(0),(DataDoubleArray)data});
            }
            break;
        }
        return outputData;
    }

    /**
     * Returns null, indicating the this DataProcessor can handle (almost) any
     * Data type.
     */
    public DataProcessor getDataCaster(DataInfo incomingDataInfo) {
        if (incomingDataInfo instanceof DataInfoGroup) {
            throw new IllegalArgumentException("Cannot cast to DataTable from "
                    + incomingDataInfo.getClass());
        }
        return null;
    }

    private DataTable outputData;
    private int inputType;
    private DataSourceIndependent xDataSource;
}