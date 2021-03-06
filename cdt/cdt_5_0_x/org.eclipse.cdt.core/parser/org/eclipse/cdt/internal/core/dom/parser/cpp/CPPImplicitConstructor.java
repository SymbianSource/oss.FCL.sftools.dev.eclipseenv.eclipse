/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;

/**
 * @author aniefer
 */
public class CPPImplicitConstructor extends CPPImplicitMethod implements ICPPConstructor {

    /**
     * @param name
     * @param params
     */
    public CPPImplicitConstructor( ICPPClassScope scope, char [] name, IParameter[] params ) {
        super( scope, name, createFunctionType(scope, params), params );
    }

	private static IFunctionType createFunctionType(ICPPClassScope scope, IParameter[] params) {
		IType returnType= new CPPBasicType(IBasicType.t_unspecified, 0);
		return CPPVisitor.createImplicitFunctionType(returnType, params, null);
	}

	/* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor#isExplicit()
     */
    public boolean isExplicit() {
        return false;
    }
}
