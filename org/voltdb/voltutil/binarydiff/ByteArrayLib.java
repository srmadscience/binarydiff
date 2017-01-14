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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.apache.commons.lang3.ArrayUtils;
import org.voltdb.voltutil.binarydiff.exceptions.HashCodeMismatchException;
import org.voltdb.voltutil.binarydiff.exceptions.TooBigToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.TooSmallToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.UneconomicToDiffException;

/**
 * A static library of methods that manipulate byte arrays
 * 
 * @author drolfe
 *
 */
public class ByteArrayLib {

	public static final short FARD_DATA_BLOCK = Short.MAX_VALUE;
	public static final short FARD_REPEATING_BLOCK = Short.MAX_VALUE - 1;
	public static final short FARD_REFERENCE_BLOCK = Short.MAX_VALUE - 2;

	// findMatchLocation returns an array of int[3]
	public static final int FAST_RUTHLESS_ARRAY_POS_NEW_LOC = 0;
	public static final int FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION = 1;
	public static final int FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH = 2;

	// The smallest pattern we we are willing to split
	public static final int FAST_RUTHLESS_MIN_SPLIT_SIZE = 15;

	// When we search the old array for a match we look for SPLIT_PATTERN_LENGTH
	// bytes
	public static final int FAST_RUTHLESS_SPLIT_PATTERN_LENGTH = 15;

	// The minimum size object we;d even consider splitting
	public static final int FAST_RUTHLESS_MINIMUM_POSSIBLE_SIZE = 3;

	// How careless we are in splitByteArrayParts...
	private static final int FAST_AND_RUTHLRESS_DEFAULT_STEPSIZE = 3;

	// Minimum repeating byte...
	private static final int FAST_AND_RUTHLRESS_MIN_RUNSIZE = 6;

	public static int splitByteArrayPartsByRepeatingBytes(
			ArrayList<FastAndRuthlessDiffImplByteArrayParts> splittableParts, int splitTarget) {

		final int originalArraySize = splittableParts.size();

		// Find the thing we are being asked to split.
		FastAndRuthlessDiffImplByteArrayParts repeatingBytesSection = splittableParts.get(splitTarget);

		// make sure we won't try and split it again
		repeatingBytesSection.setMightHaveRepeats(false);

		if (!repeatingBytesSection.isOfType(FARD_DATA_BLOCK)) {
			return 0;
		}

		if (repeatingBytesSection.theBytes == null
				|| repeatingBytesSection.theBytes.length < FAST_AND_RUTHLRESS_MIN_RUNSIZE) {
			return 0;
		}

		// Create an empty ArrayList for our split object. On a good day we'll
		// split it in 2.
		ArrayList<FastAndRuthlessDiffImplByteArrayParts> diffArrayList = new ArrayList<FastAndRuthlessDiffImplByteArrayParts>(
				2);

		// Walk through the byte[] and see if we can find a repeating group...

		int currentRunStart = 0;
		int currentRunEnd = 0;

		int longestRunStart = 0;
		int longestRunEnd = 0;

		for (int i = 1; i < repeatingBytesSection.theBytes.length; i++) {

			// If we are currently in a run is it continuing?
			if (currentRunStart > -1) {
				if (repeatingBytesSection.theBytes[i] == repeatingBytesSection.theBytes[i - 1]) {
					// run is continuing
					currentRunEnd = i;
				} else {
					// Run has ended - see if its a record
					if ((currentRunEnd - currentRunStart) > (longestRunEnd - longestRunStart)) {
						longestRunStart = currentRunStart;
						longestRunEnd = currentRunEnd;

					}
					currentRunStart = -1;
					currentRunEnd = -1;

				}
			} else {
				// Should we start a run?
				if (repeatingBytesSection.theBytes[i] == repeatingBytesSection.theBytes[i - 1]) {
					// New run starting
					currentRunStart = i - 1;
					currentRunEnd = i;

				}
			}
		}

		// Handle case where run continued to the end...
		if ((currentRunEnd - currentRunStart) > (longestRunEnd - longestRunStart)) {

			longestRunStart = currentRunStart;
			longestRunEnd = currentRunEnd;

		}

		// Give up if savings are minimal
		if (longestRunEnd - longestRunStart < FAST_AND_RUTHLRESS_MIN_RUNSIZE) {
			return 0;
		}

		// Figure out how many new parts we'll have. We only split data blocks
		// so
		// we'll end up with 1 repeating byte block and either 1 or 2 data
		// blocks,
		// except in the case where the entire data block was 1 repeating
		// character.
		if (longestRunStart == 0 && longestRunEnd == (repeatingBytesSection.theBytes.length - 1)) {
			// We are replacing an entire block...
			FastAndRuthlessDiffImplByteArrayParts newPart = new FastAndRuthlessDiffImplByteArrayParts(
					(short) repeatingBytesSection.theBytes.length, repeatingBytesSection.theBytes[longestRunStart]);
			diffArrayList.add(newPart);

		} else {

			if (longestRunStart > 0) {
				// We have a 'before' block
				byte[] beforeArray = ArrayUtils.subarray(repeatingBytesSection.theBytes, 0, longestRunStart);
				FastAndRuthlessDiffImplByteArrayParts beforePart = new FastAndRuthlessDiffImplByteArrayParts(
						beforeArray);
				diffArrayList.add(beforePart);

			}

			// We always have a 'middle'
			FastAndRuthlessDiffImplByteArrayParts newPart = new FastAndRuthlessDiffImplByteArrayParts(
					(short) (longestRunEnd - longestRunStart), repeatingBytesSection.theBytes[longestRunStart]);
			diffArrayList.add(newPart);

			if (longestRunEnd < (repeatingBytesSection.theBytes.length)) {
				// We have a 'end' block
				byte[] endArray = ArrayUtils.subarray(repeatingBytesSection.theBytes, longestRunEnd,
						repeatingBytesSection.theBytes.length);
				FastAndRuthlessDiffImplByteArrayParts afterPart = new FastAndRuthlessDiffImplByteArrayParts(endArray);
				diffArrayList.add(afterPart);

			}

		}

		// See if we actually saved space...
		if (getDiffedSize(diffArrayList) < repeatingBytesSection.theBytes.length) {

			replaceSplitTargetWithOthers(splittableParts, splitTarget, diffArrayList);

		}

		return splittableParts.size() - originalArraySize;

	}

	/**
	 * Find a chunk of newArray that also exists in oldArray.
	 * 
	 * @param splittableParts
	 *            sorted list of old byte array components. We update this by
	 *            trying to turn chunks of binary data into references to
	 *            oldArray
	 * @param splitTarget
	 *            which element in splittableParts is to be split
	 * @param oldArray
	 *            the exiting array that we are deltaing
	 * @return how many splits we did. From 0 to 3-ish.
	 */
	public static int splitByteArrayPartsByContent(ArrayList<FastAndRuthlessDiffImplByteArrayParts> splittableParts,
			int splitTarget, byte[] oldArray) {

		final int originalArraySize = splittableParts.size();

		// Find the thing we are being asked to split.
		FastAndRuthlessDiffImplByteArrayParts splittableSection = splittableParts.get(splitTarget);

		// make sure we won't try and split it again
		splittableSection.setSplittable(false);

		// Create an empty ArrayList for our split object. On a good day well
		// split it in 3.
		ArrayList<FastAndRuthlessDiffImplByteArrayParts> diffArrayList = new ArrayList<FastAndRuthlessDiffImplByteArrayParts>(
				3);

		int firstDiffFromFront = indexOfDifference(oldArray, splittableSection.theBytes);

		try {
			if (firstDiffFromFront > splittableSection.theBytes.length) {
				// new string is a superset of old string

				// Add common part of old string
				diffArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(0, firstDiffFromFront));

				// Add new bit
				byte[] endBit = ArrayUtils.subarray(splittableSection.theBytes, firstDiffFromFront,
						splittableSection.theBytes.length + 1);
				diffArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(endBit));

			} else if (firstDiffFromFront == splittableSection.theBytes.length) {
				// new string is a subset of old string
				diffArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(0, splittableSection.theBytes.length));

			} else {

				if (splittableSection.theBytes.length > FAST_RUTHLESS_MIN_SPLIT_SIZE) {

					final int[] matchLocations = findMatchLocation(splittableSection.theBytes, oldArray,
							FAST_AND_RUTHLRESS_DEFAULT_STEPSIZE);
					final int oldLocation = matchLocations[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION];
					final int newLocation = matchLocations[FAST_RUTHLESS_ARRAY_POS_NEW_LOC];
					final int runLength = matchLocations[FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH];

					if (oldLocation > -1) {

						if (newLocation > 0) {
							// 1st binary chunk
							FastAndRuthlessDiffImplByteArrayParts firstBit = new FastAndRuthlessDiffImplByteArrayParts(
									ArrayUtils.subarray(splittableSection.theBytes, 0, newLocation));
							diffArrayList.add(firstBit);
						}

						// 2nd - reference to oldString
						FastAndRuthlessDiffImplByteArrayParts secondBit = new FastAndRuthlessDiffImplByteArrayParts(
								oldLocation, oldLocation + runLength);
						diffArrayList.add(secondBit);

						if (newLocation + secondBit.actualSize() < splittableSection.theBytes.length) {
							// third binary chunk
							FastAndRuthlessDiffImplByteArrayParts thirdBit = new FastAndRuthlessDiffImplByteArrayParts(
									ArrayUtils.subarray(splittableSection.theBytes,
											newLocation + secondBit.actualSize(), splittableSection.theBytes.length));

							if (thirdBit.actualSize() > 0) {
								diffArrayList.add(thirdBit);
								
							}
						}

					} else {
						// we can't improve on what we already have...
						diffArrayList.add(splittableSection);

					}

				} else {
					// we can't improve on what we already have...
					diffArrayList.add(splittableSection);
				}

			}
		} catch (TooBigToDeltaException e) {
			// We didn't split anything...
			return 0;
		}

		// See if we actually saved space...
		if (getDiffedSize(diffArrayList) < splittableSection.theBytes.length) {

			replaceSplitTargetWithOthers(splittableParts, splitTarget, diffArrayList);

		}

		return splittableParts.size() - originalArraySize;

	}

	/**
	 * Find a point at which *part* of newArray exists in oldArray.
	 * 
	 * @param newBytes
	 *            A new array we have created that may or may not be like the
	 *            old array
	 * @param oldArray
	 *            The old Array we are trying to delta
	 * @param stepSize
	 *            we take chunks of newArray and try to match them in oldArray.
	 *            Stepsize of 3 means we check very third chunk. 3 is a good
	 *            value for this param.
	 * @return int[3]. [0] will contain the point where the new array starts to
	 *         be the same as the old array. [1] will contain the point in the
	 *         old array where this happens [2] will contain how long the run
	 *         length is
	 */
	public static int[] findMatchLocation(byte[] newBytes, byte[] oldArray, int stepSize) {

		int[] result = new int[3];

		result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC] = -1;
		result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION] = -1;
		result[FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH] = -1;

		for (int i = 0; i < newBytes.length - FAST_RUTHLESS_SPLIT_PATTERN_LENGTH; i = i
				+ (FAST_RUTHLESS_SPLIT_PATTERN_LENGTH * stepSize)) {

			byte[] targetArray1 = new byte[FAST_RUTHLESS_SPLIT_PATTERN_LENGTH];

			for (int j = 0; j < targetArray1.length; j++) {
				targetArray1[j] = newBytes[i + j];
			}

			result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION] = indexOfArrayMatch(oldArray, targetArray1);

			if (result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION] > -1) {

				result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC] = i;
				result[FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH] = indexOfDifference(oldArray,
						result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION], newBytes,
						result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC]);

				int howFarBackItsTheSame = reverseIndexOfDifference(oldArray,
						result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION], newBytes,
						result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC]);

				if (howFarBackItsTheSame > 0) {
					result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC] = result[FAST_RUTHLESS_ARRAY_POS_NEW_LOC]
							- howFarBackItsTheSame;
					result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION] = result[FAST_RUTHLESS_ARRAY_POS_OLD_LOCATION]
							- howFarBackItsTheSame;
					result[FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH] = result[FAST_RUTHLESS_ARRAY_POS_RUN_LENGTH]
							+ howFarBackItsTheSame;

				}

				break;
			}
		}

		return result;

	}

	/**
	 * Assuming we've found a matching chunk starting at oldArray[oldOffset] and
	 * newArray[newOffset] walk backwards and see whwre the identical sections
	 * actually start.
	 * 
	 * @param oldArray
	 *            original data
	 * @param oldOffset
	 *            point at which identical section starts
	 * @param newBytes
	 *            new data
	 * @param newOffset
	 *            point at which identical section starts
	 * @return how far backwards the match goes, expressed as a positive number
	 */
	public static int reverseIndexOfDifference(byte[] oldArray, int oldOffset, byte[] newBytes, int newOffset) {

		int match = -1;

		boolean keepGoing = true;

		int position = 0;

		while (keepGoing) {

			position++;

			if (position > oldOffset || position > newOffset) {
				// We are out of array - return
				keepGoing = false;
			} else if (oldArray[oldOffset - position] == newBytes[newOffset - position]) {
				match = position;
			} else {
				keepGoing = false;
			}

		}

		return match;
	}

	/**
	 * See where in targetArray oldArray exists
	 * 
	 * @param oldArray
	 *            The thing we are searching
	 * @param targetArray
	 *            The array we are trying to find in oldArray
	 * @return the location in oldArray where a match exists.
	 */
	public static int indexOfArrayMatch(byte[] oldArray, byte[] targetArray) {
		int match = -1;

		for (int i = 0; i < (oldArray.length - targetArray.length); i++) {
			if (oldArray[i] == targetArray[0]) {

				boolean restMatches = true;

				for (int j = 1; j < targetArray.length; j++) {
					if (oldArray[i + j] != targetArray[j]) {
						restMatches = false;
						break;
					}
				}

				if (restMatches) {
					match = i;
					break;
				}
			}
		}

		return match;
	}

	/**
	 * Assuming we've found a matching chunk starting at oldArray[oldOffset] and
	 * newArray[newOffset] walk backwards and see where the identical sections
	 * end.
	 * 
	 * @param oldArray
	 *            original data
	 * @param oldOffset
	 *            point at which identical section starts
	 * @param newBytes
	 *            new data
	 * @param newOffset
	 *            point at which identical section starts
	 * @return how far forwards the match goes.
	 */
	private static int indexOfDifference(byte[] oldArray, int oldOffset, byte[] newBytes, int newOffset) {

		int match = -1;

		for (int i = 0; i < (oldArray.length - oldOffset) && i < (newBytes.length - newOffset); i++) {
			if (oldArray[i + oldOffset] == newBytes[i + newOffset]) {
				match = i;
			} else {
				break;
			}
		}

		return match + 1;
	}

	/**
	 * Assuming we have two byte[] that start the same at what point do they
	 * differ?
	 * 
	 * @param oldArray
	 *            the old array we are searching
	 * @param newBytes
	 *            The new bytes we are looking for, which are supposed to start
	 *            the same.
	 * @return the point at which they differ
	 */
	public static int indexOfDifference(byte[] oldArray, byte[] newBytes) {
		return indexOfDifference(oldArray, 0, newBytes, 0);
	}

	/**
	 * Decode data in "Fast And Ruthless Diff" format
	 * 
	 * @param oldArray
	 *            Original Data
	 * @param byteArray
	 *            Encoded Data
	 * @return modified Data
	 */
	public static byte[] decodeFARDData(byte[] oldArray, byte[] byteArray) {

		ArrayList<FastAndRuthlessDiffImplByteArrayParts> outArrayList = new ArrayList<FastAndRuthlessDiffImplByteArrayParts>();
		ByteBuffer buf = ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN);

		// Map inpout byte[] to an arraylist of
		// FastAndRuthlessDiffImplByteArrayParts
		while (buf.hasRemaining()) {

			// 0 means a copy from the old array. Anything else is how much raw
			// data is next...
			short start = buf.getShort();
			if (start == FARD_REPEATING_BLOCK) {
				// copy
				short repeatCount = buf.getShort();
				byte theByte = buf.get();

				outArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(repeatCount, theByte));

			} else if (start == FARD_DATA_BLOCK) { // FARD_DATA_BLOCK
				short size = buf.getShort();
				byte[] newBit = new byte[size];

				try {
					for (int i = 0; i < newBit.length; i++) {
						newBit[i] = buf.get();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				outArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(newBit));
			} else {
				// copy
				short end = buf.getShort();
				try {
					outArrayList.add(new FastAndRuthlessDiffImplByteArrayParts(start, end));
				} catch (TooBigToDeltaException e) {
					// This line is in practice unreachable...
					e.printStackTrace();
				}

			}
		}

		// Turn our arraylist into byte[]
		FastAndRuthlessDiffImplByteArrayParts[] outArray = new FastAndRuthlessDiffImplByteArrayParts[outArrayList
				.size()];
		outArray = outArrayList.toArray(outArray);

		int totalSize = 0;

		for (int i = 0; i < outArray.length; i++) {
			System.out.println(outArray[i] + " " + outArray[i].actualSize());
			totalSize += outArray[i].actualSize();
		}

		byte[] outByteArray = new byte[totalSize];

		int position = 0;

		for (int i = 0; i < outArray.length; i++) {

			byte[] tempArray = outArray[i].getActualBytes(oldArray);

			for (int j = 0; j < tempArray.length; j++) {

				try {
					outByteArray[position++] = tempArray[j];
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

		return outByteArray;

	}

	/**
	 * Replace a single splittable part with 1 or more, while preserving the
	 * order in the ArrayList
	 * 
	 * @param splittableParts
	 *            Original ArrayList
	 * @param splitTarget
	 *            The element we're splitting
	 * @param diffArrayList
	 *            What its being split into
	 */
	public static void replaceSplitTargetWithOthers(ArrayList<FastAndRuthlessDiffImplByteArrayParts> splittableParts,
			int splitTarget, ArrayList<FastAndRuthlessDiffImplByteArrayParts> diffArrayList) {

		splittableParts.remove(splitTarget);

		for (int i = 0; i < diffArrayList.size(); i++) {
			splittableParts.add(splitTarget + i, diffArrayList.get(i));
		}

	}

	/**
	 * Find the most plausible chunk of raw text to split
	 * 
	 * @param splittableParts
	 * @param onlyData
	 * @return the id of the most plausible candidate to split
	 */
	public static int findSplitTarget(ArrayList<FastAndRuthlessDiffImplByteArrayParts> splittableParts) {
		int target = -1;
		int longestSectionLength = -1;

		// Find longest splittable section
		for (int i = 0; i < splittableParts.size(); i++) {
			if (splittableParts.get(i).isSplittable() && splittableParts.get(i).actualSize() > longestSectionLength) {

				longestSectionLength = splittableParts.get(i).actualSize();
				target = i;

			}
		}

		return target;
	}

	/**
	 * Find the most plausible chunk of raw text to split
	 * 
	 * @param repeatingGroupsParts
	 * @param onlyData
	 * @return the id of the most plausible candidate to split
	 */
	public static int findRepeatingGroupTarget(ArrayList<FastAndRuthlessDiffImplByteArrayParts> repeatingGroupsParts) {
		int target = -1;
		int longestSectionLength = -1;

		// Find longest splittable section
		for (int i = 0; i < repeatingGroupsParts.size(); i++) {
			if (repeatingGroupsParts.get(i).isOfType(FARD_DATA_BLOCK)
					&& repeatingGroupsParts.get(i).actualSize() > longestSectionLength
					&& repeatingGroupsParts.get(i).isMightHaveRepeats()) {

				longestSectionLength = repeatingGroupsParts.get(i).actualSize();
				target = i;

			}
		}

		return target;
	}

	/**
	 * Determine the size of a message after we have diffed it
	 * 
	 * @param splittableParts
	 * @return size in bytes
	 */
	public static int getDiffedSize(ArrayList<FastAndRuthlessDiffImplByteArrayParts> splittableParts) {
		int size = -1;

		// Find longest splittable section
		for (int i = 0; i < splittableParts.size(); i++) {
			size += splittableParts.get(i).messageSize();
		}

		return size;
	}

	/**
	 * Throws an exception if the hashcode of the array is not the one we have
	 * been given
	 * 
	 * @param tgtArray
	 * @param targetHashCode
	 * @throws HashCodeMismatchException
	 */
	public static void checkHashCode(byte[] tgtArray, int targetHashCode) throws HashCodeMismatchException {

		final int actualHashCode = deterministicHashcode(tgtArray);

		if (actualHashCode != targetHashCode) {
			throw new HashCodeMismatchException(
					"deterministicHashcode: Got " + actualHashCode + ", expected " + targetHashCode);
		}

	}

	/**
	 * Complain in this is too small to delta.
	 * 
	 * @param tgt
	 *            the thing we are trying to delta
	 * @param descr
	 *            a description of it
	 * @throws TooSmallToDeltaException
	 */
	public static void checkTooSmall(byte[] tgt, String descr) throws TooSmallToDeltaException {

		if (tgt == null || tgt.length < FAST_RUTHLESS_MINIMUM_POSSIBLE_SIZE) {
			throw new TooSmallToDeltaException(descr + " is too small to apply delta function");
		}

	}

	/**
	 * Complain in this is too big to delta.
	 * 
	 * @param tgt
	 *            the thing we are trying to delta
	 * @param descr
	 *            a description of it
	 * @throws TooBigToDeltaException
	 */
	public static void checkTooBig(byte[] tgt, String descr) throws TooBigToDeltaException {

		if (tgt == null || tgt.length >= Short.MAX_VALUE - 2) {
			throw new TooBigToDeltaException(descr + " is too big to apply delta function");
		}

	}

	/**
	 * Prevent diffed data from taking up more space than original data
	 * 
	 * @param oldArray
	 * @param deltaArray
	 * @throws UneconomicToDiffException
	 */
	public static void giveUpIfUneconomic(byte[] oldArray, byte[] deltaArray) throws UneconomicToDiffException {

		if (oldArray.length < deltaArray.length) {
			throw new UneconomicToDiffException(
					"delta length of " + deltaArray.length + " longer than " + oldArray.length);
		}

	}

	/**
	 * A deterministic hashcode for byte[]. We don't use the java hashcode()
	 * function as the client may be speaking C++, so this may have to be
	 * re-implemented in C++.
	 * 
	 * @param array
	 * @return
	 */
	public static int deterministicHashcode(byte[] array) {

		// TODO make this better

		long value = 42;

		if (array == null) {
			return Integer.MIN_VALUE;
		} else if (array.length == 0) {
			return Integer.MIN_VALUE + 1;
		}

		for (int i = 0; i < array.length; i++) {
			value += (array[i] * i);
		}

		if (value > Long.MAX_VALUE - (32767 * 255)) {
			value = Integer.MIN_VALUE + 2;
		}

		return (int) (value % Integer.MAX_VALUE);

	}

	/**
	 * Generate a FARD encoded representation of a start end range
	 * 
	 * @param start
	 * @param end
	 * @return byte[4] - the start and end numbers encoded.
	 */
	public static byte[] encodeFARDData(short start, short end) {

		short[] shortArray = { start, end };
		byte[] byteArray = new byte[shortArray.length * 2];
		ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray);

		return byteArray;
	}

	/**
	 * Generate a FARD encoded representation of a byte[] array
	 * 
	 * @param section
	 *            a byte array to be encoded
	 * @return retuen byte[4 + section.length] - the block of data encoded
	 */
	public static byte[] encodeFARDData(byte[] section) {

		short[] shortArray = { FARD_DATA_BLOCK, (short) section.length };
		byte[] byteArray = new byte[shortArray.length * 2];
		ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray);

		return ArrayUtils.addAll(byteArray, section);
	}

	/**
	 * Generate a FARD encoded representation of a repeating byte[] array
	 * 
	 * @param repeatCount
	 * @param repeatingByte
	 *            a byte to be encoded
	 * @return return byte[5] - the block of data encoded
	 */
	public static byte[] encodeFARDData(byte repeatingByte, short repeatCount) {
		short[] shortArray = { FARD_REPEATING_BLOCK, (short) repeatCount };
		byte[] byteArray = new byte[shortArray.length * 2];
		byte[] sectionArray = { repeatingByte };

		ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shortArray);

		return ArrayUtils.addAll(byteArray, sectionArray);
	}
}
