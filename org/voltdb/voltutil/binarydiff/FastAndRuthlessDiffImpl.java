/* This file is part of VoltDB.
 * Copyright (C) 2008-2017 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.voltdb.voltutil.binarydiff;

import java.util.ArrayList;

import org.voltdb.voltutil.binarydiff.exceptions.BadInputException;
import org.voltdb.voltutil.binarydiff.exceptions.HashCodeMismatchException;
import org.voltdb.voltutil.binarydiff.exceptions.TooBigToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.TooSmallToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.UneconomicToDiffException;
import org.voltdb.voltutil.stats.StatsHistogram;

/**
 * A class for reducing network bandwidth by only sending deltas when BLOBS / byte[] are updated.
 * @author drolfe
 *
 */
public class FastAndRuthlessDiffImpl  implements ArrayDiffIFace {


	// Tracks % effectiveness of diff
	StatsHistogram compressionHistogram = new StatsHistogram(100);
	
	// Tracks number of splits
	StatsHistogram splitsHistogram = new StatsHistogram(100);

	/**
	 * @param oldArray
	 *            An arbitrary length of byte[] that could be null, byte[0],
	 *            byte[n]...
	 * @param newArray
	 *            An arbitrary length of byte[] that could be null, byte[0],
	 *            byte[n]...
	 * @param maxSplits How any differences newArray contains - useful for optimxzation
	 * @return A byte[] which can be used to turn oldArray into newArray... How
	 *         this happens is up to the implementing class...
	 * @throws TooSmallToDeltaException
	 * @throws UneconomicToDiffException
	 * @throws TooBigToDeltaException
	 * @throws BadInputException
	 */
	@Override
	public byte[] calculateDiff(byte[] oldArray, byte[] newArray, int maxSplits)
			throws TooSmallToDeltaException, TooBigToDeltaException, UneconomicToDiffException {

		int splitCountsThisArray = 0;

		ByteArrayLib.checkTooSmall(oldArray, "old array");
		ByteArrayLib.checkTooSmall(newArray, "new array");
		ByteArrayLib.checkTooBig(oldArray, "old array");
		ByteArrayLib.checkTooBig(newArray, "new array");

		ArrayList<FastAndRuthlessDiffImplByteArrayParts> diffArrayList = new ArrayList<FastAndRuthlessDiffImplByteArrayParts>();

		FastAndRuthlessDiffImplByteArrayParts rootArray = new FastAndRuthlessDiffImplByteArrayParts(newArray);
		diffArrayList.add(rootArray);

		int splitTarget = ByteArrayLib.findSplitTarget(diffArrayList);
		while (splitTarget > -1 && splitCountsThisArray < maxSplits) {
			
			splitCountsThisArray += ByteArrayLib.splitByteArrayPartsByContent(diffArrayList, splitTarget, oldArray);
			splitTarget = ByteArrayLib.findSplitTarget(diffArrayList);
		}

	    splitTarget = ByteArrayLib.findRepeatingGroupTarget(diffArrayList);
		while (splitTarget > -1 && splitCountsThisArray < maxSplits) {
			
			splitCountsThisArray += ByteArrayLib.splitByteArrayPartsByRepeatingBytes(diffArrayList, splitTarget);
			splitTarget = ByteArrayLib.findRepeatingGroupTarget(diffArrayList);

		}

		splitsHistogram.report(splitCountsThisArray, null);

		FastAndRuthlessDiffImplByteArrayParts[] theParts = new FastAndRuthlessDiffImplByteArrayParts[0];
		theParts = diffArrayList.toArray(theParts);

		int totalMessageSize = 0;

		for (int i = 0; i < theParts.length; i++) {
			totalMessageSize += theParts[i].messageSize();
		}

		int pctDecrease = 100 - ((totalMessageSize * 100) / newArray.length);

		byte[] outArray = new byte[totalMessageSize];

		ByteArrayLib.giveUpIfUneconomic(oldArray, outArray);

		StringBuffer descBuffer = new StringBuffer(theParts.length * 10);
		
		descBuffer.append("Size=" +  newArray.length  + "->" +totalMessageSize  + " ");
		
		int position = 0;
		for (int i = 0; i < theParts.length; i++) {
			
			descBuffer.append(theParts[i]);

			byte[] tempArray = theParts[i].getMessageBytes();

			for (int j = 0; j < tempArray.length; j++) {
				outArray[position++] = tempArray[j];
			}

		}

		compressionHistogram.report(pctDecrease, descBuffer.toString());

		return outArray;

	}


	/**
	 * @param inArray
	 *            An arbitrary length of byte[] that could be null, byte[0],
	 *            byte[n]...
	 * @param inDiff
	 *            A byte[] which can be used to turn oldArray into newArray...
	 *            How this happens is up to the implementing class...
	 * @param targetHashCode
	 *            an int that is used to sanity check whether inDiff is
	 *            appropriate or not according to AbstractDiffImply.determinsiticHashcode()
	 * @return A byte[] containing the updated value...
	 * @throws HashCodeMismatchException
	 * @throws BadInputException
	 */
	@Override
	public byte[] applyDiff(byte[] inArray, byte[] inDiff, int targetHashCode) throws HashCodeMismatchException {

		ByteArrayLib.checkHashCode(inArray, targetHashCode);

		return ByteArrayLib.decodeFARDData(inArray, inDiff);
	}



	/**
	 * 
	 */
	public void resetStats() {
		compressionHistogram.resetLatency();
		splitsHistogram.resetLatency();

	}

	@Override
	public String toString() {

		final String description = "Compression:\n" + compressionHistogram.toString() + "\nSplits:\n"
				+ splitsHistogram.toString();
		resetStats();
		return description;
	}

	public StatsHistogram getCompressionHistogram() {
		return compressionHistogram;
	}

	public StatsHistogram getSplitsHistogram() {
		return splitsHistogram;
	}
	
	
}