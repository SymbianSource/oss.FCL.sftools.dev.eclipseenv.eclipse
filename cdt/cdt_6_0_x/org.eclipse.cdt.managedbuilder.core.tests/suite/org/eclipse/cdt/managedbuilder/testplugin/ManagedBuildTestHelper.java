/*******************************************************************************
 * Copyright (c) 2004, 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.testplugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ICDescriptor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IAdditionalInput;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

public class ManagedBuildTestHelper {
	private static final String rcbsToolId = new String("org.eclipse.cdt.managedbuilder.ui.rcbs");	//$NON-NLS-1$
	private static final String rcbsToolName = new String("Resource Custom Build Step");	//$NON-NLS-1$
	private static final String rcbsToolInputTypeId = new String("org.eclipse.cdt.managedbuilder.ui.rcbs.inputtype");	//$NON-NLS-1$
	private static final String rcbsToolInputTypeName = new String("Resource Custom Build Step Input Type");	//$NON-NLS-1$
	private static final String rcbsToolOutputTypeId = new String("org.eclipse.cdt.managedbuilder.ui.rcbs.outputtype");	//$NON-NLS-1$
	private static final String rcbsToolOutputTypeName = new String("Resource Custom Build Step Output Type");	//$NON-NLS-1$
	private static final String PATH_SEPERATOR = ";";	//$NON-NLS-1$

	
	/* (non-Javadoc)
	 * Create a new project named <code>name</code> or return the project in 
	 * the workspace of the same name if it exists.
	 * 
	 * @param name The name of the project to create or retrieve.
	 * @return 
	 * @throws CoreException
	 */
	static public IProject createProject(
			final String name, 
			final IPath location, 
			final String projectId, 
			final String projectTypeId) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject newProjectHandle = root.getProject(name);
		IProject project = null;
		
		if (!newProjectHandle.exists()) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			if (projectId.equals(ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID)) {
				createNewManagedProject(newProjectHandle, name, location, projectId, projectTypeId);
				project = newProjectHandle;
			} else {
				IWorkspaceDescription workspaceDesc = workspace.getDescription();
				workspaceDesc.setAutoBuilding(false);
				workspace.setDescription(workspaceDesc);
				IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
				//description.setLocation(root.getLocation());
				project = CCorePlugin.getDefault().createCProject(description, newProjectHandle, new NullProgressMonitor(), ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);
			}
		} else {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				}
			};
			NullProgressMonitor monitor = new NullProgressMonitor();
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
			project = newProjectHandle;
		}
        
		// Open the project if we have to
		if (!project.isOpen()) {
			project.open(new NullProgressMonitor());
		}
				
		return project;	
	}
	
	static public IProject createProject(
			final String name, 
			final String projectTypeId) {
		try {
			return 	createProject(name, 
					null, 
					ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID, 
					projectTypeId);
		} catch (CoreException e) {
			TestCase.fail(e.getLocalizedMessage());
		}
		return null;
	}
	
	static public IFile createFile(IProject project, String name){
		return createFile(project, name, new ByteArrayInputStream(new byte[0]));
	}

	static public IFile createFile(IProject project, String name, String contents){
		return createFile(project, name, new ByteArrayInputStream(contents.getBytes()));
	}

	static public IFile createFile(IProject project, String name, InputStream contents){
		IFile file = project.getFile(name);
		if( !file.exists() ){
			try {
				IPath dirPath = file.getFullPath().removeLastSegments(1).removeFirstSegments(1);
				if(dirPath.segmentCount() > 0){
					IFolder rc = project.getFolder(dirPath);
					if(!rc.exists()){
						rc.create(true, true, null);
					}
				}
					
//				file.create( new ByteArrayInputStream( "#include <stdio.h>\n extern void bar(); \n int main() { \nprintf(\"Hello, World!!\"); \n bar();\n return 0; }".getBytes() ), false, null );
				file.create(contents, false, null );
			} catch (CoreException e) {
				TestCase.fail(e.getLocalizedMessage());
			}
		}
		return file;
	}

	static public IFolder createFolder(IProject project, String name){
		IFolder folder = project.getFolder(name);
		if( !folder.exists() ){
			try {
				IPath dirPath = folder.getFullPath().removeLastSegments(1).removeFirstSegments(1);
				if(dirPath.segmentCount() > 0){
					IFolder rc = project.getFolder(dirPath);
					if(!rc.exists()){
						rc.create(true, true, null);
					}
				}
					
//				file.create( new ByteArrayInputStream( "#include <stdio.h>\n extern void bar(); \n int main() { \nprintf(\"Hello, World!!\"); \n bar();\n return 0; }".getBytes() ), false, null );
				folder.create(true , false, null );
			} catch (CoreException e) {
				TestCase.fail(e.getLocalizedMessage());
			}
		}
		return folder;
	}

	/**
	 * Remove the <code>IProject</code> with the name specified in the argument from the 
	 * receiver's workspace.
	 *  
	 * @param name
	 */
	static public void removeProject(String name) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject project = root.getProject(name);
		if (project.exists()) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					System.gc();
					System.runFinalization();
					project.delete(true, true, null);
				}
			};
			NullProgressMonitor monitor = new NullProgressMonitor();
			try {
				workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
			} catch (CoreException e2) {
				Assert.fail("failed to remove the project: " + e2.getLocalizedMessage());
			}
		}
	}
	
	public static IProject loadProject(String name, String path){
		IPath zipPath = new Path("resources").append(path).append(name).append(name).addFileExtension("zip");
		File zipFile = CTestPlugin.getFileInPlugin(zipPath);
		if(zipFile == null){
			zipPath = new Path("resources").append(path).append(name).addFileExtension("zip");
			zipFile = CTestPlugin.getFileInPlugin(zipPath);
		}
		if(zipFile == null) {
			Assert.fail("zip file " + zipPath.toString() + " is missing.");
			return null;
		}

		
		try{
			return createProject(name, zipFile, null, null);
		}
		catch(Exception e){
			Assert.fail("fail to create the project: " + e.getLocalizedMessage());
		}
		
		return null;
	}
	
	static public IProject createProject(String projectName, File zip, IPath location, String projectTypeId) throws CoreException, InvocationTargetException, IOException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (project.exists()) 
			removeProject(projectName);
		
		IPath destPath = (location != null) ?
				location :
				project.getFullPath();
		if (zip != null) {
			importFilesFromZip(new ZipFile(zip), destPath, null);
		}
		
		return createProject(projectName, location, ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID, projectTypeId);
	}
	
	static public void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException {		
		ZipFileStructureProvider structureProvider=	new ZipFileStructureProvider(srcZipFile);
		try {
			ImportOperation op= new ImportOperation(destPath, structureProvider.getRoot(), structureProvider, new IOverwriteQuery() {
						public String queryOverwrite(String file) {
							return ALL;
						}
			});
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
			Assert.assertTrue(false);
		}
	}

	static public IProject createNewManagedProject(IProject newProjectHandle, 
			final String name, 
			final IPath location, 
			final String projectId, 
			final String projectTypeId) throws CoreException {
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		final IProject project = newProjectHandle;
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// Create the base project
				IWorkspaceDescription workspaceDesc = workspace.getDescription();
				workspaceDesc.setAutoBuilding(false);
				workspace.setDescription(workspaceDesc);
				IProjectDescription description = workspace.newProjectDescription(project.getName());
				if (location != null) {
					description.setLocation(location);
				}
				CCorePlugin.getDefault().createCProject(description, project, new NullProgressMonitor(), projectId);
				// Add the managed build nature and builder
				addManagedBuildNature(project);
				
				// Find the base project type definition
				IProjectType[] projTypes = ManagedBuildManager.getDefinedProjectTypes();
				IProjectType projType = ManagedBuildManager.getProjectType(projectTypeId);
				Assert.assertNotNull(projType);
				
				// Create the managed-project (.cdtbuild) for our project that builds an executable.
				IManagedProject newProject = null;
				try {
					newProject = ManagedBuildManager.createManagedProject(project, projType);
				} catch (Exception e) {
					Assert.fail("Failed to create managed project for: " + project.getName());
				}
				Assert.assertEquals(newProject.getName(), projType.getName());
				Assert.assertFalse(newProject.equals(projType));
				ManagedBuildManager.setNewProjectVersion(project);
				// Copy over the configs
				IConfiguration defaultConfig = null;
				IConfiguration[] configs = projType.getConfigurations();
				for (int i = 0; i < configs.length; ++i) {
					// Make the first configuration the default 
					if (i == 0) {
						defaultConfig = newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					} else {
						newProject.createConfiguration(configs[i], projType.getId() + "." + i);
					}
				}
				ManagedBuildManager.setDefaultConfiguration(project, defaultConfig);
				
				IConfiguration cfgs[] = newProject.getConfigurations();
				for(int i = 0; i < cfgs.length; i++){
					cfgs[i].setArtifactName(newProject.getDefaultArtifactName());
				}
				
				ManagedBuildManager.getBuildInfo(project).setValid(true);
			}
		};
		NullProgressMonitor monitor = new NullProgressMonitor();
		try {
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (CoreException e2) {
			Assert.fail(e2.getLocalizedMessage());
		}

		// Initialize the path entry container
		IStatus initResult = ManagedBuildManager.initBuildInfoContainer(project);
		if (initResult.getCode() != IStatus.OK) {
			Assert.fail("Initializing build information failed for: " + project.getName() + " because: " + initResult.getMessage());
		}
		return project;
	}

	static public void addManagedBuildNature (IProject project) {
		// Create the buildinformation object for the project
		IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
//		info.setValid(true);
		
		// Add the managed build nature
		try {
			ManagedCProjectNature.addManagedNature(project, new NullProgressMonitor());
			ManagedCProjectNature.addManagedBuilder(project, new NullProgressMonitor());
		} catch (CoreException e) {
			Assert.fail("Test failed on adding managed build nature or builder: " + e.getLocalizedMessage());
		}

		// Associate the project with the managed builder so the clients can get proper information
		ICDescriptor desc = null;
		try {
			desc = CCorePlugin.getDefault().getCProjectDescription(project, true);
			desc.remove(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID);
			desc.create(CCorePlugin.BUILD_SCANNER_INFO_UNIQ_ID, ManagedBuildManager.INTERFACE_IDENTITY);
		} catch (CoreException e) {
			Assert.fail("Test failed on adding managed builder as scanner info provider: " + e.getLocalizedMessage());
		}
		try {
			desc.saveProjectData();
		} catch (CoreException e) {
			Assert.fail("Test failed on saving the ICDescriptor data: " + e.getLocalizedMessage());		}
	}
	
	static public boolean compareBenchmarks(final IProject project, IPath testDir, IPath[] files) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		};
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			workspace.run(runnable, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Assert.fail("File " + files[0].lastSegment() + " - project refresh failed.");
		}
		for (int i=0; i<files.length; i++) {
			IPath testFile = testDir.append(files[i]);
			IPath benchmarkFile = Path.fromOSString("Benchmarks/" + files[i]);
			StringBuffer testBuffer = readContentsStripLineEnds(project, testFile);
			StringBuffer benchmarkBuffer = readContentsStripLineEnds(project, benchmarkFile);
			if (!testBuffer.toString().equals(benchmarkBuffer.toString())) {
				StringBuffer buffer = new StringBuffer();
				buffer.append("File ").append(testFile.lastSegment()).append(" does not match its benchmark.\n ");
				buffer.append("expected:\n ");
				buffer.append("\"").append(benchmarkBuffer).append("\"");
				buffer.append("\n\n ");
				buffer.append("but was:\n ");
				buffer.append("\"").append(testBuffer).append("\"");
				buffer.append("\n\n ");
				
				buffer.append(">>>>>>>>>>>>>>>start diff: \n");
				String location1 = getFileLocation(project, benchmarkFile);
				String location2 = getFileLocation(project, testFile);
				String diff = DiffUtil.getInstance().diff(location1, location2);
				if(diff == null)
					diff = "!diff failed!";
				buffer.append(diff);
				buffer.append("\n<<<<<<<<<<<end diff");
				buffer.append("\n\n ");
				
				Assert.fail(buffer.toString());
			} 
		}
		return true;
	}

	static public boolean compareBenchmarks(final IProject project, IPath testDir, String[] fileNames) {
		return compareBenchmarks(project, testDir, new Path("benchmarks").append(testDir), fileNames);
	}

	static public boolean compareBenchmarks(final IProject project, IPath testDir, IPath benchmarkDir, String[] fileNames) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		};
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			workspace.run(runnable, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Assert.fail("File " + fileNames[0] + " - project refresh failed.");
		}
		
		IFolder testFolder = (IFolder)project.findMember(testDir);
		IFolder bmFolder = (IFolder)project.findMember(benchmarkDir);
		
		return compareBenchmarks(testFolder, bmFolder, fileNames);
	}
	
	static public boolean compareBenchmarks(IFolder testFolder, IFolder bmFolder, String[] fileNames) {
		Assert.assertNotNull(testFolder);
		Assert.assertNotNull(bmFolder);
		
		for (int i=0; i<fileNames.length; i++) {
			IFile tFile = testFolder.getFile(fileNames[i]);
			IFile bmFile = bmFolder.getFile(fileNames[i]);
			if(!tFile.exists() && !bmFile.exists())
				continue;
			
			compareBenchmarks(tFile, bmFile);
		}
		
		return true;
	}
	
	static public boolean compareBenchmarks(IFile tFile, IFile bmFile) {
		StringBuffer testBuffer = readContentsStripLineEnds(tFile);
		StringBuffer benchmarkBuffer = readContentsStripLineEnds(bmFile);
		if (!testBuffer.toString().equals(benchmarkBuffer.toString())) {
			StringBuffer buffer = new StringBuffer();
			buffer.append("File ").append(tFile.getName()).append(" does not match its benchmark.\n ");
			buffer.append("expected:\n ");
			buffer.append("\"").append(benchmarkBuffer).append("\"");
			buffer.append("\n\n ");
			buffer.append("but was:\n ");
			buffer.append("\"").append(testBuffer).append("\"");
			buffer.append("\n\n ");
				
			buffer.append(">>>>>>>>>>>>>>>start diff: \n");
			String location1 = getFileLocation(bmFile.getProject(), bmFile.getProjectRelativePath());
			String location2 = getFileLocation(tFile.getProject(), tFile.getProjectRelativePath());
			String diff = DiffUtil.getInstance().diff(location1, location2);
			if(diff == null)
				diff = "!diff failed!";
			buffer.append(diff);
			buffer.append("\n<<<<<<<<<<<end diff");
			buffer.append("\n\n ");
				
			Assert.fail(buffer.toString());
		} 
		return true;
	}

	static public boolean verifyFilesDoNotExist(final IProject project, IPath testDir, IPath[] files) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			}
		};
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			workspace.run(runnable, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Assert.fail("File " + files[0].lastSegment() + " - project refresh failed.");
		}
		for (int i=0; i<files.length; i++) {
			IPath testFile = testDir.append(files[i]);
			IPath fullPath = project.getLocation().append(testFile);
			try {
				if (fullPath.toFile().exists()) {
					Assert.fail("File " + testFile.lastSegment() + " unexpectedly found.");
					return false;
				}					
			} catch (Exception e) {
				Assert.fail("File " + fullPath.toString() + " could not be referenced.");
			}
		}
		return true;
	}

	static public String getFileLocation(IProject project, IPath path){
		return project.getLocation().append(path).toString();
	}
	static public StringBuffer readContentsStripLineEnds(IFile file) {
		return readContentsStripLineEnds(file.getProject(), file.getProjectRelativePath());
	}
	static public StringBuffer readContentsStripLineEnds(IProject project, IPath path) {
		StringBuffer buff = new StringBuffer();
		IPath fullPath = project.getLocation().append(path);
		try {
			FileReader input = null;
			try {
				input = new FileReader(fullPath.toFile());
			} catch (Exception e) {
				Assert.fail("File " + fullPath.toString() + " could not be read: " + e.getLocalizedMessage());
			}
			//InputStream input = file.getContents(true);   // A different way to read the file...
			int c;
			do {
				c = input.read();
				if (c == -1) break;
				if (c != '\r' && c != '\n') {
					buff.append((char)c);
				}
			} while (c != -1);
			input.close();
		} catch (Exception e) {
			Assert.fail("File " + fullPath.toString() + " could not be read.");
		}
		return buff;
	}
	
	static public IPath copyFilesToTempDir(IPath srcDir, IPath tmpSubDir, IPath[] files) {
		IPath tmpSrcDir = null;
		String userDirStr = System.getProperty("user.home");
		if (userDirStr != null) {
			IPath userDir = Path.fromOSString(userDirStr);
			tmpSrcDir = userDir.append(tmpSubDir);
			if (userDir.toString().equalsIgnoreCase(tmpSrcDir.toString())) {
				Assert.fail("Temporary sub-directory cannot be the empty string.");				
			} else {
				File tmpSrcDirFile = tmpSrcDir.toFile();
				if (tmpSrcDirFile.exists()) {
					//  Make sure that this is the expected directory before we delete it...
					if (tmpSrcDir.lastSegment().equals(tmpSubDir.lastSegment())) {
						deleteDirectory(tmpSrcDirFile);
					} else {
						Assert.fail("Temporary directory " + tmpSrcDirFile.toString() + " already exists.");
					}
				}
				boolean succeed = tmpSrcDirFile.mkdir();
				if (succeed) {
					for (int i=0; i<files.length; i++) {
						IPath file = files[i];
						IPath srcFile = srcDir.append(file);
						FileReader srcReader = null;
						try {
							srcReader = new FileReader(srcFile.toFile());
						} catch (Exception e) {
							Assert.fail("File " + file.toString() + " could not be read.");
						}
						if (file.segmentCount() > 1) {
							IPath newDir = tmpSrcDir;
							do {
								IPath dir = file.uptoSegment(1);
								newDir = newDir.append(dir);
								file = file.removeFirstSegments(1);
								succeed = newDir.toFile().mkdir();
							} while (file.segmentCount() > 1);
						}
						IPath destFile = tmpSrcDir.append(files[i]);
						FileWriter writer = null;
						try {
							writer = new FileWriter(destFile.toFile());
						} catch (Exception e) {
							Assert.fail("File " + files[i].toString() + " could not be written.");
						}
						try {
							int c;
							do {
								c = srcReader.read();
								if (c == -1) break;
								writer.write(c);
							} while (c != -1);
							srcReader.close();
							writer.close();
						} catch (Exception e) {
							Assert.fail("File " + file.toString() + " could not be copied.");
						}
					}
				}
			}
		}
		return tmpSrcDir;
	}
	
	static public void deleteTempDir(IPath tmpSubDir, IPath[] files) {
		IPath tmpSrcDir = null;
		String userDirStr = System.getProperty("user.home");
		if (userDirStr != null) {
			IPath userDir = Path.fromOSString(userDirStr);
			tmpSrcDir = userDir.append(tmpSubDir);
			if (userDir.toString().equalsIgnoreCase(tmpSrcDir.toString())) {
				Assert.fail("Temporary sub-directory cannot be the empty string.");				
			} else {
				File tmpSrcDirFile = tmpSrcDir.toFile();
				if (!tmpSrcDirFile.exists()) {
					Assert.fail("Temporary directory " + tmpSrcDirFile.toString() + " does not exist.");				
				} else {
					boolean succeed;
					for (int i=0; i<files.length; i++) {
						// Delete the file
						IPath thisFile = tmpSrcDir.append(files[i]);
						succeed = thisFile.toFile().delete();
					}
					// Delete the dir
					succeed = tmpSrcDirFile.delete();
				}
			}
		}
	}
	
	/*
	 * Cloned from core CProjectHelper
	 */
	public static void delete(ICProject cproject) {
		try {
			cproject.getProject().delete(true, true, null);
		} catch (CoreException e) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			} finally {
				try {
					System.gc();
					System.runFinalization();
					cproject.getProject().delete(true, true, null);
				} catch (CoreException e2) {
					Assert.fail(getMessage(e2.getStatus()));
				}
			}
		}
	}
	
	/*
	 * Cloned from core CProjectHelper
	 */
	private static String getMessage(IStatus status) {
		StringBuffer message = new StringBuffer("[");
		message.append(status.getMessage());
		if (status.isMultiStatus()) {
			IStatus children[] = status.getChildren();
			for( int i = 0; i < children.length; i++) {
				message.append(getMessage(children[i]));
			}
		}
		message.append("]");
		return message.toString();
	}

	static private void deleteDirectory(File dir) {
		boolean b;
		File[] toDelete = dir.listFiles();
		for (int i=0; i<toDelete.length; i++) {
			File fileToDelete = toDelete[i];
			if (fileToDelete.isDirectory()) {
				deleteDirectory(fileToDelete);
			}
			b = fileToDelete.delete();
		}
		b = dir.delete();
	}
	
	public static ITool createRcbsTool(IConfiguration cfg, String file, String inputs, String outputs, String cmds){
		IProject project = cfg.getOwner().getProject();
		IResource f = project.findMember(file);
		
		Assert.assertTrue("file does not exist", f != null);
		Assert.assertEquals("resource is not a file", f.getType(), IResource.FILE);
		
		return createRcbsTool(cfg, (IFile)f, inputs, outputs, cmds);
	}

	public static ITool createRcbsTool(IConfiguration cfg, IFile file, String inputs, String outputs, String cmds){
		IResourceConfiguration rcCfg = cfg.getResourceConfiguration(file.getFullPath().toString());
		if(rcCfg == null)
			rcCfg = cfg.createResourceConfiguration(file);
		
		Assert.assertTrue("failed to create resource configuration", rcCfg != null);
		
		ITool tool = getRcbsTool(rcCfg, true);
		
		setRcbsInputs(tool, inputs);
		setRcbsOutputs(tool, outputs);
		tool.setToolCommand(cmds);
		tool.setAnnouncement("default test rcbs announcement");
		
		rcCfg.setRcbsApplicability(IResourceConfiguration.KIND_APPLY_RCBS_TOOL_AS_OVERRIDE);
		return tool;
	}
	
	public static ITool setRcbsInputs(ITool tool, String inputs){
		tool.getInputTypes()[0].getAdditionalInputs()[0].setPaths(inputs);
		return tool;
	}

	public static ITool setRcbsOutputs(ITool tool, String outputs){
		tool.getOutputTypes()[0].setOutputNames(outputs);
		return tool;
	}

	public static ITool getRcbsTool(IResourceConfiguration rcConfig, boolean create){
		ITool rcbsTools[] = getRcbsTools(rcConfig);
		ITool rcbsTool = null; 
		if(rcbsTools != null)
			rcbsTool = rcbsTools[0];
		else if (create) {
			rcbsTool = rcConfig.createTool(null,rcbsToolId + "." + ManagedBuildManager.getRandomNumber(),rcbsToolName,false);	//$NON-NLS-1$
			rcbsTool.setCustomBuildStep(true);
			IInputType rcbsToolInputType = rcbsTool.createInputType(null,rcbsToolInputTypeId + "." + ManagedBuildManager.getRandomNumber(),rcbsToolInputTypeName,false);	//$NON-NLS-1$
			IAdditionalInput rcbsToolInputTypeAdditionalInput = rcbsToolInputType.createAdditionalInput(new String());
			rcbsToolInputTypeAdditionalInput.setKind(IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY);
			rcbsTool.createOutputType(null,rcbsToolOutputTypeId + "." + ManagedBuildManager.getRandomNumber(),rcbsToolOutputTypeName,false);	//$NON-NLS-1$
		}
		return rcbsTool;
	}
	
	public static ITool[] getRcbsTools(IResourceConfiguration rcConfig){
		List list = new ArrayList();
		ITool tools[] = rcConfig.getTools();
		for (int i = 0; i < tools.length; i++) {
			ITool tool = tools[i];
			if (tool.getCustomBuildStep() && !tool.isExtensionElement()) {
				list.add(tool);
			}
		}
		if(list.size() != 0)
			return (ITool[])list.toArray(new ITool[list.size()]);
		return null;
	}

	public static boolean setObjs(IConfiguration cfg, String[] objs){
		return setOption(cfg, IOption.OBJECTS, objs);
	}

	public static boolean setLibs(IConfiguration cfg, String[] objs){
		return setOption(cfg, IOption.LIBRARIES, objs);
	}

	public static boolean setOption(IConfiguration cfg, int type, Object value){
		return setOption(cfg.getFilteredTools(), type, value);
	}

	public static boolean setOption(IResourceConfiguration rcCfg, int type, Object value){
		return setOption(rcCfg.getToolsToInvoke()[0], type, value);
	}

	public static boolean setOption(ITool tools[], int type, Object value){
		for(int i = 0; i < tools.length; i++){
			if(setOption(tools[i], type, value))
				return true;
		}
		return false;
	}

	public static IBuildObject[] getOption(IConfiguration cfg, int type){
		return getOption(cfg.getFilteredTools(), type);
	}

	public static IBuildObject[] getOption(IResourceConfiguration rcCfg, int type, Object value){
		return getOption(new ITool[]{rcCfg.getToolsToInvoke()[0]}, type);
	}

	public static IBuildObject[] getOption(ITool tools[], int type){
		for(int i = 0; i < tools.length; i++){
			IOption option = getOption(tools[i], type);
			if(option != null)
				return new IBuildObject[]{tools[i],option};
		}
		return null;
	}

	public static IOption getOption(IHoldsOptions tool, int type){
		IOption opts[] = tool.getOptions();
		
		for(int i = 0; i < opts.length; i++){
			IOption option = opts[i];
			try {
				if(option.getValueType() == type){
					return option;
				}
			} catch (BuildException e) {
			}
		}
		return null;
	}

	public static boolean setOption(ITool tool, int type, Object value){
		IOption option = getOption(tool, type);
		
		if(option == null)
			return false;
		IBuildObject obj = tool.getParent();
		IConfiguration cfg = null;
		IResourceConfiguration rcCfg = null;
		if(obj instanceof IToolChain)
			cfg = ((IToolChain)obj).getParent();
		else
			rcCfg = (IResourceConfiguration)obj;

		try {
				if(option.getValueType() == type){
					switch(type){
						case IOption.BOOLEAN:
						{
							boolean val = ((Boolean)value).booleanValue();
							if(rcCfg != null)
								rcCfg.setOption(tool, option, val);
							else
								cfg.setOption(tool, option, val);
						}
							return true;
						case IOption.ENUMERATED:
						case IOption.STRING:
						{
							String val = (String)value;
							if(rcCfg != null)
								rcCfg.setOption(tool, option, val);
							else
								cfg.setOption(tool, option, val);
						}
							return true;
						case IOption.STRING_LIST:
						case IOption.INCLUDE_PATH:
						case IOption.PREPROCESSOR_SYMBOLS:
						case IOption.LIBRARIES:
						case IOption.OBJECTS:
						case IOption.INCLUDE_FILES:
						case IOption.LIBRARY_PATHS:
						case IOption.LIBRARY_FILES:
						case IOption.MACRO_FILES:
						{
							String val[] = (String[])value;
							if(rcCfg != null)
								rcCfg.setOption(tool, option, val);
							else
								cfg.setOption(tool, option, val);
						}
							return true;
						default:
							Assert.fail("wrong option type passed");
					}
				}
			} catch (BuildException e) {
			}
		return false;
	}
	
}
