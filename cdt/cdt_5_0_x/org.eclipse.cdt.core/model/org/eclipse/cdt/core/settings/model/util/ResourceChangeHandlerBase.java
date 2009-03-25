/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.settings.model.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public abstract class ResourceChangeHandlerBase implements IResourceChangeListener {
	public interface IResourceMoveHandler {
		boolean handleResourceMove(IResource fromRc, IResource toRc);
		
		boolean handleResourceRemove(IResource rc);

		void handleProjectClose(IProject project);
		
		void done();
	}

	private IWorkspaceRoot fRoot = ResourcesPlugin.getWorkspace().getRoot();

	private class DeltaVisitor implements IResourceDeltaVisitor{
//		private IResourceDelta fRootDelta;
		private Map fMoveMap = new HashMap();
		private IResourceMoveHandler fHandler;

		public DeltaVisitor(IResourceMoveHandler handler, IResourceDelta rootDelta){
			fHandler = handler;
//			fRootDelta = rootDelta;
		}
		
		private IResource getResource(IPath path, IResource baseResource){
			switch(baseResource.getType()){
			case IResource.FILE:
				return fRoot.getFile(path);
			case IResource.FOLDER:
				return fRoot.getFolder(path);
			case IResource.PROJECT:
				return fRoot.getProject(path.segment(0));
			case IResource.ROOT:
			default:
				throw new IllegalArgumentException();
			}
		}
		
		private boolean checkInitHandleResourceMove(IResource fromRc, IResource toRc){
			if(fMoveMap.put(fromRc, toRc) == null){
				return fHandler.handleResourceMove(fromRc, toRc);
			}
			
			return true;
		}
		
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource dResource = delta.getResource();
			
			if(dResource.getType() == IResource.PROJECT && !shouldVisit((IProject)dResource))
				return false;
					
			boolean resume = true;
			boolean removed = false;
			
			switch (delta.getKind()) {
				case IResourceDelta.REMOVED :
					removed = true;
				case IResourceDelta.CHANGED :
					int flags = delta.getFlags(); 
					if ((flags & IResourceDelta.MOVED_TO) != 0) {
						IPath path = delta.getMovedToPath();
						if(path != null){
							IResource toRc = getResource(path, dResource);
							resume = checkInitHandleResourceMove(dResource, toRc);
						}
						break;
					} else if((flags & IResourceDelta.MOVED_FROM) != 0){
						IPath path = delta.getMovedFromPath();
						if(path != null){
							IResource fromRc = getResource(path, dResource);
							resume = checkInitHandleResourceMove(fromRc, dResource);
						}
						break;
					} else if (removed){
						resume = fHandler.handleResourceRemove(dResource);
					}
				default:
					break;
				}

			
			return resume;	//  visit the children
		}
	}
		
	/*
	 *  I R e s o u r c e C h a n g e L i s t e n e r 
	 */

	/* (non-Javadoc)
	 * 
	 *  Handle the renaming and deletion of project resources
	 *  This is necessary in order to update ResourceConfigurations and AdditionalInputs
	 *  
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		if (event.getSource() instanceof IWorkspace) {
			IResourceMoveHandler handler = createResourceMoveHandler(event);
			doHahdleResourceMove(event, handler);
		}
	}
	
	protected boolean shouldVisit(IProject project){
		try {
			return project.isOpen() ? project.hasNature(CProjectNature.C_NATURE_ID) : true;
		} catch (CoreException e) {
			CCorePlugin.log(e);
			return false;
		}
	}
	
	protected void doHahdleResourceMove(IResourceChangeEvent event, IResourceMoveHandler handler){
		switch (event.getType()) {
			case IResourceChangeEvent.PRE_CLOSE:
				IProject project = (IProject)event.getResource();
				if(shouldVisit(project))
					handler.handleProjectClose(project);
				break;
//				case IResourceChangeEvent.PRE_DELETE :
//					handler.handleResourceRemove(event.getResource());
//					break;
			case IResourceChangeEvent.POST_CHANGE :
				IResourceDelta resDelta = event.getDelta();
				if (resDelta == null) {
					break;
				}
				try {
					DeltaVisitor rcChecker = new DeltaVisitor(handler, resDelta);
					resDelta.accept(rcChecker);
				} catch (CoreException e) {
					CCorePlugin.log(e);
				}
				break;
			default :
				break;
		}
			
		handler.done();
	}
	
	protected abstract IResourceMoveHandler createResourceMoveHandler(IResourceChangeEvent event);
}
