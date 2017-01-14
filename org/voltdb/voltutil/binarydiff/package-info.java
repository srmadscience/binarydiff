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

/**
 * This package contains FastAndRuthlessDiffImpl, which is used to reduce
 * network bandwidth when working with byte[] objects that are being changed
 * slightly by a client before being sent back to a server.
 * <p>
 * Alternative binary diff packages go for maximum compression at the expense of
 * CPU time. FastAndRuthlessDiffImpl seeks a compromise between compression and
 * speed.
 * <p>
 * Sample usage - client side:
 * 
 * <pre>
 * 
 * 	{@code
 * final String key = "foo";
 * final int oldHashcode = FastAndRuthlessDiffImpl.deterministicHashcode(oldArray);
 * final int maxNumberOfChanges = 12;
 * 
 * byte[] delta = i.calculateDiff(oldArray, newArray, maxNumberOfChanges);
 * ClientResponse r = client.callProcedure("DiffedUpdate", key, delta, oldHashcode);
 *} 
 * </PRE>
 * 
 * Sample usage - server side:
 * 
 * <pre>
 * 
 * 	{@code
* import org.voltdb.SQLStmt;
*
* import org.voltdb.VoltProcedure;
* import org.voltdb.VoltTable;
*
* import com.voltdb.voltutil.server.*;
* import com.voltdb.voltutil.binarydiff.*;
* import com.voltdb.voltutil.binarydiff.exceptions.HashCodeMismatchException;
*
* public class DiffedUpdate extends VoltProcedure {
*
*       private static final long HASHCODE_MISMATCH = -2;
*       private static final long NO_SUCH_KEY = -1;
*       private static final long UPDATE_OK = 0;
*
*       public final SQLStmt queryStmt = new SQLStmt("SELECT value FROM store WHERE key = ?;");
*       public final SQLStmt updateStmt = new SQLStmt("UPDATE store SET value = ? WHERE key = ?;");
*
*       FastAndRuthlessDiffImpl theDiffImpl = new FastAndRuthlessDiffImpl();
*
*       public VoltTable[] run(String key, byte[] deltaValue, int hashcode) {
*
*               // Define a single row output table
*               VoltTable statusTable = ProcedureHelper.GetStatusTable();
*               VoltTable[] results = { statusTable };
*
*               voltQueueSQL(queryStmt, key);
*
*               final VoltTable[] queryResults = voltExecuteSQL();
*
*               queryResults[0].resetRowPosition();
*               if (queryResults[0].advanceRow()) {
*
*                       final byte[] oldPayload = queryResults[0].getVarbinary("VALUE");
*                       byte[] newPayload = null;
*
*                       try {
*                               newPayload = theDiffImpl.applyDiff(oldPayload, deltaValue, hashcode);
*                               voltQueueSQL(updateStmt, newPayload, key);
*                               voltExecuteSQL(true);
*                               ProcedureHelper.setStatus(statusTable, UPDATE_OK, null);
*
*                       } catch (HashCodeMismatchException e) {
*                               ProcedureHelper.setStatus(statusTable, HASHCODE_MISMATCH,
*                                               "Got " + hashcode + ", needed " + oldPayload.hashCode());
*                       }
*
*               } else {
*
*                       // There is no current row to diff.
*                       ProcedureHelper.setStatus(statusTable, NO_SUCH_KEY, "No such Key");
*               }
*
*               return results;
*
*       }
*
* }
* 
 *} 
 * </PRE>
* 
* @author drolfe@voltdb.com
*
*/


package org.voltdb.voltutil.binarydiff;

