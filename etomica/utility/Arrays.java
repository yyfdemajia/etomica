/*
 * History
 * Created on Oct 28, 2004 by kofke, schultz
 */
package etomica.utility;

import java.lang.reflect.Array;

/**
 * Non-instantiable class with static utility methods for working with arrays.
 */
public final class Arrays {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private Arrays() {	}

    /**
     * Returns a new array holding the objects in the given array, but adjusted
     * to the given size.  If the new size is greater than the given array, all
     * elements of the array are copied to the new array, starting from index 0
     * (with unfilled elements of new array left as null);
     * if the new size is smaller than the given array, elements of the old array
     * (starting from 0) will be copied to fill the new array.  New array is
     * of the same type as the old one (returned array must be cast before
     * assigning to a field of the same type as the old array).
     * @param oldArray array with elements to be copied to the new array
     * @param newSize size of the new array
     * @return the new array
     */
	public static Object[] resizeArray(Object[] oldArray, int newSize) {
		Object[] newArray = (Object[])Array.newInstance(oldArray.getClass().getComponentType(),newSize);
		int minSize = Math.min(oldArray.length,newSize);
		System.arraycopy(oldArray,0,newArray,0,minSize);
		return newArray;
	}
	

    /**
     * Returns an array formed from adding the newObject to the elements
     * in the given array.  New array is one element larger than given array,
     * and is of same type as given array.  newObject is placed at the end of 
     * the new array.
     * @param objects array with objects to be put in new array
     * @param newObject object placed at end of new array
     * @return new array 
     */
	public static Object[] addObject(Object[] objects, Object newObject) {
		objects = resizeArray(objects,objects.length+1);
		objects[objects.length-1] = newObject;
		return objects;
	}

    /**
     * Returns an array formed by removing the given object from the given array.  
     * New array is one element smaller than given array,
     * and is of same type as given array.
     * @param array array with objects to be put in new array
     * @param newObject object placed at end of new array
     * @return new array 
     */
	public static Object[] removeObject(Object[] array, Object object) {
		int length = array.length;
		for (int i=0; i<length; i++) {
			if (array[i] == object) {//look for object in array
				Object lastObject = array[length-1];//save last object, which is about to be dropped
				array = resizeArray(array,length-1);//shorten array, dropping last object
				if (i < length-2) {//overwrite target object
					System.arraycopy(array,i+1,array,i,length-i-2);
				}
				if (i < length-1) {//recover last object
					array[length-2] = lastObject;
				}
				break;
			}
		}
		return array;
	}

}
