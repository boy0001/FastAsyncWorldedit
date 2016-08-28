/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.jnbt;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.RunnableVal2;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class reads <strong>NBT</strong>, or <strong>Named Binary Tag</strong>
 * streams, and produces an object graph of subclasses of the {@code Tag}
 * object.
 *
 * <p>The NBT format was created by Markus Persson, and the specification may be
 * found at <a href="http://www.minecraft.net/docs/NBT.txt">
 * http://www.minecraft.net/docs/NBT.txt</a>.</p>
 */
public final class NBTInputStream implements Closeable {

    private final DataInput is;

    /**
     * Creates a new {@code NBTInputStream}, which will source its data
     * from the specified input stream.
     *
     * @param is the input stream
     * @throws IOException if an I/O error occurs
     */
    public NBTInputStream(InputStream is) throws IOException {
        this.is = new DataInputStream(is);
    }

    public NBTInputStream(DataInput di) {
        this.is = di;
    }

    /**
     * Reads an NBT tag from the stream.
     *
     * @return The tag that was read.
     * @throws IOException if an I/O error occurs.
     */
    public NamedTag readNamedTag() throws IOException {
        return readNamedTag(0);
    }

    /**
     * Reads an NBT from the stream.
     *
     * @param depth the depth of this tag
     * @return The tag that was read.
     * @throws IOException if an I/O error occurs.
     */
    private NamedTag readNamedTag(int depth) throws IOException {
        int type = is.readByte();
        return new NamedTag(readNamedTagName(type), readTagPayload(type, depth));
    }

    public Tag readTag() throws IOException {
        int type = is.readByte();
        return readTagPayload(type, 0);
    }

    public void readNamedTagLazy(RunnableVal2<String, RunnableVal2> getReader) throws IOException {
        int type = is.readByte();
        String name = readNamedTagName(type);
        RunnableVal2 reader = getReader.runAndGet(name, null).value2;
        if (reader != null) {
            reader.run(0, readTagPaylodRaw(type, 0));
            return;
        }
        readTagPaylodLazy(type, 0, name, getReader);
    }

    private String readNamedTagName(int type) throws IOException {
        String name;
        if (type != NBTConstants.TYPE_END) {
            int nameLength = is.readShort() & 0xFFFF;
            byte[] nameBytes = new byte[nameLength];
            is.readFully(nameBytes);
            return new String(nameBytes, NBTConstants.CHARSET);
        } else {
            return "";
        }
    }

    private byte[] buf;

    private void readTagPaylodLazy(int type, int depth, String node, RunnableVal2<String, RunnableVal2> getReader) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                return;
            case NBTConstants.TYPE_BYTE:
                is.skipBytes(1);
                return;
            case NBTConstants.TYPE_SHORT:
                is.skipBytes(2);
                return;
            case NBTConstants.TYPE_INT:
                is.skipBytes(4);
                return;
            case NBTConstants.TYPE_LONG:
                is.skipBytes(8);
                return;
            case NBTConstants.TYPE_FLOAT:
                is.skipBytes(4);
                return;
            case NBTConstants.TYPE_DOUBLE:
                is.skipBytes(8);
                return;
            case NBTConstants.TYPE_STRING:
                int length = is.readShort();
                is.skipBytes(length);
                return;
            case NBTConstants.TYPE_BYTE_ARRAY:
                RunnableVal2 reader = getReader.runAndGet(node + ".?", null).value2;
                length = is.readInt();
                if (reader != null) {
                    reader.run(length, NBTConstants.TYPE_BYTE);
                }
                reader = getReader.runAndGet(node + ".#", null).value2;
                if (reader == null) {
                    is.skipBytes(length);
                    return;
                }
                if (reader instanceof NBTStreamer.ByteReader) {
                    NBTStreamer.ByteReader byteReader = (NBTStreamer.ByteReader) reader;
                    int i = 0;
                    if (is instanceof InputStream) {
                        DataInputStream dis = (DataInputStream) is;
                        if (length > 720) {
                            if (buf == null) {
                                buf = new byte[720];
                            }
                            int left = length;
                            for (; left > 720; left -= 720) {
                                dis.read(buf);
                                for (byte b : buf) {
                                    byteReader.run(i++, b & 0xFF);
                                }
                            }
                        }
                        for (; i < length; i++) {
                            byteReader.run(i, dis.read());
                        }
                    } else {
                        if (length > 720) {
                            if (buf == null) {
                                buf = new byte[720];
                            }
                            int left = length;
                            for (; left > 720; left -= 720) {
                                is.readFully(buf);
                                for (byte b : buf) {
                                    byteReader.run(i++, b & 0xFF);
                                }
                            }
                        }
                        for (; i < length; i++) {
                            byteReader.run(i, is.readByte() & 0xFF);
                        }
                    }
                } else {
                    for (int i = 0; i < length; i++) {
                        reader.run(i, is.readByte());
                    }
                }
                return;
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                length = is.readInt();
                reader = getReader.runAndGet(node + ".?", null).value2;
                if (reader != null) {
                    reader.run(length, childType);
                }
                node += ".#";
                reader = getReader.runAndGet(node, null).value2;
                depth++;
                if (reader == null) {
                    for (int i = 0; i < length; ++i) {
                        readTagPaylodLazy(childType, depth, node, getReader);
                    }
                    return;
                }
                for (int i = 0; i < length; ++i) {
                    reader.run(i, readTagPayload(childType, depth));
                }
                return;
            case NBTConstants.TYPE_COMPOUND:
                depth++;
                for (int i = 0;;i++) {
                    childType = is.readByte();
                    if (childType == NBTConstants.TYPE_END) {
                        return;
                    }
                    String name = readNamedTagName(childType);
                    String childNode = node + "." + name;
                    reader = getReader.runAndGet(childNode, null).value2;
                    if (reader == null) {
                        readTagPaylodLazy(childType, depth, childNode, getReader);
                        continue;
                    }
                    reader.run(i, readTagPaylodRaw(childType, depth));
                }
            case NBTConstants.TYPE_INT_ARRAY:
                length = is.readInt();
                reader = getReader.runAndGet(node + ".?", null).value2;
                if (reader != null) {
                    reader.run(length, NBTConstants.TYPE_INT);
                }
                reader = getReader.runAndGet(node + ".#", null).value2;
                if (reader == null) {
                    is.skipBytes(length << 2);
                    return;
                }
                for (int i = 0; i < length; i++) {
                    reader.run(i, is.readInt());
                }
                return;
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    private Object readTagPaylodRaw(int type, int depth) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return null;
                }
            case NBTConstants.TYPE_BYTE:
                return (is.readByte());
            case NBTConstants.TYPE_SHORT:
                return (is.readShort());
            case NBTConstants.TYPE_INT:
                return (is.readInt());
            case NBTConstants.TYPE_LONG:
                return (is.readLong());
            case NBTConstants.TYPE_FLOAT:
                return (is.readFloat());
            case NBTConstants.TYPE_DOUBLE:
                return (is.readDouble());
            case NBTConstants.TYPE_BYTE_ARRAY:
                int length = is.readInt();
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return (bytes);
            case NBTConstants.TYPE_STRING:
                length = is.readShort();
                bytes = new byte[length];
                is.readFully(bytes);
                return (new String(bytes, NBTConstants.CHARSET));
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                length = is.readInt();
                List<Tag> tagList = new ArrayList<Tag>();
                for (int i = 0; i < length; ++i) {
                    Tag tag = readTagPayload(childType, depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    tagList.add(tag);
                }
                return (tagList);
            case NBTConstants.TYPE_COMPOUND:
                Map<String, Tag> tagMap = new HashMap<String, Tag>();
                while (true) {
                    NamedTag namedTag = readNamedTag(depth + 1);
                    Tag tag = namedTag.getTag();
                    if (tag instanceof EndTag) {
                        break;
                    } else {
                        tagMap.put(namedTag.getName(), tag);
                    }
                }
                return (tagMap);
            case NBTConstants.TYPE_INT_ARRAY:
                length = is.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readInt();
                }
                return (data);
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    /**
     * Reads the payload of a tag given the type.
     *
     * @param type the type
     * @param depth the depth
     * @return the tag
     * @throws IOException if an I/O error occurs.
     */
    private Tag readTagPayload(int type, int depth) throws IOException {
        switch (type) {
            case NBTConstants.TYPE_END:
                if (depth == 0) {
                    throw new IOException(
                            "TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                } else {
                    return new EndTag();
                }
            case NBTConstants.TYPE_BYTE:
                return new ByteTag(is.readByte());
            case NBTConstants.TYPE_SHORT:
                return new ShortTag(is.readShort());
            case NBTConstants.TYPE_INT:
                return new IntTag(is.readInt());
            case NBTConstants.TYPE_LONG:
                return new LongTag(is.readLong());
            case NBTConstants.TYPE_FLOAT:
                return new FloatTag(is.readFloat());
            case NBTConstants.TYPE_DOUBLE:
                return new DoubleTag(is.readDouble());
            case NBTConstants.TYPE_BYTE_ARRAY:
                int length = is.readInt();
                byte[] bytes = new byte[length];
                is.readFully(bytes);
                return new ByteArrayTag(bytes);
            case NBTConstants.TYPE_STRING:
                length = is.readShort();
                bytes = new byte[length];
                is.readFully(bytes);
                return new StringTag(new String(bytes, NBTConstants.CHARSET));
            case NBTConstants.TYPE_LIST:
                int childType = is.readByte();
                length = is.readInt();
                List<Tag> tagList = new ArrayList<Tag>();
                for (int i = 0; i < length; ++i) {
                    Tag tag = readTagPayload(childType, depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    tagList.add(tag);
                }

                return new ListTag(NBTUtils.getTypeClass(childType), tagList);
            case NBTConstants.TYPE_COMPOUND:
                Map<String, Tag> tagMap = new HashMap<String, Tag>();
                while (true) {
                    NamedTag namedTag = readNamedTag(depth + 1);
                    Tag tag = namedTag.getTag();
                    if (tag instanceof EndTag) {
                        break;
                    } else {
                        tagMap.put(namedTag.getName(), tag);
                    }
                }

                return new CompoundTag(tagMap);
            case NBTConstants.TYPE_INT_ARRAY:
                length = is.readInt();
                int[] data = new int[length];
                for (int i = 0; i < length; i++) {
                    data[i] = is.readInt();
                }
                return new IntArrayTag(data);
            default:
                throw new IOException("Invalid tag type: " + type + ".");
        }
    }

    @Override
    public void close() throws IOException {
        if (is instanceof AutoCloseable) {
            try {
                ((AutoCloseable) is).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Class<?> inject() {
        return NBTInputStream.class;
    }
}
