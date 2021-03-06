/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.internal.core.model.ext;

import org.eclipse.cdt.core.dom.IName;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IEnumerator;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.c.ICCompositeTypeScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBlockScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassTemplate;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespace;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateScope;
import org.eclipse.cdt.core.index.IIndexMacro;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;

/**
 * Factory for creating CElement handles. These are a minimal implementation
 * of the ICElement interface and can be used for displaying information about
 * the index.
 * @since 4.0
 */
public class CElementHandleFactory {
	private CElementHandleFactory() {}

	public static ICElementHandle create(ITranslationUnit tu, IBinding binding, boolean isDefinition,
			IRegion region, long timestamp) throws CoreException {
		try {
			return internalCreate(tu, binding, isDefinition, region, timestamp);
		}
		catch (DOMException e) {
			return null;
		}
	}

	public static ICElementHandle create(ITranslationUnit tu, IIndexMacro macro, 
			IRegion region, long timestamp) throws CoreException {
		CElementHandle element= new MacroHandle(tu, macro);
		if (region != null) {
			element.setRangeOfID(region, timestamp);
		}
		return element;
	}

	
	public static ICElementHandle internalCreate(ITranslationUnit tu, IBinding binding, boolean definition,
			IRegion region, long timestamp) throws CoreException, DOMException {	

		ICElement parentElement= create(tu, binding.getScope());
		if (parentElement == null) {
			return null;
		}
		
		CElementHandle element= null;
		if (binding instanceof ICPPMethod) {
			element= definition 
					? new MethodHandle(parentElement, (ICPPMethod) binding)
					: new MethodDeclarationHandle(parentElement, (ICPPMethod) binding);
		}	
		else if (binding instanceof IFunction) {
			element= definition 
					? new FunctionHandle(parentElement, (IFunction) binding)
					: new FunctionDeclarationHandle(parentElement, (IFunction) binding);
		}
		else if (binding instanceof IField) {
			element= new FieldHandle(parentElement, (IField) binding);
		}
		else if (binding instanceof IVariable) {
			if (binding instanceof IParameter) {
				return null;
			}
			element= new VariableHandle(parentElement, (IVariable) binding);
		}
		else if (binding instanceof IEnumeration) {
			element= new EnumerationHandle(parentElement, (IEnumeration) binding);
		}
		else if (binding instanceof IEnumerator) {
			element= new EnumeratorHandle(parentElement, (IEnumerator) binding);
		}
		else if (binding instanceof ICompositeType) {
			if (binding instanceof ICPPClassTemplate) {
				element= new StructureTemplateHandle(parentElement, (ICompositeType) binding);
			}
			else {
				element= new StructureHandle(parentElement, (ICompositeType) binding);
			}
		}
		else if (binding instanceof ICPPNamespace) {
			element= new NamespaceHandle(parentElement, (ICPPNamespace) binding);
		}
		else if (binding instanceof ITypedef) {
			element= new TypedefHandle(parentElement, (ITypedef) binding);
		}
		if (element != null && region != null) {
			element.setRangeOfID(region, timestamp);
		}
		return element;
	}

	private static ICElement create(ITranslationUnit tu, IScope scope) throws DOMException {
		if (scope == null) {
			return tu;
		}
		
		IName scopeName= scope.getScopeName();
		if (scopeName == null) {
			if (scope.getParent() == null) {
				return tu;
			} 
			if (scope instanceof ICPPTemplateScope) {
				return create(tu, scope.getParent());
			}
			return null; // unnamed namespace
		}

		ICElement parentElement= create(tu, scope.getParent());
		if (parentElement == null) {
			return null;
		}

		CElementHandle element= null;
		if (scope instanceof ICPPClassScope) {
			ICPPClassType type= ((ICPPClassScope) scope).getClassType();
			element= new StructureHandle(parentElement, type);
		}
		else if (scope instanceof ICCompositeTypeScope) {
			ICompositeType type= ((ICCompositeTypeScope) scope).getCompositeType();
			element= new StructureHandle(parentElement, type);
		}
		else if (scope instanceof ICPPBlockScope) {
			return null;
		}
		else if (scope instanceof ICPPNamespaceScope) {
			element= new NamespaceHandle(parentElement, new String(scopeName.toCharArray()));
		}		
		return element;
	}
}
