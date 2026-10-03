package io.kestra.plugin.ansible.runner.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveUtils {

    /**
     * Recursively compresses a source directory into a zip archive.
     */
    public static Path zipDirectory(Path sourceDir, Path targetZipFile) throws IOException {
        if (!Files.exists(sourceDir)) {
            throw new FileNotFoundException("Source directory does not exist: " + sourceDir);
        }

        if (targetZipFile.getParent() != null) {
            Files.createDirectories(targetZipFile.getParent());
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(targetZipFile)))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.equals(targetZipFile)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String relativePath = sourceDir.relativize(file).toString().replace("\\", "/");
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!sourceDir.equals(dir)) {
                        String relativePath = sourceDir.relativize(dir).toString().replace("\\", "/") + "/";
                        zos.putNextEntry(new ZipEntry(relativePath));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return targetZipFile;
    }

    /**
     * Extracts a zip stream into the target directory.
     */
    public static void unzip(InputStream inputStream, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new IOException("Zip slip security exception: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(resolvedPath))) {
                        IOUtils.copy(zis, os);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Extracts a tar.gz / tgz stream into the target directory.
     */
    public static void untarGz(InputStream inputStream, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (GzipCompressorInputStream gzis = new GzipCompressorInputStream(new BufferedInputStream(inputStream));
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextTarEntry()) != null) {
                Path resolvedPath = targetDir.resolve(entry.getName()).normalize();
                if (!resolvedPath.startsWith(targetDir)) {
                    throw new IOException("Tar slip security exception: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(resolvedPath))) {
                        IOUtils.copy(tais, os);
                    }
                }
            }
        }
    }
}
