//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2018 Getdown authors
// https://github.com/bekoenig/getdown/blob/master/LICENSE

package io.github.bekoenig.getdown.data;

import io.github.bekoenig.getdown.util.FileUtil;
import io.github.bekoenig.getdown.util.ProgressObserver;
import io.github.bekoenig.getdown.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Models a single file resource used by an {@link Application}.
 */
public class Resource implements Comparable<Resource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Resource.class);

    /**
     * Defines special attributes for resources.
     */
    public enum Attr {
        /**
         * Indicates that the resource should be unpacked.
         */
        UNPACK,
        /**
         * If present, when unpacking a resource, any directories created by the newly
         * unpacked resource will first be cleared of files before unpacking.
         */
        CLEAN,
        /**
         * Indicates that the resource should be marked executable.
         */
        EXEC,
        /**
         * Indicates that the resource should be downloaded before a UI is displayed.
         */
        PRELOAD,
        /**
         * Indicates that the resource is a jar containing native libs.
         */
        NATIVE
    }

    public static final EnumSet<Attr> NORMAL = EnumSet.noneOf(Attr.class);
    public static final EnumSet<Attr> UNPACK = EnumSet.of(Attr.UNPACK);
    public static final EnumSet<Attr> EXEC = EnumSet.of(Attr.EXEC);
    public static final EnumSet<Attr> PRELOAD = EnumSet.of(Attr.PRELOAD);
    public static final EnumSet<Attr> NATIVE = EnumSet.of(Attr.NATIVE);

    /**
     * Computes the MD5 hash of the supplied file.
     *
     * @param version the version of the digest protocol to use.
     */
    public static String computeDigest(int version, File target, MessageDigest md,
                                       ProgressObserver obs)
        throws IOException {
        md.reset();
        byte[] buffer = new byte[DIGEST_BUFFER_SIZE];
        int read;

        boolean isZip = isJar(target) || isZip(target); // jar is a zip too

        // if this is a jar, we need to compute the digest in a "timestamp and file order" agnostic
        // manner to properly correlate jardiff patched jars with their unpatched originals
        if (isZip) {
            // if this is a compressed zip file, uncompress it to compute the zip file digest
            try (ZipFile zip = new ZipFile(target)) {
                List<? extends ZipEntry> entries = Collections.list(zip.entries());
                entries.sort(ENTRY_COMP);

                int eidx = 1;
                for (ZipEntry entry : entries) {

                    // old versions of the digest code skipped metadata
                    if (version >= 2 || !entry.getName().startsWith("META-INF")) {
                        try (InputStream in = zip.getInputStream(entry)) {
                            while ((read = in.read(buffer)) != -1) {
                                md.update(buffer, 0, read);
                            }
                        }
                    }

                    updateProgress(obs, eidx++, entries.size());
                }

                return StringUtil.hexlate(md.digest());
            } catch (ZipException e) {
                LOGGER.warn("Zip digest computation for {} failed. Falling back to binary mode.",
                    target, e);
                md.reset();
            }
        }

        long totalSize = target.length(), position = 0L;
        try (FileInputStream fin = new FileInputStream(target)) {
            while ((read = fin.read(buffer)) != -1) {
                md.update(buffer, 0, read);
                position += read;
                updateProgress(obs, position, totalSize);
            }
        }

        return StringUtil.hexlate(md.digest());
    }

    /**
     * Returns whether {@code file} is a {@code zip} file.
     */
    public static boolean isZip(File file) {
        String path = file.getName();
        return path.endsWith(".zip") || path.endsWith(".zip_new");
    }

    /**
     * Returns whether {@code file} is a {@code jar} file.
     */
    public static boolean isJar(File file) {
        String path = file.getName();
        return path.endsWith(".jar") || path.endsWith(".jar_new");
    }

    /**
     * Creates a resource with the supplied remote URL and local path.
     */
    public Resource(String path, URL remote, File local, EnumSet<Attr> attrs) {
        _path = path;
        _remote = remote;
        _local = local;
        _localNew = new File(local.toString() + "_new");
        _marker = new File(_local.getPath() + "v");

        _attrs = attrs;
        _isZip = isJar(local) || isZip(local);
        boolean unpack = attrs.contains(Attr.UNPACK);
        if (unpack && _isZip) {
            _unpacked = _local.getParentFile();
        }
    }

    /**
     * Returns the path associated with this resource.
     */
    public String getPath() {
        return _path;
    }

    /**
     * Returns the local location of this resource.
     */
    public File getLocal() {
        return _local;
    }

    /**
     * Returns the location of the to-be-installed new version of this resource.
     */
    public File getLocalNew() {
        return _localNew;
    }

    /**
     * Returns the location of the unpacked resource.
     */
    public File getUnpacked() {
        return _unpacked;
    }

    /**
     * Returns the final target of this resource, whether it has been unpacked or not.
     */
    public File getFinalTarget() {
        return shouldUnpack() ? getUnpacked() : getLocal();
    }

    /**
     * Returns the remote location of this resource.
     */
    public URL getRemote() {
        return _remote;
    }

    /**
     * Returns true if this resource should be unpacked as a part of the validation process.
     */
    public boolean shouldUnpack() {
        return _attrs.contains(Attr.UNPACK) && !SysProps.noUnpack();
    }

    /**
     * Returns true if this resource should be pre-downloaded.
     */
    public boolean shouldPredownload() {
        return _attrs.contains(Attr.PRELOAD);
    }

    /**
     * Returns true if this resource is a native lib jar.
     */
    public boolean isNative() {
        return _attrs.contains(Attr.NATIVE);
    }

    /**
     * Computes the MD5 hash of this resource's underlying file.
     * <em>Note:</em> This is both CPU and I/O intensive.
     *
     * @param version the version of the digest protocol to use.
     */
    public String computeDigest(int version, MessageDigest md, ProgressObserver obs)
        throws IOException {
        File file;
        if (_local.toString().toLowerCase(Locale.ROOT).endsWith(Application.CONFIG_FILE)) {
            file = _local;
        } else {
            file = _localNew.exists() ? _localNew : _local;
        }
        return computeDigest(version, file, md, obs);
    }

    /**
     * Returns true if this resource has an associated "validated" marker
     * file.
     */
    public boolean isMarkedValid() {
        if (!_local.exists()) {
            clearMarker();
            return false;
        }
        return _marker.exists();
    }

    /**
     * Creates a "validated" marker file for this resource to indicate
     * that its MD5 hash has been computed and compared with the value in
     * the digest file.
     *
     * @throws IOException if we fail to create the marker file.
     */
    public void markAsValid()
        throws IOException {
        _marker.createNewFile();
    }

    /**
     * Removes any "validated" marker file associated with this resource.
     */
    public void clearMarker() {
        if (_marker.exists() && !FileUtil.deleteHarder(_marker)) {
            LOGGER.warn("Failed to erase marker file '{}'.", _marker);
        }
    }

    /**
     * Installs the {@code getLocalNew} version of this resource to {@code getLocal}.
     *
     * @param validate whether or not to mark the resource as valid after installing.
     */
    public void install(boolean validate) throws IOException {
        File source = getLocalNew(), dest = getLocal();
        LOGGER.info("- {}", source);
        if (!FileUtil.renameTo(source, dest)) {
            throw new IOException("Failed to rename " + source + " to " + dest);
        }
        applyAttrs();
        if (validate) {
            markAsValid();
        }
    }

    /**
     * Unpacks this resource file into the directory that contains it.
     */
    public void unpack() throws IOException {
        // sanity check
        if (!_isZip) {
            throw new IOException("Requested to unpack non-jar file '" + _local + "'.");
        }
        try (ZipFile jar = new ZipFile(_local)) {
            FileUtil.unpackJar(jar, _unpacked, _attrs.contains(Attr.CLEAN));
        }
    }

    /**
     * Applies this resources special attributes: unpacks this resource if needed, marks it as
     * executable if needed.
     */
    public void applyAttrs() throws IOException {
        if (shouldUnpack()) {
            unpack();
        }
        if (_attrs.contains(Attr.EXEC)) {
            FileUtil.makeExecutable(_local);
        }
    }

    /**
     * Wipes this resource file along with any "validated" marker file that may be associated with
     * it.
     */
    public void erase() {
        clearMarker();
        if (_local.exists() && !FileUtil.deleteHarder(_local)) {
            LOGGER.warn("Failed to erase resource '{}'.", _local);
        }
    }

    @Override
    public int compareTo(Resource other) {
        return _path.compareTo(other._path);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Resource) {
            return _path.equals(((Resource) other)._path);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return _path.hashCode();
    }

    @Override
    public String toString() {
        return _path;
    }

    /**
     * Helper function to simplify the process of reporting progress.
     */
    protected static void updateProgress(ProgressObserver obs, long pos, long total) {
        if (obs != null) {
            obs.progress((int) (100 * pos / total));
        }
    }

    protected final String _path;
    protected final URL _remote;
    protected final File _local;
    protected final File _localNew;
    protected final File _marker;
    protected File _unpacked;
    protected final EnumSet<Attr> _attrs;
    protected final boolean _isZip;

    /**
     * Used to sort the entries in a jar file.
     */
    protected static final Comparator<ZipEntry> ENTRY_COMP = Comparator.comparing(ZipEntry::getName);

    protected static final int DIGEST_BUFFER_SIZE = 5 * 1025;
}
