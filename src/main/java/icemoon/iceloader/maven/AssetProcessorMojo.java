package icemoon.iceloader.maven;

import icemoon.iceloader.EncryptionContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.crypto.spec.SecretKeySpec;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Process assets (encrypt and index).
 */
@Mojo(name = "process-assets", threadSafe = true)
public class AssetProcessorMojo extends AbstractMojo {

    /**
     * Whether or not to encrypt.
     */
    @Parameter(defaultValue = "true")
    private boolean encrypt;

    /**
     * Whether or not to index.
     */
    @Parameter(defaultValue = "true")
    private boolean index;

    /**
     * A custom class to use for the encryption context.
     */
    @Parameter
    private Class<? extends EncryptionContext> context;

    /**
     * When no custom encryption context class has been set, this is the salt
     * used by default.
     */
    @Parameter
    private String simpleSalt;

    /**
     * When no custom encryption context class has been set, this is the
     * password used by default.
     */
    @Parameter
    private String simplePassword;

    /**
     * A custom 'magic' string, used for identifying encrypted files.
     * 
     */
    @Parameter
    private String magic;

    /**
     * An alternative cipher to use.
     * 
     */
    @Parameter
    private String cipher;

    /**
     * When set, this directory is used instead of the main project source folder.
     */
    @Parameter
    private String source;

    /**
     * The destination directory for encrypted assets / indexes
     * 
     */
    @Parameter(defaultValue = "${project.basedir}/target/enc_assets")
    private String destination;

    /**
     * The project currently being build.
     * 
     * ..@parameter expression="${project}"
     */
    @Component
    private MavenProject mavenProject;

    //

    public void execute() throws MojoExecutionException {
	if (simplePassword != null || simpleSalt != null) {
	    if (simplePassword == null) {
		throw new MojoExecutionException(
			"If you provide simpleSalt, you must also provide simplePassword.");
	    }
	    if (simpleSalt == null) {
		throw new MojoExecutionException(
			"If you provide simplePassword, you must also provide simpleSalt.");
	    }
	    if (context != null) {
		throw new MojoExecutionException(
			"Cannot use a custom encryption context if you provide simplePassword or simpleSalt.");
	    }
	    try {
		final EncryptionContext defContext = EncryptionContext.get();
		EncryptionContext.set(new EncryptionContext() {
		    @Override
		    public SecretKeySpec createKey() throws Exception {
			return createKey(simplePassword, simpleSalt);
		    }

		    @Override
		    public String getMagic() {
			return magic == null ? defContext.getMagic() : magic;
		    }

		    @Override
		    public String getCipher() {
			return cipher == null ? defContext.getCipher() : cipher;
		    }
		});
	    } catch (Exception e) {
		throw new MojoExecutionException(
			"Failed to set custom encryption context.");
	    }

	} else {
	    if (context != null) {
		try {
		    EncryptionContext.set(context.newInstance());
		} catch (Exception e) {
		    throw new MojoExecutionException(
			    "Failed to set custom encryption context.");
		}
	    }
	}

	File destDir = new File(destination);
	File srcDir = new File(source == null ? mavenProject.getBuild().getSourceDirectory() : source);

	// Encrypt all assets
	if (encrypt) {
	    getLog().info(String.format("Encrypting %s to %s", srcDir, destDir));
	    try {
		EncryptDir dir = new EncryptDir(srcDir, destDir, this);
		dir.start();
	    } catch (Exception ex) {
		throw new MojoExecutionException(String.format(
			"Failed to encrypt %s to %s", srcDir, destDir), ex);
	    }
	}

	// Create an index file of the assets (helps the choosers in game locate
	// assets where using
	// reflections is not possible - i.e. when loaded from server
	// on-the-fly)
	if (index) {
	    try {
		File indexFileObject = new File(destDir, "index.dat");
		PrintWriter indexWriter = new PrintWriter(new FileOutputStream(
			indexFileObject), true);
		try {
		    if (!encrypt) {
			getLog().info(
				String.format("Creating index from root %s",
					srcDir));
			index(indexWriter, indexFileObject, srcDir, srcDir);
		    } else {
			getLog().info(
				String.format(
					"Creating index from encrypted root %s",
					destDir));
			index(indexWriter, indexFileObject, destDir, destDir);
		    }
		} finally {
		    indexWriter.close();
		}
	    } catch (Exception ex) {
		throw new MojoExecutionException(String.format(
			"Failed to index %s", destDir), ex);
	    }
	}

    }

    public static void main(String[] args) {
    }

    private void index(PrintWriter indexWriter, File indexFile, File rootDir,
	    File dirToIndex) throws IOException {
	for (File f : dirToIndex.listFiles()) {
	    if (!f.equals(indexFile)) {
		if (f.isFile()) {
		    getLog().info(String.format("Indexing %s", f));
		    String relPath = f.getCanonicalPath().substring(
			    rootDir.getCanonicalPath().length() + 1);
		    long lastMod = f.lastModified();
		    indexWriter.println(relPath + "\t" + lastMod + "\t"
			    + f.length());
		} else if (f.isDirectory()) {
		    index(indexWriter, indexFile, rootDir, f);
		}
	    }
	}
    }

}
