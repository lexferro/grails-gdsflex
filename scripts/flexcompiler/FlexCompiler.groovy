/*
 GRANITE DATA SERVICES
 Copyright (C) 2007-2008 ADEQUATE SYSTEMS SARL
 This file is part of Granite Data Services.
 Granite Data Services is free software you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation either version 3 of the License, or (at your
 option) any later version.
 Granite Data Services is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 for more details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library if not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.util.ArrayList
import java.util.List
import java.util.ResourceBundle

import org.apache.log4j.Logger

import flex2.tools.oem.Project
import flex2.tools.oem.Application
import flex2.tools.oem.Builder
import flex2.tools.oem.Configuration
import flex2.tools.oem.Library
import flex2.tools.oem.Message
import flex2.tools.oem.VirtualLocalFileSystem
import flex2.tools.oem.VirtualLocalFile
import flex2.tools.oem.PathResolver;


/**
 * Flex compiler main manager
 */
public class FlexCompiler {

    private Logger logger = Logger.getLogger(FlexCompiler.class)
    
    private VirtualLocalFileSystem fs
    private Project project
    private Application application
    
    private String flexSDK
    private String basedir
    private String pluginDir
    private String sourceDir
    private String targetDir
    private String appName

    
    public FlexCompiler(flexSDK, basedir, pluginDir, sourceDir, appName) {
    	println "Initialize Flex project (source: ${sourceDir}, app: ${appName})"
    	
    	fs = new VirtualLocalFileSystem()
    	
    	project = new Project()
    	
    	this.flexSDK = flexSDK
    	this.basedir = basedir
    	this.pluginDir = pluginDir
    	this.sourceDir = sourceDir
    	this.targetDir = "${basedir}/grails-app/views/swf"
    	this.appName = appName
    	
    	File outputDir = new File(targetDir)
    	if (!outputDir.exists())
    		outputDir.mkdirs()
    	
    	File source = new File(sourceDir)
    	def appFile = new File(source, "${appName}.mxml") 
        def componentList = []
        source.eachFileRecurse { file ->
        	if (file.isFile() && (file.name.endsWith(".mxml") || file.name.endsWith(".as")))
        		componentList.add(fs.create(file.canonicalPath, file.text, source, file.lastModified()))
        }
        
        File outputFile = new File(outputDir, appFile.name.replaceAll("\\.mxml", ".swf"))
        
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        
        application = new Application(componentList.toArray() as VirtualLocalFile[])
    	Configuration configuration = application.getDefaultConfiguration()
    	configure(configuration)
    	application.setConfiguration(configuration)
        application.setOutput(outputFile)
        if (savedDataFile.exists()) {
        	println "Load existing compilation data"
        	application.load(new FileInputStream(savedDataFile))
        }
        
        println "Adding flex application to project: " + appFile.name
        project.addBuilder(application)
    }

    
    protected void configure(Configuration configuration) throws FlexCompilerException {
    
        configuration.includeLibraries([new File("${pluginDir}/src/flex/libs/granite-essentials.swc")] as File[])
        configuration.addLibraryPath([new File("${pluginDir}/src/flex/libs/granite.swc")] as File[])
        
        configuration.addSourcePath([new File(sourceDir)] as File[])
        
        configuration.addActionScriptMetadata(["Name", "In", "Inject", "Out", "Observer", "PostConstruct", "Destroy", "Path", "Id", "Version"] as String[])
        
        configuration.setServiceConfiguration(new File("${basedir}/web-app/WEB-INF/flex/services-config.xml"))
        
        configuration.setContextRoot(appName)
    }
    
    
    /**
     * Compile the Flex project 
     */
    public void compile(configFile) throws IOException, FlexCompilerException {
    	long timer = System.currentTimeMillis()
    	
    	project.build(true)
        
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        application.save(new FileOutputStream(savedDataFile))
        
    	println "Full compilation of Flex project in ${(System.currentTimeMillis()-timer)/1000}s"
    }
    
    public void incrementalCompile(file) {
    	long timer = System.currentTimeMillis()
    	
    	fs.update(file.canonicalPath, file.text, file.lastModified())
    	
    	project.build(true)
    	
        File savedDataFile = new File("${basedir}/target/flex-${appName}.dat")
        application.save(new FileOutputStream(savedDataFile))
        
    	println "Incremental compilation of Flex project in ${(System.currentTimeMillis()-timer)/1000}s"
    }
}