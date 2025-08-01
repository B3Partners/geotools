/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.shapefile;

import static org.geotools.data.shapefile.files.ShpFileType.PRJ;
import static org.geotools.data.shapefile.files.ShpFileType.SHP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.geotools.TestData;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.files.StorageFile;
import org.junit.Before;
import org.junit.Test;

public class ShpFilesStreamTest implements org.geotools.data.shapefile.files.FileWriter {

    private String typeName;
    private Map<ShpFileType, File> map;
    private ShpFiles files;

    @Before
    public void setUp() throws Exception {
        map = ShpFilesTest.createFiles("shpFiles", ShpFileType.values(), false);

        typeName = map.get(SHP).getName();
        typeName = typeName.substring(0, typeName.lastIndexOf("."));

        files = new ShpFiles(map.get(SHP));
    }

    private void writeDataToFiles() throws IOException {
        Set<Entry<ShpFileType, File>> entries = map.entrySet();
        for (Entry<ShpFileType, File> entry : entries) {
            try (FileWriter out = new FileWriter(entry.getValue(), StandardCharsets.UTF_8)) {
                out.write(entry.getKey().name());
            }
        }
    }

    @Test
    public void testIsLocalURL() throws IOException {
        ShpFiles files = new ShpFiles("http://someurl.com/file.shp");
        assertFalse(files.isLocal());
    }

    @Test
    public void testIsLocalFiles() throws IOException {
        assertTrue(files.isLocal());
    }

    @Test
    public void testDelete() throws IOException {

        assertTrue(files.delete());

        for (File file : map.values()) {
            assertFalse(file.exists());
        }
    }

    @Test
    public void testExceptionGetInputStream() throws Exception {
        ShpFiles shpFiles = new ShpFiles(new URL("http://blah/blah.shp"));
        try {
            shpFiles.getInputStream(SHP, this);
            fail("maybe test is bad?  We want an exception here");
        } catch (Throwable e) {
            assertEquals(0, shpFiles.numberOfLocks());
        }
    }

    @Test
    public void testExceptionGetOutputStream() throws Exception {
        ShpFiles shpFiles = new ShpFiles(new URL("http://blah/blah.shp"));
        try {
            shpFiles.getOutputStream(SHP, this);
            fail("maybe test is bad?  We want an exception here");
        } catch (Throwable e) {
            assertEquals(0, shpFiles.numberOfLocks());
        }
    }

    @Test
    public void testExceptionGetWriteChannel() throws Exception {
        ShpFiles shpFiles = new ShpFiles(new URL("http://blah/blah.shp"));
        try {
            shpFiles.getWriteChannel(SHP, this);
            fail("maybe test is bad?  We want an exception here");
        } catch (Throwable e) {
            assertEquals(0, shpFiles.numberOfLocks());
        }
    }

    @Test
    public void testExceptionGetReadChannel() throws Exception {
        ShpFiles shpFiles = new ShpFiles(new URL("http://blah/blah.shp"));
        try {
            shpFiles.getReadChannel(SHP, this);
            fail("maybe test is bad?  We want an exception here");
        } catch (Throwable e) {
            assertEquals(0, shpFiles.numberOfLocks());
        }
    }

    @Test
    public void testGetInputStream() throws IOException {
        writeDataToFiles();

        ShpFileType[] types = ShpFileType.values();
        for (ShpFileType shpFileType : types) {
            String read = "";
            try (InputStream in = files.getInputStream(shpFileType, this);
                    InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                assertEquals(1, files.numberOfLocks());
                int current = reader.read();
                while (current != -1) {
                    read += (char) current;
                    current = reader.read();
                }
            } finally {
                assertEquals(0, files.numberOfLocks());
            }
            assertEquals(shpFileType.name(), read);
        }
    }

    @Test
    public void testGetWriteStream() throws IOException {

        ShpFileType[] types = ShpFileType.values();
        for (ShpFileType shpFileType : types) {

            try (OutputStream out = files.getOutputStream(shpFileType, this)) {
                assertEquals(1, files.numberOfLocks());
                out.write((byte) 2);
            } finally {
                assertEquals(0, files.numberOfLocks());
            }
        }
    }

    @Test
    public void testGetReadChannelFileChannel() throws IOException {
        writeDataToFiles();

        ShpFileType[] types = ShpFileType.values();
        for (ShpFileType shpFileType : types) {
            doRead(shpFileType);
        }
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void testGetReadChannelURL() throws IOException {
        URL url = TestData.url("shapes/statepop.shp");
        ShpFiles files = new ShpFiles(url);

        try (ReadableByteChannel read = files.getReadChannel(SHP, this)) {
            assertEquals(1, files.numberOfLocks());
        }

        assertEquals(0, files.numberOfLocks());
    }

    @SuppressWarnings({"PMD.CloseResource", "PMD.UseTryWithResources"}) // manual handling to try double close
    private void doRead(ShpFileType shpFileType) throws IOException {
        ReadableByteChannel in = files.getReadChannel(shpFileType, this);
        assertEquals(1, files.numberOfLocks());
        assertTrue(in instanceof FileChannel);

        ByteBuffer buffer = ByteBuffer.allocate(10);
        in.read(buffer);
        buffer.flip();
        String read = "";
        try {
            while (buffer.hasRemaining()) {
                read += (char) buffer.get();
            }
        } finally {
            in.close();
            // verify that you can close multiple times without bad things
            // happening
            in.close();
        }
        assertEquals(0, files.numberOfLocks());
        assertEquals(shpFileType.name(), read);
    }

    private void doWrite(ShpFileType shpFileType) throws IOException {
        WritableByteChannel out = files.getWriteChannel(shpFileType, this);

        try (out) {
            assertEquals(1, files.numberOfLocks());
            assertTrue(out instanceof FileChannel);
            ByteBuffer buffer = ByteBuffer.allocate(10);
            buffer.put(shpFileType.name().getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            out.write(buffer);
        }
        // verify that you can close multiple times without bad things
        // happening
        assertEquals(0, files.numberOfLocks());
    }

    @Test
    public void testGetWriteChannel() throws IOException {

        ShpFileType[] types = ShpFileType.values();
        for (ShpFileType shpFileType : types) {
            doWrite(shpFileType);
            doRead(shpFileType);
        }
    }

    @Test
    public void testGetStorageFile() throws Exception {
        StorageFile prj = files.getStorageFile(PRJ);
        assertTrue(prj.getFile().getName().startsWith(typeName));
        assertTrue(prj.getFile().getName().endsWith(".prj"));
    }

    @Test
    public void testGetTypeName() throws Exception {
        assertEquals(typeName, files.getTypeName());
    }

    @Override
    public String id() {
        return getClass().getName();
    }
}
