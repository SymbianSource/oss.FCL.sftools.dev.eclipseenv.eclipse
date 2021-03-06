/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.internal.core.dom.rewrite;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/**
 * Factory to create CTextFileChanges. Allows for creating ui-dependent objects in the core plugin.
 * @since 5.0
 */
public interface ICTextFileChangeFactory {

	/**
	 * Creates a text file change for the given file.
	 */
	TextFileChange createCTextFileChange(IFile file);
}
