/*******************************************************************************
 * Copyright (c) 2008 Symbian Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrew Ferguson (Symbian) - Initial implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.index.composite.cpp;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateNonTypeParameter;
import org.eclipse.cdt.internal.core.index.composite.ICompositesFactory;

public class CompositeCPPTemplateNonTypeParameter extends CompositeCPPVariable implements ICPPTemplateNonTypeParameter {
	public CompositeCPPTemplateNonTypeParameter(ICompositesFactory cf, ICPPTemplateNonTypeParameter binding) {
		super(cf, binding);
	}

	public boolean isSameType(IType type) {
		return ((IType)rbinding).isSameType(type);
	}
	
	@Override
	public Object clone() {
		fail(); return null; 
	}

	public IASTExpression getDefault() {
		return null;
	}
}
