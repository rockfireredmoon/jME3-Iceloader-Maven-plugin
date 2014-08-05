# Iceloader Maven Plugin

A Maven plugin for encrypting and indexing assets for use with the Iceloader plugin.

## Encrypting Your Assets and Creating Indexes

Indexes are used for two things. 

1. To speed up freshness checks when loading assets from a remote server for example using
   EncryptedServerLocator or ServerLocator. The index also contains the last modified
   time so only has to be downloaded once up-front, saving one request per asset.

2. You may have a need to know what assets you have at runtime. I use this for some 
   in game design tools (for creatures and world building). New assets may be uploaded
   by users at any time, so the index is useful to me.

The same tool that is used for indexing is also used to encrypt the assets for upload
to the server that will be supplying them (or used to encrypt classpath resources if
the assets you supply with your game are to be encrypted). 

So, to create indexes and encrypt the assets, you use the provided Maven plugin. Add 
something like the following to your _pom.xml_. This example will produce encrypted
and indexed files during the package phase of your build.

```
<plugin>
	<artifactId>jME3-Iceloader-Maven-plugin</artifactId>
	<groupId>jME3-ext</groupId>
	<version>0.0.1-SNAPSHOT</version>
	<executions>
		<execution>
			<id>process-assets</id>
			<phase>package</phase>
			<goals>
				<goal>process-assets</goal>
			</goals>
			<configuration>
				<source>assets</source>
				<destination>target/enc_assets</destination>
				<index>true</index>
				<encrypt>true</encrypt>			
			</configuration>
		</execution>
	</executions>
</plugin>

```

This will create the directory _target/enc_assets_, you can then upload this entire direwctory
to any HTTP server and use EncryptedServerLocator in your locator list (see below for 
how to configure the location of the server).

## Configuration

The plugin supports the following configuration.

| Name  | Type | Default | Description |
| ----- | ---- | ------- | ----------- |
| encrypt | boolean | true | Determines whether assets will will be encrypted. |
| index | boolean | true | Determines whether assets will will be indexed. |
| source | Path | Option | Source location of assets. Will use project source folder if not specified. |
| destination | Path | Required | Destination location of assets. |
| encryptionContextClassName | Class name | Optional | Fully qualified class name of a custom EncryptionContext (must be on tasks classpath). |
| simplePassword | String | Optional | When default EncryptionContext is in use, the password to use for encryption key. |
| simpleSalt | String | Optional | When default EncryptionContext is in use, the salt to use for encryption key. |
| magic | String | !@ENC/PF_0 | Header used for encrypted files. |
| cipher | String | AES/CFB8/NoPadding | Cipher to use for encrypting files. |
