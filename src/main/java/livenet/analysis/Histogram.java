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

public class Histogram
{
    private int[][] histData;
    private int size;

    public Histogram(int maxSize)
    {
	histData = new int[maxSize][2];
	size = 0;
    }


    /**
       Returns the value of the entry in the histogram for the
       supplied key, or -1 if the key does not exist in the histogram
    **/
    public int get(int key)
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


    public int getKeyAt(int index)
    {
	if (index >= 0 && index < size)
	    return histData[index][0];
	return -1;
    }


    public int getValueAt(int index)
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
    public void put(int key, int value)
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


    public void put(int key)
    {
	this.put(key, 1);
    }


    public int getSize()
    {
	return size;
    }


    public int[][] getData()
    {
	return histData;
    }


    public int getMinKey()
    {
	return histData[0][0];
    }


    public int getMaxKey()
    {
	return histData[size - 1][0];
    }


    public int getMinValue()
    {
	int minValue = Integer.MAX_VALUE;
	for (int i = 0; i < size; i++)
	    if (histData[i][1] < minValue)
		minValue = histData[i][1];
	return minValue;
    }


    public int getMaxValue()
    {
	int maxValue = Integer.MIN_VALUE;
	for (int i = 0; i < size; i++)
	    if (histData[i][1] > maxValue)
		maxValue = histData[i][1];
	return maxValue;
    }


    public int getIndex(int key)
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
