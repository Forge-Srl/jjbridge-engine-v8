package jjbridge.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NativeLibraryLoader
{
    private static File tempDir;

    public static void load(final String libName)
    {
        try
        {
            System.loadLibrary(libName);
        } catch (Throwable t)
        {
            try
            {
                extractJniLibraries();
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            File library = new File(tempDir, System.mapLibraryName(libName));
            try
            {
                System.load(library.getAbsolutePath());
            } finally
            {
                if (isPosixCompliant())
                {
                    // Assume POSIX compliant file system, can be deleted after loading
                    library.delete();
                } else
                {
                    // Assume non-POSIX, and don't delete until last file descriptor closed
                    library.deleteOnExit();
                }
            }
        }
    }

    private static void extractJniLibraries() throws IOException
    {
        if (tempDir != null) return;

        CodeSource codeSource = NativeLibraryLoader.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) throw new IOException("Cannot extract files from jar");

        tempDir = createTempDirectory("jjbridge-v8");
        tempDir.deleteOnExit();

        for (String path : getResourceFileNames(codeSource, "jni"))
        {
            File tempFile = new File(tempDir, path.substring(4));
            try (InputStream is = NativeLibraryLoader.class.getResourceAsStream("/" + path))
            {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e)
            {
                tempFile.delete();
                throw e;
            } catch (NullPointerException e)
            {
                tempFile.delete();
                throw new FileNotFoundException("File " + path + " was not found inside JAR.");
            }
        }
    }

    private static ArrayList<String> getResourceFileNames(CodeSource codeSource, String path) throws IOException
    {
        ZipInputStream zipInputStream = new ZipInputStream(codeSource.getLocation().openStream());
        ArrayList<String> jniFiles = new ArrayList<>();

        for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry())
        {
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(path))
            {
                jniFiles.add(name);
            }
        }
        return jniFiles;
    }

    private static File createTempDirectory(String prefix) throws IOException
    {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir())
            throw new IOException("Failed to create temp directory " + generatedDir.getName());

        return generatedDir;
    }

    private static boolean isPosixCompliant()
    {
        try
        {
            return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        } catch (FileSystemNotFoundException | ProviderNotFoundException | SecurityException e)
        {
            return false;
        }
    }
}