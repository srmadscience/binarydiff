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

import org.voltdb.voltutil.binarydiff.exceptions.BadInputException;
import org.voltdb.voltutil.binarydiff.exceptions.HashCodeMismatchException;
import org.voltdb.voltutil.binarydiff.exceptions.TooBigToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.TooSmallToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.UneconomicToDiffException;

/**
 * Interface for classes that do 'diff' for byte[] data.
 * 
 * @author drolfe@voltdb.com
 *
 */
public interface ArrayDiffIFace {

	/**
	 * @param oldArray
	 *            An arbitrary length of byte[] that could be null, byte[0],
	 *            byte[n]...
	 * @param newArray
	 *            An arbitrary length of byte[] that could be null, byte[0],
	 *            byte[n]...
	 * @param maxSplits How any differences newArray contains - useful for optimization
	 * @return A byte[] which can be used to turn oldArray into newArray... How
	 *         this happens is up to the implementing class...
	 * @throws TooSmallToDeltaException too small to encode
	 * @throws UneconomicToDiffException uneconomic to encode - encoded version is bugger than source
	 * @throws TooBigToDeltaException Too big eo encode
	 * @throws BadInputException miscellaneous other failure
	 */
	public byte[] calculateDiff(byte[] oldArray, byte[] newArray, int maxSplits)
			throws TooSmallToDeltaException, UneconomicToDiffException, TooBigToDeltaException, BadInputException;

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
	 * @throws HashCodeMismatchException the hashcode passed in doesn't match the existing data's. 
	 * @throws BadInputException miscellaneous other failure
	 */
	public byte[] applyDiff(byte[] inArray, byte[] inDiff, int targetHashCode)
			throws HashCodeMismatchException, BadInputException;

}