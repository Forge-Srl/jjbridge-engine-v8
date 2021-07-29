package jjbridge.engine.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final String TEMP_DIR_PREFIX = "jjbridge-v8";
    private File loadingDir;
    private AssetLoader assetLoader;

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static void extractJniLibraries(File destinationDirectory) throws IOException
    {
        CodeSource codeSource = NativeLibraryLoader.class.getProtectionDomain().getCodeSource();
        if (codeSource == null)
        {
            throw new IOException("Cannot extract files from jar");
        }

        for (String path : getResourceFileNames(codeSource, "jni"))
        {
            File tempFile = new File(destinationDirectory, path.substring(4));
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

    private static boolean isAndroid()
    {
        return System.getProperty("java.specification.vendor").toLowerCase(Locale.getDefault()).contains("android")
                || System.getProperty("java.vendor").toLowerCase(Locale.getDefault()).contains("android")
                || System.getProperty("java.vm.vendor").toLowerCase(Locale.getDefault()).contains("android")
                || System.getProperty("java.vm.specification.vendor").toLowerCase(Locale.getDefault())
                    .contains("android");
    }

    private static File automaticLoad(final String libName, final String[] forceDependencies)
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

        return isAndroid() ? null : new File(System.getProperty("java.library.path"));
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    private static File manualLoad(final String libName)
    {
        File loadingDir;
        try
        {
            loadingDir = createTempDirectory(TEMP_DIR_PREFIX);
            loadingDir.deleteOnExit();

            extractJniLibraries(loadingDir);
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
                try (Stream<Path> files = Files.walk(Paths.get(loadingDir.getAbsolutePath())))
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

        File library = new File(loadingDir, libraryName);
        try
        {
            String libraryAbsolutePath = library.getAbsolutePath();
            System.load(libraryAbsolutePath);
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

        return loadingDir;
    }

    public NativeLibraryLoader()
    {
    }

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
     * */
    public void loadLibrary(final String libName, final String[] forceDependencies)
    {
        try
        {
            loadingDir = automaticLoad(libName, forceDependencies);
        }
        catch (Throwable t)
        {
            loadingDir = manualLoad(libName);
        }
    }

    /**
     * Gets the path to a resource.
     * <p>You can invoke this method only after {@link NativeLibraryLoader#loadLibrary(String, String[])}.</p>
     * <p>If running on Android you must provide a {@link AssetLoader} by calling
     * {@link NativeLibraryLoader#setAssetLoader(AssetLoader)}</p>
     *
     * @param fileName the name of the file to resolve
     * @return the absolute path to the given resource file name
     * */
    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
    public String getResourcePath(String fileName)
    {
        if (!isAndroid())
        {
            return loadingDir.getAbsolutePath() + File.separator + fileName;
        }

        if (assetLoader == null)
        {
            throw new NullPointerException("On Android you must set an AssetLoader!");
        }

        File tempDir = assetLoader.getTempDir();
        File tempFile = new File(tempDir, fileName);
        try (InputStream assetAsStream = assetLoader.getAssetAsStream(fileName);
             OutputStream outputStream = new FileOutputStream(tempFile))
        {
            /*
             * Can't use Files.copy(InputStream in, Path target, CopyOption... options)
             * because Android sucks :(
             */
            byte[] buf = new byte[8192];
            int n;
            while ((n = assetAsStream.read(buf)) > 0)
            {
                outputStream.write(buf, 0, n);
            }
        }
        catch (IOException e)
        {
            tempFile.delete();
        }
        return tempFile.getAbsolutePath();
    }

    public void setAssetLoader(AssetLoader assetLoader)
    {
        this.assetLoader = assetLoader;
    }

    /**
     * The asset loader is used when running on Android to load the resources files shipped with the library.
     * This is needed due to the way Android manages permissions to access the filesystem.
     * */
    public interface AssetLoader
    {
        /**
         * Provides a temporary directory.
         *
         * @return the temporary directory
         * */
        File getTempDir();

        /**
         * Gets the stream associated to the to the asset file.
         *
         * @param fileName the name of the asset
         * @return the stream of the asset
         */
        InputStream getAssetAsStream(String fileName);
    }
}