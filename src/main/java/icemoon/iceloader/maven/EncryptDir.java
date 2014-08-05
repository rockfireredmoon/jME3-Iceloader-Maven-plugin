package icemoon.iceloader.maven;

import icemoon.iceloader.tools.AbstractCrypt;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;

public class EncryptDir extends AbstractCrypt {

    private AbstractMojo mojo;

    EncryptDir(File sourceDir, File targetDir, AbstractMojo mojo) throws Exception {
        super(sourceDir, targetDir);
        this.mojo = mojo;
    }

    @Override
    protected void doStream(SecretKeySpec secret, File targetFile, Cipher c, File file) throws Exception {
	mojo.getLog().info(String.format("Encrypting %s", file));
        c.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = c.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        final FileOutputStream out = new FileOutputStream(targetFile);
        DataOutputStream dos = new DataOutputStream(out);
        out.write(header);
        dos.writeLong(file.length());
        out.write(iv.length);
        out.write(iv);
        out.flush();
        CipherOutputStream cos = new CipherOutputStream(out, c);
        try {

            InputStream in = new FileInputStream(file);
            try {
                IOUtils.copy(in, cos);
            } finally {
                in.close();
            }
        } finally {
            cos.flush();
            cos.close();
        }
        targetFile.setLastModified(file.lastModified());
    }
}
