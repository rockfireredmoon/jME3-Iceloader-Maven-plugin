package icemoon.iceloader.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

import icemoon.iceloader.tools.AbstractProcessor;

public class ArchiveDir extends AbstractProcessor {

	private boolean incremental;
	private Log log;

	ArchiveDir(File sourceDir, File targetDir, Log log, boolean incremental) throws Exception {
		super(sourceDir, targetDir);
		this.incremental = incremental;
		this.log = log;
	}

	@Override
	protected void processDir(File file) throws Exception {
		File[] files = file.listFiles();
		int fileCount = 0;
		int dirCount = 0;
		for (File f : files) {
			if (f.isFile()) {
				fileCount++;
			} else if (f.isDirectory()) {
				dirCount++;
			}
		}

		if (fileCount > 0) {
			// If there are ANY files in this dir, we make an archive
			doProcess(file);
		}

		if (dirCount > 0) {
			// If there are ANY sub-directories, we scan it for more files to
			// archive
			for (File f : files) {
				if (f.isDirectory()) {
					processDir(f);
				}
			}
		}
	}

	@Override
	protected void doProcess(File file) throws Exception {

		if (!file.equals(sourceDir)) {
			log.info(String.format("Archiving %s", file.getAbsolutePath()));
			String relpath = file.getAbsolutePath().substring(sourceDir.getAbsolutePath().length() + 1);

			File destFile = new File(targetDir, relpath + ".jar");
			File destDir = destFile.getParentFile();

			if (!destDir.exists() && !destDir.mkdirs())
				throw new IOException(String.format("Failed to create directory %s", destDir));

			long lastMod = destFile.exists() ? destFile.lastModified() / 60000 : -1;
			long thisMod = file.lastModified() / 60000;
			if (incremental && lastMod == thisMod) {
				return;
			}

			File[] files = file.listFiles();
			Manifest m = new Manifest();
			m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			for (File f : files) {
				Attributes attrs = new Attributes();
				m.getEntries().put(f.getName(), attrs);
			}
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(destFile), m);
			try {
				JarEntry je = new JarEntry("META-INF/INDEX.LIST");
				jos.putNextEntry(je);
				PrintWriter pw = new PrintWriter(jos, true);
				pw.println("JarIndex-Version: 1.0");
				pw.println();
				pw.println(destFile.getName());
				jos.closeEntry();

				for (File f : files) {
					if (f.isFile()) {
						JarEntry ze = new JarEntry(f.getName());
						ze.setTime(f.lastModified());
						ze.setSize(f.length());
						jos.putNextEntry(ze);
						FileInputStream fin = new FileInputStream(f);
						try {
							IOUtils.copy(fin, jos);
						} finally {
							fin.close();
						}
						jos.closeEntry();
					}
				}
			} finally {
				jos.close();
			}
		}
	}

}
