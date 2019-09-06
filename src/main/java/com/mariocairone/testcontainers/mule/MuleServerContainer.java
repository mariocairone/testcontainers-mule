package com.mariocairone.testcontainers.mule;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.filefilter.WildcardFileFilter;
import org.testcontainers.utility.MountableFile;


public class MuleServerContainer extends GenericContainer<MuleServerContainer> {
	
	private static final String MULE_APPS_FOLDER = "/opt/mule/apps/";
	private static final String MULE_CONF_FOLDER = "/opt/mule/conf/";
	private static final String MULE_LOGS_FOLDER = "/opt/mule/logs/";
	private static final String MULE_WRAPPER_CONF_FILE_NAME = "wrapper.conf";
	
	private static final String MULE_ARGS_VAR_NAME = "MULE_ARGS";
	private static final String MULE_ARGS_PREFIX = "-M-D";
	
	
	private static final String DEFAULT_IMAGE_NAME = "mariocairone/mule-ee";
	private static final String DEFAULT_TAG = "latest";  
	 
	private List<Path> deployedApplications; 
	private StringBuilder muleArgs;
    
 
	public MuleServerContainer() {
	    this(DEFAULT_IMAGE_NAME + ":" + DEFAULT_TAG);
	}
	
	public MuleServerContainer(String image) {
	    super(image);
	    muleArgs = new StringBuilder();
	    this.deployedApplications = new ArrayList<>();
	}
	
	@Override
	protected void configure() {	
		super.configure(); 	
		withEnv(MULE_ARGS_VAR_NAME,muleArgs.toString());
		copyApplicationsToMuleAppsFolder();
		
	}

	
	/**
	 * Add mule applications to the container. The application files will be copied in the mule apps folder
	 * @param folder the root folder to scan  
	 * @param pattern the application pattern to search
	 * @return
	 * 
	 */
	public MuleServerContainer withDeployedApplications (
	        final String folder,final String pattern)  {   
	
		List<Path> applications = findApplications(folder,pattern); 
		this.deployedApplications.addAll(applications);    	
		return this;
	} 
	
	/**
	 * Add wrapper.conf file to the Mule Container    
	 * @param wrapper the host path of the wrapper.conf file
	 * @return
	 * this
	 */
	public MuleServerContainer withWrapperConfig(
	        final String wrapper) {    
		
		MountableFile wrapperConf = MountableFile.forHostPath(Paths.get(wrapper));
		return withCopyFileToContainer(wrapperConf, MULE_CONF_FOLDER + MULE_WRAPPER_CONF_FILE_NAME);
	}  
	
	/**
	* Add mule arguments from a Map to the container start command
	* @param muleArgs mule arguments as java.util.Map
	* @return
	* this
	*/
	public MuleServerContainer withMuleArgs(
	        final Map<String,String> muleArgs) {      	
		
	    	muleArgs.forEach((k,v) -> {
	    		withMuleArg(k,v);
	    	});
		
		return this;
	}    
	/**
	* Add mule arguments to the container start command
	* @param argName name of the argument
	* @param argValue value of the argument
	* @return
	* this
	*/   
	public MuleServerContainer withMuleArg(
	        final String argName,final String argValue ) {       	 	 
		 String arg = String.format("%s%s=%s ", MULE_ARGS_PREFIX,argName,argValue);
		 muleArgs.append(arg);
		return this;
	}
	/**
	 * Mount the mule log folder into the specified host path
	 * @param folder host folder to mound
	 * @return
	 * this
	 */
	public MuleServerContainer withMuleLogFolder(String folder) {	
	    String path = MountableFile.forHostPath(folder).getResolvedPath();
		return super.withFileSystemBind(path, MULE_LOGS_FOLDER);
	}
	
	
	private void copyApplicationsToMuleAppsFolder() {
		
		deployedApplications
			.stream()
				.filter(app -> app.toFile().exists())
					.forEach(app -> {									
						MountableFile appMountableFile = MountableFile.forHostPath(app);
						withCopyFileToContainer(appMountableFile, MULE_APPS_FOLDER + app.getFileName());							
					});
											
	}
	
	private List<Path> findApplications(String folder,String filePattern) {
		
		File dir = new File(folder);
		FileFilter fileFilter = new WildcardFileFilter(filePattern);
		File[] files = dir.listFiles(fileFilter);
		
		return Arrays.stream(files).map(file -> file.toPath()).collect(Collectors.toList());
	}
	

	
}
