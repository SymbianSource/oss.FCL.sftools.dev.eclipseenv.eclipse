/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Rational Software - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.model;

/**
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IEnumerator extends ICElement, ISourceManipulation, ISourceReference {

	/**
	 * Returns the enumerator's constant expression if any.
	 * Returns null otherwise.
	 * @return String
	 */
	String getConstantExpression();
}
