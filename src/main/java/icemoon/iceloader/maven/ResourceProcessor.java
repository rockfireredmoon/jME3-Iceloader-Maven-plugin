package icemoon.iceloader.maven;

import icemoon.iceloader.EncryptionContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.crypto.spec.SecretKeySpec;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * Process assets (encrypt and index).
 */
public class ResourceProcessor {

	private Class<? extends EncryptionContext> context;
	private String simpleSalt;
	private String simplePassword;
	private String magic;
	private String cipher;
	private String source;
	private String destination;
	private Log log;
	private String[] excludes;
	private String[] includes;
	private boolean incremental = true;
	private String unprocessedSource;

	//

	public ResourceProcessor(Log log) {
		this.log = log;
	}

	public void encrypt() throws MojoExecutionException {
		if (simplePassword != null || simpleSalt != null) {
			if (simplePassword == null) {
				throw new MojoExecutionException("If you provide simpleSalt, you must also provide simplePassword.");
			}
			if (simpleSalt == null) {
				throw new MojoExecutionException("If you provide simplePassword, you must also provide simpleSalt.");
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
				throw new MojoExecutionException("Failed to set custom encryption context.");
			}

		} else {
			if (context != null) {
				try {
					EncryptionContext.set(context.newInstance());
				} catch (Exception e) {
					throw new MojoExecutionException("Failed to set custom encryption context.");
				}
			}
		}

		File destDir = new File(destination);
		File srcDir = new File(source);

		// Encrypt all assets
		log.info(String.format("Encrypting %s to %s", srcDir, destDir));
		try {
			EncryptDir dir = new EncryptDir(srcDir, destDir, log, incremental);
			dir.start();
		} catch (Exception ex) {
			throw new MojoExecutionException(String.format("Failed to encrypt %s to %s", srcDir, destDir), ex);
		}

	}

	public boolean isIncremental() {
		return incremental;
	}

	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
	}

	public String[] getExclude() {
		return excludes;
	}

	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}

	public String[] getIncludes() {
		return includes;
	}

	public void setIncludes(String[] includes) {
		this.includes = includes;
	}

	public Class<? extends EncryptionContext> getContext() {
		return context;
	}

	public void setContext(Class<? extends EncryptionContext> context) {
		this.context = context;
	}

	public String getSimpleSalt() {
		return simpleSalt;
	}

	public void setSimpleSalt(String simpleSalt) {
		this.simpleSalt = simpleSalt;
	}

	public String getSimplePassword() {
		return simplePassword;
	}

	public void setSimplePassword(String simplePassword) {
		this.simplePassword = simplePassword;
	}

	public String getMagic() {
		return magic;
	}

	public void setMagic(String magic) {
		this.magic = magic;
	}

	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	private void index(File root, Log log, PrintWriter indexWriter, File indexFile, String dirPath) throws IOException {
		File dirToIndex = dirPath == null ? root : new File(root, dirPath);
		for (File f : dirToIndex.listFiles()) {
			String relPath = dirPath == null ? f.getName() : dirPath + "/" + f.getName();
			if (!f.equals(indexFile)) {
				if (f.isFile()) {
					if (matches(relPath)) {
						log.info(String.format("Indexing %s", f));
						long lastMod = f.lastModified();
						// Look for the unprocessed version if availabble						
						String output = relPath + "\t" + lastMod + "\t" + f.length();
						if(unprocessedSource != null) {
							File unprocessed = new File(unprocessedSource + File.separator + relPath);
							if(unprocessed.exists()) {
								output += "\t" + unprocessed.length();
							}
						}
						indexWriter.println(output);
					}
				} else if (f.isDirectory()) {
					index(root, log, indexWriter, indexFile, relPath);
				}
			}
		}
	}

	public void index(boolean append) throws MojoExecutionException {
		File srcDir = new File(source);
		try {
			PrintWriter indexWriter = new PrintWriter(new FileOutputStream(destination, append), true);
			try {
				log.info(String.format("Creating index from root %s", source));
				index(srcDir, log, indexWriter, new File(destination), null);
			} finally {
				indexWriter.close();
			}
		} catch (Exception ex) {
			throw new MojoExecutionException(String.format("Failed to index %s", srcDir), ex);
		}

	}

	private boolean matches(String path) {
		System.err.println("testing " + path);
		if (includes == null && excludes == null) {
			System.err.println(" path matches because no excludes");
			return true;
		}

		// Is it excluded?
		if (excludes != null) {
			System.err.println(" testing exclude");
			for (String p : excludes) {
				if (SelectorUtils.matchPath(p, path)) {
					System.err.println("  doesnt match " + p);
					return false;
				}
			}
		}

		// Not excluded, is it included?
		if (includes != null) {
			System.err.println(" testing include");
			for (String p : includes) {
				if (SelectorUtils.matchPath(p, path)) {
					System.err.println("  match " + p);
					return true;
				}
			}
			System.err.println("  doesnt match at all");
			return false;
		} else {
			System.err.println("  matches because no exclud");
			return true;
		}
	}

	public void archive() throws MojoExecutionException {

		File destDir = new File(destination);
		File srcDir = new File(source);

		log.info(String.format("Archiving %s to %s", srcDir, destDir));
		try {
			ArchiveDir dir = new ArchiveDir(srcDir, destDir, log, incremental);
			dir.start();
		} catch (Exception ex) {
			throw new MojoExecutionException(String.format("Failed to encrypt %s to %s", srcDir, destDir), ex);
		}
	}

	public void setUnprocessedSource(String unprocessedSource) {
		this.unprocessedSource = unprocessedSource;		
	}

}
