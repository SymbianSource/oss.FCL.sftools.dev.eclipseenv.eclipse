/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 * Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.core.dom.ast;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Represents the semantics of a name found in the AST or the index.
 * @author Doug Schaefer
 */
public interface IBinding extends IAdaptable {
    public static final IBinding[] EMPTY_BINDING_ARRAY = new IBinding[0];
	/**
	 * The name of the binding.
	 * 
	 * @return name
	 */
	public String getName();
    
    /**
     * The name of the binding.
     * 
     * @return name
     */
	public char[] getNameCharArray();
	
	/**
	 * Every name has a scope.
	 * 
	 * @return the scope of this name
	 */
	public IScope getScope() throws DOMException;
	
	/**
	 * Every binding has a linkage.
	 */
	public ILinkage getLinkage() throws CoreException;

}
