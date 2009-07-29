/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Doug Schaefer (IBM) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.dom.ast;

/**
 * This is the break clause in a loop.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IASTBreakStatement extends IASTStatement {

	/**
	 * @since 5.1
	 */
	public IASTBreakStatement copy();
}
