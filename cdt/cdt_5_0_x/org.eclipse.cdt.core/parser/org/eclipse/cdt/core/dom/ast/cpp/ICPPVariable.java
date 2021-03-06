/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/

/*
 * Created on Mar 15, 2005
 */
package org.eclipse.cdt.core.dom.ast.cpp;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IVariable;

/**
 * @author aniefer
 */
public interface ICPPVariable extends IVariable, ICPPBinding {
	
    /**
     * does this variable have the mutable storage class specifier
     * @throws DOMException
     */
    public boolean isMutable() throws DOMException;

    /**
     * Returns whether this variable is declared as extern "C".
     */
    public boolean isExternC() throws DOMException;
}
