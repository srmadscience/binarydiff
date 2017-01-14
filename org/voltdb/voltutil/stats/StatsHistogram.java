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

package org.voltdb.voltutil.stats;

public class StatsHistogram {

	int maxSize = 1000;
	double[] latencyHistogram = new double[0];
	String[] latencyComment = new String[0];
	boolean isRolledOver = false;

	public StatsHistogram(int maxSize) {

		this.maxSize = maxSize;

		latencyHistogram = new double[maxSize];
		latencyComment = new String[maxSize];

		for (int i = 0; i < latencyComment.length; i++) {
			latencyComment[i] = "";
		}

		resetLatency();

	}

	public void resetLatency() {
		for (int i = 0; i < maxSize; i++) {
			latencyHistogram[i] = 0;
		}
	}

	public void report(int latency, String comment) {
		if (latency < 0) {
			latency = 0;
		}

		if (latency < maxSize) {
			if (latencyHistogram[latency] < Integer.MAX_VALUE) {
				latencyHistogram[latency]++;
			} else {
				isRolledOver = true;
				resetLatency();
			}

		} else {

			if (latencyHistogram[maxSize - 1] < Integer.MAX_VALUE) {
				latencyHistogram[maxSize - 1]++;
			} else {
				isRolledOver = true;
				resetLatency();
			}

		}

		if (comment != null && comment.length() > 0) {
			if (latency < maxSize) {
				if (latencyHistogram[latency] < Integer.MAX_VALUE) {
					latencyComment[latency] = comment;
				}

			} else {

				latencyComment[maxSize - 1] = comment;

			}
		}

	}

	public double[] getLatencyHistogram() {
		return latencyHistogram;
	}

	public String[] getLatencyComment() {
		return latencyComment;
	}

	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();

		if (isRolledOver) {
			b.append("ROLLED OVER\n");
		}

		for (int i = 0; i < latencyHistogram.length; i++) {
			if (latencyHistogram[i] != 0) {
				b.append(i);
				b.append("\t");
				b.append(latencyHistogram[i]);
				b.append("\t");
				b.append(latencyComment[i]);
				b.append("\n");
			}
		}

		return b.toString();
	}

	public boolean isHasRolledOver() {
		return isRolledOver;
	}

}
