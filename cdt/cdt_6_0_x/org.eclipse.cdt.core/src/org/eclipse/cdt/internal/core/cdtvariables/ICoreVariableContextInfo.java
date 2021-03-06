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
package org.eclipse.cdt.internal.core.cdtvariables;

import org.eclipse.cdt.utils.cdtvariables.IVariableContextInfo;

public interface ICoreVariableContextInfo extends IVariableContextInfo {
//	public final static int CONTEXT_FILE = 1;
//	public final static int CONTEXT_OPTION = 2;
	public final static int CONTEXT_CONFIGURATION = 3;
//	public final static int CONTEXT_PROJECT = 4;
	public final static int CONTEXT_WORKSPACE = 5;
	public final static int CONTEXT_INSTALLATIONS = 6;
	public final static int CONTEXT_ECLIPSEENV = 7;

	/**
	 * returns the context type
	 * 
	 * @return int
	 */
	public int getContextType();

	/**
	 * returns the context data
	 * 
	 * @return Object
	 */
	public Object getContextData();
	

}
