package icemoon.iceloader.maven;

import icemoon.iceloader.EncryptionContext;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
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
	@Parameter(defaultValue = "false")
	private boolean encrypt;

	/**
	 * Whether or not to index.
	 */
	@Parameter(defaultValue = "false")
	private boolean index;

	/**
	 * Whether or not to archive.
	 */
	@Parameter(defaultValue = "false")
	private boolean archive;

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
	 * When set, this directory is used instead of the main project source
	 * folder.
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
	 * The destination directory for archived assets / indexes
	 * 
	 */
	@Parameter(defaultValue = "${project.basedir}/target/arc_assets")
	private String archives;

	/**
	 * The project currently being build.
	 * 
	 * ..@parameter expression="${project}"
	 */
	@Component
	private MavenProject mavenProject;

	/**
	 * When set, files will only be encrypted if they have been modified since
	 * the last encryption.
	 */
	@Parameter
	private boolean incremental = true;

	public void execute() throws MojoExecutionException {
		ResourceProcessor rp = new ResourceProcessor(getLog());

		String indexFile = null;

		if (index) {
			rp.setSource(source == null ? mavenProject.getBuild().getOutputDirectory() : source);
			rp.setDestination(mavenProject.getBuild().getOutputDirectory() + File.separator + "index.dat");
			rp.index(false);
			indexFile = rp.getDestination();
			getLog().info(String.format("REMOVEME SOURCE: %s",
					source));
			if (source != null) {
				rp.setDestination(source + File.separator + "index.dat");
				rp.index(false);
			}
		}

		if (encrypt) {
			rp.setSimplePassword(simplePassword);
			rp.setSimpleSalt(simpleSalt);
			rp.setContext(context);
			rp.setIncremental(incremental);
			rp.setMagic(magic);
			rp.setCipher(cipher);
		}

		if (encrypt) {
			rp.setSource(source == null ? mavenProject.getBuild().getSourceDirectory() : source);
			rp.setDestination(destination);
			rp.encrypt();

			if (index) {
				rp.setSource(destination);
				rp.setDestination(destination + File.separator + "index.dat");
				rp.setUnprocessedSource(mavenProject.getBuild().getOutputDirectory());
				getLog().info(String.format("Creating index from encrypted root %s", destination));
				rp.index(false);
				indexFile = rp.getDestination();
			}
		}

		if (archive) {
			rp.setDestination(archives);
			if (encrypt) {
				rp.setSource(destination);
				rp.archive();
			} else {
				rp.setSource(mavenProject.getBuild().getOutputDirectory());
				rp.archive();
			}
			if (index) {
				rp.setDestination(new File(new File(archives), "index.dat").getAbsolutePath());
				rp.setSource(archives);
				try {
					FileUtils.copyFile(new File(indexFile), new File(rp.getDestination()));
				} catch (IOException e) {
					throw new MojoExecutionException("Failed to copy index.", e);
				}
				rp.index(true);
			}
		}

	}

}
