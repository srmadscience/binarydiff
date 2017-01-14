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

package org.voltdb.voltutil.binarydiff.test;

import java.util.Random;

import org.apache.commons.lang3.ArrayUtils;
import org.voltdb.voltutil.binarydiff.ArrayDiffIFace;
import org.voltdb.voltutil.binarydiff.ByteArrayLib;
import org.voltdb.voltutil.binarydiff.FastAndRuthlessDiffImpl;
import org.voltdb.voltutil.binarydiff.exceptions.BadInputException;
import org.voltdb.voltutil.binarydiff.exceptions.HashCodeMismatchException;
import org.voltdb.voltutil.binarydiff.exceptions.TooBigToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.TooSmallToDeltaException;
import org.voltdb.voltutil.binarydiff.exceptions.UneconomicToDiffException;

public class Runtest {

	private static final int TEST_COUNT = 1000;

	public static void main(String[] args) {

		byte[] byte42 = new byte[1];
		byte42[0] = 42;

		byte[] byte43 = new byte[1];
		byte43[0] = 43;

		String b4String = "The quick brown fox jumped over the lazy dog.";
		String afterString = "The quick brown fox walked over the lazy dog.";

		String longString1 = "Four score and seven years ago our fathers brought forth on this "
				+ "continent a new nation, conceived in liberty, and dedicated to the proposition"
				+ " that all men are created equal. Now we are engaged in a great civil war, "
				+ "testing whether that nation, or any nation so conceived and so dedicated, can "
				+ "long endure. We are met on a great battlefield of that war. We have come to "
				+ "dedicate a portion of that field, as a final resting place for those who here "
				+ "gave their lives that that nation might live. It is altogether fitting and proper"
				+ " that we should do this. But, in a larger sense, we can not dedicate, we can not "
				+ "consecrate, we can not hallow this ground. The brave men, living and dead, who "
				+ "struggled here, have consecrated it, far above our poor power to add or detract. "
				+ "The world will little note, nor long remember what we say here, but it can never "
				+ "forget what they did here. It is for us the living, rather, to be dedicated here "
				+ "to the unfinished work which they who fought here have thus far so nobly advanced. "
				+ "It is rather for us to be here dedicated to the great task remaining before us—that "
				+ "from these honored dead we take increased devotion to that cause for which they "
				+ "gave the last full measure of devotion that we here highly resolve that these dead "
				+ "shall not have died in vain that this nation, under God, shall have a new birth of "
				+ "freedom and that government of the people, by the people, for the people, shall "
				+ "not perish from the earth.";

		String longString2 = longString1.replace("people", "populace");

		byte[] binaryArray100_1 = new byte[100];
		byte[] binaryArray100_2 = new byte[100];

		for (int i = 0; i < binaryArray100_1.length; i++) {
			binaryArray100_1[i] = (byte) (i % 255);

			if ((i > 30 && i < 40) || (i > 80 && i < 90)) {
				binaryArray100_2[i] = (byte) (50);

			} else {
				binaryArray100_2[i] = (byte) (i % 255);

			}

		}

		byte[] binaryArray1024_1 = new byte[1024];
		byte[] binaryArray1024_2 = new byte[1024];

		for (int i = 0; i < binaryArray1024_1.length; i++) {
			binaryArray1024_1[i] = (byte) (i % 255);

			if (i > 100 && i < 120) {
				binaryArray1024_2[i] = (byte) (50);

			} else {
				binaryArray1024_2[i] = (byte) (i % 255);

			}

		}

		byte[] binaryArray2048_1 = new byte[2048];
		byte[] binaryArray2048_2 = new byte[2048];

		for (int i = 0; i < binaryArray1024_1.length; i++) {
			binaryArray2048_1[i] = (byte) (i % 255);

			if (i > 100 && i < 120) {
				binaryArray2048_2[i] = (byte) (50);

			} else {
				binaryArray2048_2[i] = (byte) (i % 255);

			}

		}

		byte[] binaryArray8192_1 = new byte[8192];
		byte[] binaryArray8192_2 = new byte[8192];

		for (int i = 0; i < binaryArray8192_1.length; i++) {
			binaryArray8192_1[i] = (byte) (i % 255);

			if (i > 100 && i < 120) {
				binaryArray8192_2[i] = (byte) (50);

			} else if (i > 500 && i < 520) {
				binaryArray8192_2[i] = (byte) (51);

			} else if (i > 3000 && i < 3100) {
				binaryArray8192_2[i] = (byte) (52);

			} else if (i > 3200 && i < 3225) {
				binaryArray8192_2[i] = (byte) (53);

			} else {
				binaryArray8192_2[i] = (byte) (i % 255);

			}

		}

		DiffTestCase[] testCases = {

				new DiffTestCase("one word replacement", b4String.getBytes(), afterString.getBytes(), false, false),
				new DiffTestCase("many word replacement", longString1.getBytes(), longString2.getBytes(), false, false),
				new DiffTestCase("1 byte 1 change test case", new byte[42], new byte[43], false, false),
				new DiffTestCase("differing binary array 100", binaryArray100_1, binaryArray100_2, false, false),
				new DiffTestCase("0 byte no change test case", new byte[0], new byte[0], true, false),
				new DiffTestCase("0 byte to 1 byte change test case", new byte[0], byte42, true, false),
				new DiffTestCase("1 byte to zero byte change test case", byte42, new byte[0], true, false),
				new DiffTestCase("1 byte 1 change test case2", new byte[43], new byte[42], false, false),
				new DiffTestCase("null test case", null, null, true, false),
				new DiffTestCase("identical binary array", binaryArray1024_1, binaryArray1024_1, false, false),
				new DiffTestCase("differing binary array", binaryArray1024_1, binaryArray1024_2, false, false),
				new DiffTestCase("differing binary array2", binaryArray2048_1, binaryArray2048_2, false, false),
				new DiffTestCase("differing binary array3", binaryArray8192_1, binaryArray8192_2, false, false),
				new DiffTestCase("differing binary length 1 ", binaryArray8192_1, binaryArray8192_2, false, false),

		};
		
		
		ArrayTestDataItem[] randomTestData = new ArrayTestDataItem[100000];
		
		Random r = new Random(0);
		int randomBlockCount = 15;
		int randomBlockSize = 30;
		
		for (int i=0; i < randomTestData.length; i++)
		{
			randomTestData[i] = new ArrayTestDataItem();
			r.nextBytes(randomTestData[i].oldValue);
			
			for (int j=0; j < randomTestData[i].oldValue.length; j++) {
				randomTestData[i].newValue[j] = randomTestData[i].oldValue[j];
			}
			
			for (int j=0; j < randomBlockCount; j++) {
				int randomBlockStart = r.nextInt(randomTestData[i].newValue.length - randomBlockSize);
				int randomBlockEnd = randomBlockStart + r.nextInt(randomBlockSize);
				
				
				for (int z=randomBlockStart; z <= randomBlockEnd; z++) {
					randomTestData[i].newValue[z] = (byte) (r.nextInt() % 255);
				}
			}
			
			
			
		}
		
		

		ArrayDiffIFace[] testImpls = { new FastAndRuthlessDiffImpl() };

		boolean retCode = true;

		for (int i = 0; i < testImpls.length; i++) {

			System.out.println("Test " + testImpls[i].getClass().getName());

			for (int j = 0; j < testCases.length; j++) {
				System.out.println("Test " + testCases[j].name);

				try {
					byte[] diffValue = testImpls[i].calculateDiff(testCases[j].source, testCases[j].target, 100);

					try {
						byte[] actualResult = testImpls[i].applyDiff(testCases[j].source, diffValue,
						ByteArrayLib.deterministicHashcode(testCases[j].source));

						if (!ArrayUtils.isEquals(testCases[j].target, actualResult)) {
							retCode = false;
							System.err.println("Arrays differ...");
							System.out.println(new String(testCases[j].target));
							System.out.println(new String(actualResult));
						}
					} catch (HashCodeMismatchException e) {
						retCode = false;
						e.printStackTrace();
					}

					if (testCases[j].tooSmall) {
						System.err.println("Didn't fail when was supposed to...");
						retCode = false;
					}

					if (testCases[j].uneconomic) {
						System.err.println("Didn't fail when was supposed to...");
						retCode = false;
					}

				} catch (TooSmallToDeltaException e) {
					if (!testCases[j].tooSmall) {
						System.err.println("Failed TooSmallToDeltaException when wasn't supposed to...");
						retCode = false;
					}
				} catch (UneconomicToDiffException e1) {
					if (!testCases[j].uneconomic) {
						System.err.println("Failed UneconomicToDiffException when wasn't supposed to...");
						retCode = false;
					}
				} catch (TooBigToDeltaException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (BadInputException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

			System.out.println(testImpls[i]);
		}

		if (retCode) {
			System.out.println("Pass");
		} else {
			System.err.println("Fail");
		}

		// Speed test

		boolean decode = false;
		for (int j = 0; j < testImpls.length; j++) {
			

			System.out.println(testImpls[j].getClass().getName());

			for (int sz = 8192; sz > 512; sz -= 512) {
				System.out.println("Size = " + sz);

				binaryArray8192_1 = ArrayUtils.subarray(binaryArray8192_1, 0, sz);
				binaryArray8192_2 = ArrayUtils.subarray(binaryArray8192_2, 0, sz);

				long start = System.currentTimeMillis();

					for (int i = 0; i < TEST_COUNT; i++) {
						try {
							byte[] diffValue = testImpls[j].calculateDiff(binaryArray8192_1, binaryArray8192_2, 100);

							try {
								if (decode) {
								byte[] actualResult = testImpls[j].applyDiff(binaryArray8192_1, diffValue,
										ByteArrayLib.deterministicHashcode(testCases[j].source));
			
								if (!ArrayUtils.isEquals(testCases[j].target, actualResult)) {
									retCode = false;
									System.err.println("Arrays differ...");
									System.out.println(new String(testCases[j].target));
									System.out.println(new String(actualResult));
								}
							
								
								}
								// }
							} catch (HashCodeMismatchException e) {
								retCode = false;
								e.printStackTrace();
							}

						} catch (TooSmallToDeltaException e) {
							if (!testCases[j].tooSmall) {
								// e.printStackTrace();
								retCode = false;
							}
						} catch (UneconomicToDiffException e1) {
							if (!testCases[j].uneconomic) {
								// e1.printStackTrace();
								retCode = false;
							}
						} catch (TooBigToDeltaException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (BadInputException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}

					long elapsed = System.currentTimeMillis() - start;

					System.out.println("Took " + elapsed + "ms to do " + TEST_COUNT);
				
			}
			System.out.println(testImpls[j]);
			
			
		}
		
		
		
		// Speed test II

		System.out.println("TEST 2");
		for (int j = 0; j < testImpls.length; j++) {
			
			long start = System.currentTimeMillis();

			System.out.println(testImpls[j].getClass().getName());

			for (int q=0; q < randomTestData.length; q++) {
		

						try {
							byte[] diffValue = testImpls[j].calculateDiff(randomTestData[q].oldValue,randomTestData[q].newValue, 100);

							try {
								if (decode) {
								byte[] actualResult = testImpls[j].applyDiff(randomTestData[q].oldValue, diffValue,
										ByteArrayLib.deterministicHashcode(randomTestData[q].oldValue));
								}
								// }
							} catch (HashCodeMismatchException e) {
								retCode = false;
								e.printStackTrace();
							}

						} catch (TooSmallToDeltaException e) {
							if (!testCases[j].tooSmall) {
								// e.printStackTrace();
								retCode = false;
							}
						} catch (UneconomicToDiffException e1) {
							if (!testCases[j].uneconomic) {
								// e1.printStackTrace();
								retCode = false;
							}
						} catch (TooBigToDeltaException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (BadInputException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					


				
			}
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Took " + elapsed + "ms to do " + randomTestData.length);
			System.out.println(testImpls[j]);
			
			
		}
		
		
	}

}
