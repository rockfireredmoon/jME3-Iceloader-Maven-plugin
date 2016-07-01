package icemoon.iceloader.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Create index
 */
@Mojo(name = "index", threadSafe = true)
public class IndexMojo extends AssetProcessorMojo {

    /**
     * When set, this directory is used instead of the main project source
     * folder.
     */
    @Parameter
    private String source;
    
    /**
     * Paths to exclude from indexing.
     */
    @Parameter
    private String[] excludes;

    
    /**
     * Paths to include for indexing.
     */
    @Parameter
    private String[] includes;
    

    /**
     * The project currently being build.
     * 
     * ..@parameter expression="${project}"
     */
    @Component
    private MavenProject mavenProject;

    //

    public void execute() throws MojoExecutionException {
	ResourceProcessor rp = new ResourceProcessor(getLog());
	rp.setSource(source == null ? mavenProject.getBuild()
		.getOutputDirectory() : source);
	rp.setExcludes(excludes);
	rp.setIncludes(includes);
	rp.index();

    }


}
