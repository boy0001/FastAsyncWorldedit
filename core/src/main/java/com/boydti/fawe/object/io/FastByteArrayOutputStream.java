package com.boydti.fawe.object.io;

/*
 * #%L
 * ch-commons-util
 * %%
 * Copyright (C) 2012 Cloudhopper by Twitter
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Originally found here: http://www.java2s.com/Code/Java/File-Input-Output/AspeedyimplementationofByteArrayOutputStream.htm
 *
 * A speedy implementation of ByteArrayOutputStream. It's not synchronized, and it
 * does not copy buffers when it's expanded. There's also no copying of the internal buffer
 * if it's contents is extracted with the writeTo(stream) method.
 *
 * @author Rickard berg
 * @author Brat Baker (Atlassian)
 * @author Alexey
 * @version $Date: 2008-01-19 10:09:56 +0800 (Sat, 19 Jan 2008) $ $Id: FastByteArrayOutputStream.java 3000 2008-01-19 02:09:56Z tm_jee $
 */
public class FastByteArrayOutputStream extends OutputStream {

    // Static --------------------------------------------------------
    private static final int DEFAULT_BLOCK_SIZE = 16384;
    private LinkedList<byte[]> buffers;
    // Attributes ----------------------------------------------------
    // internal buffer
    private byte[] buffer;
    // is the stream closed?
    private boolean closed;
    private int blockSize;
    private int index;
    private int size;

    // Constructors --------------------------------------------------
    public FastByteArrayOutputStream() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public FastByteArrayOutputStream(int aSize) {
        blockSize = aSize;
        buffer = new byte[blockSize];
    }

    public int getSize() {
        return size + index;
    }

    @Override
    public void close() {
        closed = true;
    }

    public byte[] toByteArray() {
        return toByteArray(false);
    }

    public byte[] toByteArray(boolean delete) {
        byte[] data = new byte[getSize()];

        // Check if we have a list of buffers
        int pos = 0;
        buffer = null;
        if (buffers != null) {
            Iterator iter = buffers.iterator();

            while (iter.hasNext()) {
                byte[] bytes = (byte[])iter.next();
                if (delete) {
                    iter.remove();
                }
                System.arraycopy(bytes, 0, data, pos, blockSize);
                pos += blockSize;
            }
        }

        // write the internal buffer directly
        System.arraycopy(buffer, 0, data, pos, index);

        return data;
    }

    @Override
    public String toString() {
        return new String(toByteArray());
    }

    // OutputStream overrides ----------------------------------------
    public void write(int datum) throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        } else {
            if (index == blockSize) {
                addBuffer();
            }

            // store the byte
            buffer[index++] = (byte) datum;
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (data == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || ((offset + length) > data.length) || (length < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (closed) {
            throw new IOException("Stream closed");
        } else {
            if ((index + length) > blockSize) {
                int copyLength;

                do {
                    if (index == blockSize) {
                        addBuffer();
                    }

                    copyLength = blockSize - index;

                    if (length < copyLength) {
                        copyLength = length;
                    }

                    System.arraycopy(data, offset, buffer, index, copyLength);
                    offset += copyLength;
                    index += copyLength;
                    length -= copyLength;
                } while (length > 0);
            } else {
                // Copy in the subarray
                System.arraycopy(data, offset, buffer, index, length);
                index += length;
            }
        }
    }

    // Public
    public void writeTo(OutputStream out) throws IOException {
        // Check if we have a list of buffers
        if (buffers != null) {
            Iterator iter = buffers.iterator();

            while (iter.hasNext()) {
                byte[] bytes = (byte[]) iter.next();
                out.write(bytes, 0, blockSize);
            }
        }

        // write the internal buffer directly
        out.write(buffer, 0, index);
    }

    public void writeTo(RandomAccessFile out) throws IOException {
        // Check if we have a list of buffers
        if (buffers != null) {
            Iterator iter = buffers.iterator();

            while (iter.hasNext()) {
                byte[] bytes = (byte[]) iter.next();
                out.write(bytes, 0, blockSize);
            }
        }

        // write the internal buffer directly
        out.write(buffer, 0, index);
    }

    public void writeTo(Writer out, String encoding) throws IOException {
        /*
        There is design tradeoff between being fast, correct and using too much memory when decoding bytes to strings.

        The rules are thus :

        1. if there is only one buffer then its a simple String conversion

        REASON : Fast!!!

        2. uses full buffer allocation annd System.arrayCopy() to smooosh together the bytes
        and then use String conversion

        REASON : Fast at the expense of a known amount of memory (eg the used memory * 2)
         */
        if (buffers != null) {
            // RULE 2 : a balance between using some memory and speed
            writeToViaSmoosh(out, encoding);
        } else {
            // RULE 1 : fastest!
            writeToViaString(out, encoding);
        }
    }

    /**
     * This can <b>ONLY</b> be called if there is only a single buffer to write, instead
     * use {@link #writeTo(java.io.Writer, String)}, which auto detects if
     * {@link #writeToViaString(java.io.Writer, String)} is to be used or
     * {@link #writeToViaSmoosh(java.io.Writer, String)}.
     *
     * @param out      the JspWriter
     * @param encoding the encoding
     * @throws IOException
     */
    void writeToViaString(Writer out, String encoding) throws IOException {
        byte[] bufferToWrite = buffer; // this is always the last buffer to write
        int bufferToWriteLen = index;  // index points to our place in the last buffer
        writeToImpl(out, encoding, bufferToWrite, bufferToWriteLen);
    }

    /**
     * This is recommended to be used where there's more than 1 buffer to write, instead
     * use {@link #writeTo(java.io.Writer, String)} which auto detects if
     * {@link #writeToViaString(java.io.Writer, String)} is to be used or
     * {@link #writeToViaSmoosh(java.io.Writer, String)}.
     *
     * @param out
     * @param encoding
     * @throws IOException
     */
    void writeToViaSmoosh(Writer out, String encoding) throws IOException {
        byte[] bufferToWrite = toByteArray();
        int bufferToWriteLen = bufferToWrite.length;
        writeToImpl(out, encoding, bufferToWrite, bufferToWriteLen);
    }

    /**
     * Write <code>bufferToWriteLen</code> of bytes from <code>bufferToWrite</code> to
     * <code>out</code> encoding it at the same time.
     *
     * @param out
     * @param encoding
     * @param bufferToWrite
     * @param bufferToWriteLen
     * @throws IOException
     */
    private void writeToImpl(Writer out, String encoding, byte[] bufferToWrite, int bufferToWriteLen)
            throws IOException {
        String writeStr;
        if (encoding != null) {
            writeStr = new String(bufferToWrite, 0, bufferToWriteLen, encoding);
        } else {
            writeStr = new String(bufferToWrite, 0, bufferToWriteLen);
        }
        out.write(writeStr);
    }

    /**
     * Create a new buffer and store the
     * current one in linked list
     */
    protected void addBuffer() {
        if (buffers == null) {
            buffers = new LinkedList<byte[]>();
        }

        buffers.addLast(buffer);

        buffer = new byte[blockSize];
        size += index;
        index = 0;
    }
}