/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 * Markus Schorn (Wind River Systems)
 *******************************************************************************/
/*
 * Created on Apr 20, 2005
 */
package org.eclipse.cdt.core.dom.ast.cpp;

import org.eclipse.cdt.core.dom.ast.IParameter;

/**
 * @author aniefer
 *
 */
public interface ICPPParameter extends IParameter, ICPPVariable {
	
	/**
	 * if there is a default value or not.
	 */
	public boolean hasDefaultValue();
}
