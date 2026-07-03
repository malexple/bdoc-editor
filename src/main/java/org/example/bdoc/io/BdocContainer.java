package org.example.bdoc.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Тонкая обёртка над Zip FileSystem Provider из java.nio.
 * Позволяет читать и писать отдельные файлы внутри .bdoc-архива
 * без полной распаковки на диск.
 */
public class BdocContainer implements Closeable {

    private final FileSystem fileSystem;

    private BdocContainer(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public static BdocContainer openForRead(Path bdocFile) throws IOException {
        if (!Files.exists(bdocFile)) {
            throw new NoSuchFileException(bdocFile.toString());
        }
        URI uri = URI.create("jar:" + bdocFile.toUri());
        FileSystem fs = FileSystems.newFileSystem(uri, Map.of());
        return new BdocContainer(fs);
    }

    public static BdocContainer createForWrite(Path bdocFile) throws IOException {
        Files.deleteIfExists(bdocFile);
        if (bdocFile.getParent() != null) {
            Files.createDirectories(bdocFile.getParent());
        }
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        URI uri = URI.create("jar:" + bdocFile.toUri());
        FileSystem fs = FileSystems.newFileSystem(uri, env);
        return new BdocContainer(fs);
    }

    public byte[] readBytes(String pathInArchive) throws IOException {
        Path path = fileSystem.getPath(normalize(pathInArchive));
        if (!Files.exists(path)) {
            throw new NoSuchFileException("Entry not found in .bdoc: " + pathInArchive);
        }
        return Files.readAllBytes(path);
    }

    public void writeBytes(String pathInArchive, byte[] data) throws IOException {
        Path path = fileSystem.getPath(normalize(pathInArchive));
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public boolean exists(String pathInArchive) {
        return Files.exists(fileSystem.getPath(normalize(pathInArchive)));
    }

    private static String normalize(String pathInArchive) {
        return pathInArchive.startsWith("/") ? pathInArchive : "/" + pathInArchive;
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}