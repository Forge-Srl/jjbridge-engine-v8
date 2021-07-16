package jjbridge.engine.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class to load native libraries.
 * */
public class NativeLibraryLoader
{
    private static File tempDir;

    /**
     * Loads the given native library.
     * <p>You should use this method much like {@link System#loadLibrary(String)}.</p>
     * <p>This method performs additional attempts to load the library handling:</p>
     * <ul>
     *     <li>Whether the code is running from inside or outside a jar file.</li>
     *     <li>Quirks of the operating systems in loading additional native libraries needed for the correct
     *     execution.</li>
     * </ul>
     *
     * @param libName the name of the library to load
     * @param forceDependencies list of dependencies to force-load before loading the library
     * @return the absolute path to the loaded library
     * */
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public static String load(final String libName, final String[] forceDependencies)
    {
        try
        {
            try
            {
                System.loadLibrary(libName);
            }
            catch (Throwable t)
            {
                if (t.getMessage().contains("no " + libName + " in java.library.path"))
                {
                    throw t;
                }

                for (String dependency : forceDependencies)
                {
                    System.loadLibrary(dependency);
                }
                System.loadLibrary(libName);
            }
            return System.getProperty("java.library.path") + File.separator + System.mapLibraryName(libName);
        }
        catch (Throwable t)
        {
            try
            {
                extractJniLibraries();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            String libraryName = System.mapLibraryName(libName);

            // On Windows we need to preload all .dll dependencies
            if (isWindows())
            {
                // Also we must repeat loading twice to resolve possible circular dependencies
                for (int i = 0; i < 2; i++)
                {
                    try (Stream<Path> files = Files.walk(Paths.get(tempDir.getAbsolutePath())))
                    {
                        files.map(Path::toFile)
                                .filter(f -> !f.isDirectory()
                                        && f.getAbsolutePath().endsWith(".dll")
                                        && !f.getAbsolutePath().endsWith(libraryName))
                                .forEach(f ->
                                {
                                    try
                                    {
                                        System.load(f.getAbsolutePath());
                                    }
                                    catch (Throwable e)
                                    {
                                        System.err.println("Caught loading exception:\n\t" + e.getMessage());
                                        // It's ok to fail here; will eventually fail later on final load.
                                    }
                                });
                    }
                    catch (IOException e)
                    {
                        // It's ok to fail here; will eventually fail later on final load.
                    }
                }
            }

            File library = new File(tempDir, libraryName);
            try
            {
                String libraryAbsolutePath = library.getAbsolutePath();
                System.load(libraryAbsolutePath);
                return libraryAbsolutePath;
            }
            catch (Throwable throwable)
            {
                if (isPosixCompliant())
                {
                    // Assume POSIX compliant file system, can be deleted after loading
                    library.delete();
                }
                else
                {
                    // Assume non-POSIX, and don't delete until last file descriptor closed
                    library.deleteOnExit();
                }
                throw new RuntimeException(throwable);
            }
        }
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void extractJniLibraries() throws IOException
    {
        if (tempDir != null)
        {
            return;
        }

        CodeSource codeSource = NativeLibraryLoader.class.getProtectionDomain().getCodeSource();
        if (codeSource == null)
        {
            throw new IOException("Cannot extract files from jar");
        }

        tempDir = createTempDirectory("jjbridge-v8");
        tempDir.deleteOnExit();

        for (String path : getResourceFileNames(codeSource, "jni"))
        {
            File tempFile = new File(tempDir, path.substring(4));
            try (InputStream is = NativeLibraryLoader.class.getResourceAsStream("/" + path))
            {
                Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                tempFile.delete();
                throw e;
            }
            catch (NullPointerException e)
            {
                tempFile.delete();
                throw new FileNotFoundException("File " + path + " was not found inside JAR.");
            }
        }
    }

    private static ArrayList<String> getResourceFileNames(CodeSource codeSource, String path) throws IOException
    {
        InputStream in = codeSource.getLocation().openStream();
        ZipInputStream zipInputStream = new ZipInputStream(in);
        ArrayList<String> jniFiles = new ArrayList<>();

        try
        {
            for (ZipEntry entry = zipInputStream.getNextEntry(); entry != null; entry = zipInputStream.getNextEntry())
            {
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith(path))
                {
                    jniFiles.add(name);
                }
            }
        }
        finally
        {
            zipInputStream.close();
            in.close();
        }

        return jniFiles;
    }

    private static File createTempDirectory(String prefix) throws IOException
    {
        String tempDir = System.getProperty("java.io.tmpdir");
        File generatedDir = new File(tempDir, prefix + System.nanoTime());

        if (!generatedDir.mkdir())
        {
            throw new IOException("Failed to create temp directory " + generatedDir.getName());
        }

        return generatedDir;
    }

    private static boolean isPosixCompliant()
    {
        try
        {
            return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        }
        catch (FileSystemNotFoundException | ProviderNotFoundException | SecurityException e)
        {
            return false;
        }
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("win");
    }
}