/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.internal.core.pdom;

import org.eclipse.cdt.core.index.IIndexFileLocation;

/**
 * Abstract class for resolving paths as computed by the parser.
 * @since 5.0
 */
public abstract class ASTFilePathResolver {

	/**
	 * Resolve a path as stored in the AST.
	 * @return an index file location.
	 */
	public abstract IIndexFileLocation resolveASTPath(String astFilePath);

	/**
	 * Resolve a path for an inclusion as computed by the preprocessor. Check for existence
	 * and return <code>null</code> if the file does not exist. 
	 * @return an index file location or <code>null</code> if the file does not exist.
	 */
	public abstract IIndexFileLocation resolveIncludeFile(String includePath);
	
	/**
	 * Check for existence of an inclusion as computed by the preprocessor.
	 */
	public abstract boolean doesIncludeFileExist(String includePath);
	
	/**
	 * Convert an index file location to the path as it will be stored in the AST.
	 */
	public abstract String getASTPath(IIndexFileLocation ifl);
}
