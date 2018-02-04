package livenet.analysis;

/*
 * Copyright (c) 2000-2018 Robert Biuk-Aghai
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You
 * may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

public class FloatHistogram
{
    private float[][] histData;
    private int size;

    public FloatHistogram(int maxSize)
    {
	histData = new float[maxSize][2];
	size = 0;
    }


    /**
       Returns the value of the entry in the histogram for the
       supplied key, or -1 if the key does not exist in the histogram
    **/
    public float get(float key)
    {
	for (int i = 0; i < size; i++) {
	    if (histData[i][0] < key)
		continue;
	    else if (histData[i][0] == key)
		return histData[i][1];
	    else if (histData[i][0] > key)
		break;
	}
	return -1;
    }


    public float getKeyAt(int index)
    {
	if (index >= 0 && index < size)
	    return histData[index][0];
	return -1;
    }


    public float getValueAt(int index)
    {
	if (index >= 0 && index < size)
	    return histData[index][1];
	return -1;
    }


    /**
       Inserts the specified key-value pair into the histogram at the
       appropriate position, or adds the value to the existing value
       for that key if the key is already in the histogram.
    **/
    public void put(float key, float value)
    {
	boolean haveKey = false;
	int index = -1;

	while (++index < size) {
	    if (histData[index][0] < key)
		continue;
	    if (histData[index][0] == key) {
		histData[index][1] += value;
		haveKey = true;
		break;
	    }
	    if (histData[index][0] > key) {
		for (int i = size; i > index; i--) {
		    histData[i][0] = histData[i - 1][0];
		    histData[i][1] = histData[i - 1][1];
		}
		break;
	    }
	}

	if (!haveKey) {
	    histData[index][0] = key;
	    histData[index][1] = value;
	    size++;
	}
    }


    public void put(float key)
    {
	this.put(key, (float) 1.0);
    }


    public int getSize()
    {
	return size;
    }


    public float[][] getData()
    {
	return histData;
    }


    public float getMinKey()
    {
	return histData[0][0];
    }


    public float getMaxKey()
    {
	return histData[size - 1][0];
    }


    public float getMinValue()
    {
	float minValue = Float.MAX_VALUE;
	for (int i = 0; i < size; i++)
	    if (histData[i][1] < minValue)
		minValue = histData[i][1];
	return minValue;
    }


    public float getMaxValue()
    {
	float maxValue = Float.MIN_VALUE;
	for (int i = 0; i < size; i++)
	    if (histData[i][1] > maxValue)
		maxValue = histData[i][1];
	return maxValue;
    }


    public int getIndex(float key)
    {
	for (int i = 0; i < size; i++) {
	    if (histData[i][0] < key)
		continue;
	    else if (histData[i][0] == key)
		return i;
	    else if (histData[i][0] > key)
		break;
	}
	return -1;
    }
}
