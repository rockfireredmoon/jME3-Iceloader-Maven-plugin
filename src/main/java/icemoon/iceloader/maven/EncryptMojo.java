package icemoon.iceloader.maven;

import icemoon.iceloader.EncryptionContext;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Process assets (encrypt and index).
 */
@Mojo(name = "encrypt", threadSafe = true)
public class EncryptMojo extends AbstractMojo {

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

	//

	public void execute() throws MojoExecutionException {
		ResourceProcessor rp = new ResourceProcessor(getLog());
		rp.setSimplePassword(simplePassword);
		rp.setSimpleSalt(simpleSalt);
		rp.setContext(context);
		rp.setIncremental(incremental);
		rp.setMagic(magic);
		rp.setCipher(cipher);
		rp.setSource(source == null ? mavenProject.getBuild().getSourceDirectory() : source);
		rp.setDestination(destination);
		rp.encrypt();

	}

}
