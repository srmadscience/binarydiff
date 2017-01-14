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

import org.apache.commons.lang3.ArrayUtils;
import org.voltdb.voltutil.binarydiff.exceptions.TooBigToDeltaException;

/**
 * A class representing a chunk of FastAndRuthlessDiffImpl encoded data.
 * 
 * @author drolfe
 *
 */
public class FastAndRuthlessDiffImplByteArrayParts {

	// binary data
	byte[] theBytes = new byte[0];

	// If start = Short.MAXVALUE then end is theBytes.length.
	private short start = ByteArrayLib.FARD_DATA_BLOCK;
	private short end = 0;

	// Whether this is splittible.
	private boolean splittable = false;
	
	private boolean mightHaveRepeats = false;

	/**
	 * @param oldStart
	 * @param oldEnd
	 * @throws TooBigToDeltaException
	 */
	public FastAndRuthlessDiffImplByteArrayParts(int oldStart, int oldEnd) throws TooBigToDeltaException {

		if (oldStart >= ByteArrayLib.FARD_REFERENCE_BLOCK) {
			throw new TooBigToDeltaException("Value for start of " + oldStart + " is too big, max is "
					+ (ByteArrayLib.FARD_REFERENCE_BLOCK - 1));
		}

		if (oldEnd >= ByteArrayLib.FARD_REFERENCE_BLOCK) {
			throw new TooBigToDeltaException(
					"Value for end of " + oldEnd + " is too big, max is " + (ByteArrayLib.FARD_REFERENCE_BLOCK - 1));
		}

		this.start = (short) oldStart;
		this.end = (short) oldEnd;

	}

	/**
	 * First constructor - data is represented as a block in another array
	 * somewhere..
	 * 
	 * @param start
	 * @param end
	 * @throws TooBigToDeltaException
	 */
	public FastAndRuthlessDiffImplByteArrayParts(short start, short end) throws TooBigToDeltaException {

		if (start >= ByteArrayLib.FARD_REFERENCE_BLOCK) {
			throw new TooBigToDeltaException(
					"Value for start of " + start + " is too big, max is " + (ByteArrayLib.FARD_REFERENCE_BLOCK - 1));
		}

		this.start = start;
		this.end = end;
	}

	/**
	 * Second constructor - data is passed in directly
	 * 
	 * @param section
	 */
	public FastAndRuthlessDiffImplByteArrayParts(byte[] section) {

		theBytes = section;
		start = ByteArrayLib.FARD_DATA_BLOCK;
		end = (short) section.length;

		if (section.length >= ByteArrayLib.FAST_RUTHLESS_MIN_SPLIT_SIZE) {
			setSplittable(true);
			setMightHaveRepeats(true);
		}
	}

	/**
	 * Third constructor - A repeating block of data
	 * 
	 * @param repeatCount
	 * @param section
	 */
	public FastAndRuthlessDiffImplByteArrayParts(short repeatCount, byte aByte) {

		theBytes = new byte[1];
		theBytes[0] = aByte;
		start = ByteArrayLib.FARD_REPEATING_BLOCK;
		end = repeatCount;
	}

	/**
	 * @return whether this block could be split
	 */
	public boolean isSplittable() {
		return splittable;
	}

	/**
	 * Set whether this block can be split.
	 * 
	 * @param splittable
	 */
	public void setSplittable(boolean splittable) {
		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			this.splittable = splittable;
		}
	}

	/**
	 * The data encoded in FARD
	 * 
	 * @return the data
	 */
	public byte[] getMessageBytes() {

		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return ByteArrayLib.encodeFARDData(theBytes);
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return ByteArrayLib.encodeFARDData(theBytes[0], end);
		} else {
			return ByteArrayLib.encodeFARDData(start, end);
		}
	}

	/**
	 * return the actual data, using oldArray if needed.
	 * 
	 * @param oldArray
	 * @return byte count
	 */
	byte[] getActualBytes(byte[] oldArray) {
		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return theBytes;
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {

			byte[] theExpandedData = new byte[end];

			for (int i = 0; i < end; i++) {

				theExpandedData[i] = theBytes[0];

			}

			return theExpandedData;

		} else {

			return ArrayUtils.subarray(oldArray, start, end);

		}
	}

	/**
	 * Data size in FARD format.
	 * 
	 * @return data size,
	 */
	public int messageSize() {

		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return 2 + 2 + theBytes.length;
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return 2 + 2 + theBytes.length;
		} else {
			return 2 + 2;
		}

	}

	/**
	 * Data size in reality.
	 * 
	 * @return data size
	 */
	public int actualSize() {

		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return theBytes.length;
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return end;
		} else {
			return (end - start);
		}

	}

	public String toString(String oldValue) {
		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return new String(theBytes);
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return new String(end + " * " + theBytes[0]);
		} else {
			return oldValue.substring(start, end);
		}

	}
	public String toString() {
		if (start == ByteArrayLib.FARD_DATA_BLOCK) {
			return new String("[RAW "+ theBytes.length +"]");
		} else if (start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return new String("[GROUP '"+ theBytes[0] + "' * " + end +"]");
		} else {
			return new String("[REF "+ start +"->" + end +"]");
		}

	}
	
	
	public boolean isOfType(short type) {
		if (type == ByteArrayLib.FARD_DATA_BLOCK && start == ByteArrayLib.FARD_DATA_BLOCK) {
			return true;
		} else if (type == ByteArrayLib.FARD_REPEATING_BLOCK && start == ByteArrayLib.FARD_REPEATING_BLOCK) {
			return true;
		} else if (type == ByteArrayLib.FARD_REFERENCE_BLOCK && start != ByteArrayLib.FARD_DATA_BLOCK && start !=  ByteArrayLib.FARD_REPEATING_BLOCK) {
		 return true;
		 
		}
		
		return false;
	}

	public boolean isMightHaveRepeats() {
		return mightHaveRepeats;
	}

	public void setMightHaveRepeats(boolean mightHaveRepeats) {
		this.mightHaveRepeats = mightHaveRepeats;
	}

}
