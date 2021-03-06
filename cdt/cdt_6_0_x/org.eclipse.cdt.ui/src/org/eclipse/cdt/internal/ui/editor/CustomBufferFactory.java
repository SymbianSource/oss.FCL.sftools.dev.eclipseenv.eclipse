/*******************************************************************************
 * Copyright (c) 2002, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * Anton Leherbauer (Wind River Systems)
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.editor;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.cdt.core.model.IBuffer;
import org.eclipse.cdt.core.model.IOpenable;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.model.IWorkingCopy;
import org.eclipse.cdt.ui.CUIPlugin;

import org.eclipse.cdt.internal.core.model.IBufferFactory;

/**
 * CustomBufferFactory
 */
public class CustomBufferFactory implements IBufferFactory {
	/**
	 * 
	 */
	public CustomBufferFactory() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.internal.core.model.IBufferFactory#createBuffer(org.eclipse.cdt.core.model.IOpenable)
	 */
	public IBuffer createBuffer(IOpenable owner) {
		if (owner instanceof IWorkingCopy) {

			IWorkingCopy unit= (IWorkingCopy) owner;
			ITranslationUnit original= unit.getOriginalElement();
			IResource resource= original.getResource();
			if (resource instanceof IFile) {
				IFile fFile = (IFile)resource;
				DocumentAdapter adapter= new DocumentAdapter(owner, fFile);
				return adapter;
			}

			// URI
			URI locationUri= original.getLocationURI();
			if (locationUri != null) {
				try {
					return new DocumentAdapter(owner, locationUri);
				} catch (CoreException exc) {
					CUIPlugin.log(exc);
				}
			}

		}
		return DocumentAdapter.NULL;
	}
}
