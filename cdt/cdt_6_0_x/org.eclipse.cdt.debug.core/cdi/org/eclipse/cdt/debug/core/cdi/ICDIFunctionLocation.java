/*******************************************************************************
 * Copyright (c) 2000, 2005 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.debug.core.cdi;

/**
 * 
 * Represents a file:function location in the debuggable program.
 * 
 */
public interface ICDIFunctionLocation extends ICDIFileLocation {

	/**
	 * Returns the function of this location or <code>null</code>
	 * if the function is unknown.
	 *  
	 * @return the function of this location
	 */
	String getFunction();

}
