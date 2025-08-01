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
package org.geotools.data.shapefile.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;

/**
 * Encapsulates the idea of a file for writing data to and then later updating the original.
 *
 * @author jesse
 */
public final class StorageFile implements Comparable<StorageFile>, FileWriter {
    static final Logger LOGGER = Logging.getLogger(StorageFile.class);

    private final ShpFiles shpFiles;
    private final File tempFile;
    private final ShpFileType type;

    public StorageFile(ShpFiles shpFiles, File tempFile, ShpFileType type) {
        this.shpFiles = shpFiles;
        this.tempFile = tempFile;
        this.type = type;
    }

    /**
     * Returns the storage file
     *
     * @return the storage file
     */
    public File getFile() {
        return tempFile;
    }

    @SuppressWarnings("resource")
    public FileChannel getWriteChannel() throws IOException {
        return new RandomAccessFile(tempFile, "rw").getChannel();
    }

    /**
     * Replaces the file that the temporary file is acting as a transactional type cache for. Acts similar to a commit.
     *
     * @see #replaceOriginals(StorageFile...)
     */
    public void replaceOriginal() throws IOException {
        replaceOriginals(this);
    }

    /**
     * Takes a collection of StorageFiles and performs the replace functionality described in
     * {@link #replaceOriginal()}. However, all files that are part of the same {@link ShpFiles} are done within a lock
     * so all of the updates for all the Files of a Shapefile can be updated within a single lock.
     *
     * @param storageFiles files to execute the replace functionality.
     */
    public static void replaceOriginals(StorageFile... storageFiles) throws IOException {
        SortedSet<StorageFile> files = new TreeSet<>(Arrays.asList(storageFiles));

        ShpFiles currentShpFiles = null;
        URL shpURL = null;
        StorageFile locker = null;
        LinkedList<String> exceptionMessages = new LinkedList<>();
        try {

            for (StorageFile storageFile : files) {
                if (currentShpFiles != storageFile.shpFiles) {
                    // there's a new set of files so unlock old and lock new.
                    if (currentShpFiles != null) {
                        currentShpFiles.unlockWrite(shpURL, locker);
                    }
                    locker = storageFile;
                    currentShpFiles = storageFile.shpFiles;
                    shpURL = currentShpFiles.acquireWrite(ShpFileType.SHP, storageFile);
                }

                File storage = storageFile.getFile();
                URL url = currentShpFiles.acquireWrite(storageFile.type, storageFile);

                try {
                    File dest = URLs.urlToFile(url);

                    if (storage.equals(dest)) return;

                    if (dest.exists()) {
                        if (!dest.delete()) {
                            LOGGER.severe("Unable to delete the file: "
                                    + dest
                                    + " when attempting to replace with temporary copy.");
                        }
                    }

                    if (storage.exists() && !storage.renameTo(dest)) {
                        LOGGER.fine("Unable to rename temporary file to the file: "
                                + dest
                                + " when attempting to replace with temporary copy");
                        copyFile(storage, url, dest);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Unable to replace original file with temporary.", e);
                    exceptionMessages.add(e.getMessage());
                } finally {
                    storageFile.unlockWriteURL(url);

                    if (storage.exists()) {
                        storage.delete();
                    }
                }
            }
        } finally {
            if (currentShpFiles != null) {
                currentShpFiles.unlockWrite(shpURL, locker);
            }

            if (!exceptionMessages.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Couldn't replace original file with a temporary after a write operation.\nBecause of:\n");
                exceptionMessages.forEach(s -> sb.append("    ").append(s).append("\n"));
                throw new IOException(sb.toString());
            }
        }
    }

    @SuppressWarnings("resource")
    private static void copyFile(File storage, URL url, File dest) throws FileNotFoundException, IOException {
        try (FileChannel in = new FileInputStream(storage).getChannel();
                FileChannel out = new FileOutputStream(dest).getChannel()) {

            // magic number for Windows, 64Mb - 32Kb)
            int maxCount = 64 * 1024 * 1024 - 32 * 1024;
            long size = in.size();
            long position = 0;
            while (position < size) {
                position += in.transferTo(position, maxCount, out);
            }
        }
    }

    private void unlockWriteURL(URL url) {
        shpFiles.unlockWrite(url, this);
    }

    /** Just groups together files that have the same ShpFiles instance */
    @Override
    public int compareTo(StorageFile o) {
        // group together files that have the same shapefile instance
        if (this == o) {
            return 0;
        }

        // assume two StorageFile that do not share the same ShpFiles
        // are not given the same temp file
        return getFile().compareTo(o.getFile());
    }

    @Override
    public String toString() {
        return id();
    }

    @Override
    public String id() {
        return getClass().getSimpleName() + ": " + tempFile.getName();
    }
}
