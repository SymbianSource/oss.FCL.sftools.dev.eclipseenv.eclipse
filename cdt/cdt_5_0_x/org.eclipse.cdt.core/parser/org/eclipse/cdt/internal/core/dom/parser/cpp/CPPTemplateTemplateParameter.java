/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassTemplatePartialSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPField;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPSpecialization;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateTemplateParameter;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.parser.util.ObjectMap;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPTemplates;
import org.eclipse.cdt.internal.core.dom.parser.cpp.semantics.CPPVisitor;

/**
 * @author aniefer
 */
public class CPPTemplateTemplateParameter extends CPPTemplateParameter implements
		ICPPTemplateTemplateParameter, ICPPClassType, ICPPInternalTemplate, ICPPUnknownBinding {

	private ICPPTemplateParameter[] templateParameters;
	private ObjectMap instances;
	private ICPPScope unknownScope;
	
	/**
	 * @param name
	 */
	public CPPTemplateTemplateParameter(IASTName name) {
		super(name);
	}

	public ICPPScope getUnknownScope() {
	    if (unknownScope == null) {
	    	IASTName n = null;
	    	IASTNode[] nodes = getDeclarations();
	    	if (nodes != null && nodes.length > 0)
	    		n = (IASTName) nodes[0];
	        unknownScope = new CPPUnknownScope(this, n);
	    }
	    return unknownScope;
	}
	
	public ICPPTemplateParameter[] getTemplateParameters() {
		if (templateParameters == null) {
			ICPPASTTemplatedTypeTemplateParameter template = (ICPPASTTemplatedTypeTemplateParameter) getPrimaryDeclaration().getParent();
			ICPPASTTemplateParameter[] params = template.getTemplateParameters();
			ICPPTemplateParameter p = null;
			ICPPTemplateParameter[] result = null;
			for (ICPPASTTemplateParameter param : params) {
				p= (ICPPTemplateParameter) CPPTemplates.getTemplateParameterName(param).resolveBinding();
				if (p != null) {
					result = (ICPPTemplateParameter[]) ArrayUtil.append(ICPPTemplateParameter.class, result, p);
				}
			}
			templateParameters = (ICPPTemplateParameter[]) ArrayUtil.trim(ICPPTemplateParameter.class, result);
		}
		return templateParameters;
	}

	public IBinding resolveTemplateParameter(ICPPASTTemplateParameter templateParameter) {
		IASTName name = CPPTemplates.getTemplateParameterName(templateParameter);
		
		IBinding binding = name.getBinding();
		if (binding == null) {
			//create a new binding and set it for the corresponding parameter in all known decls
	    	if (templateParameter instanceof ICPPASTSimpleTypeTemplateParameter)
	    		binding = new CPPTemplateTypeParameter(name);
	    	else if (templateParameter instanceof ICPPASTParameterDeclaration)
	    		binding = new CPPTemplateNonTypeParameter(name);
	    	else 
	    		binding = new CPPTemplateTemplateParameter(name);
	    	name.setBinding(binding);
		}
		return binding;
	}

	public ICPPClassTemplatePartialSpecialization[] getTemplateSpecializations() throws DOMException {
		return ICPPClassTemplatePartialSpecialization.EMPTY_PARTIAL_SPECIALIZATION_ARRAY;
	}

	public IType getDefault() throws DOMException {
		IASTNode[] nds = getDeclarations();
		if (nds == null || nds.length == 0)
		    return null;
		IASTName name = (IASTName) nds[0];
		ICPPASTTemplatedTypeTemplateParameter param = (ICPPASTTemplatedTypeTemplateParameter) name.getParent();
		IASTExpression defaultValue = param.getDefaultValue();
		if (defaultValue != null)
		    return CPPVisitor.createType(defaultValue);
		return null;
	}

	public ICPPBase[] getBases() {
		return ICPPBase.EMPTY_BASE_ARRAY;
	}

	public IField[] getFields() throws DOMException {
		return null;
	}

	public IField findField(String name) throws DOMException {
		return null;
	}

	public ICPPField[] getDeclaredFields() throws DOMException {
		return null;
	}

	public ICPPMethod[] getMethods() throws DOMException {
		return null;
	}

	public ICPPMethod[] getAllDeclaredMethods() throws DOMException {
		return null;
	}

	public ICPPMethod[] getDeclaredMethods() throws DOMException {
		return null;
	}

	public ICPPConstructor[] getConstructors() {
		return ICPPConstructor.EMPTY_CONSTRUCTOR_ARRAY;
	}

	public IBinding[] getFriends() throws DOMException {
		return null;
	}

	public int getKey() throws DOMException {
		return 0;
	}

	public IScope getCompositeScope() throws DOMException {
		return null;
	}

	@Override
	public void addDefinition(IASTNode node) {
	}

	@Override
	public void addDeclaration(IASTNode node) {
	}

    public boolean isSameType(IType type) {
        if (type == this)
            return true;
        if (type instanceof ITypedef)
            return ((ITypedef)type).isSameType(this);
        return false;
    }

	public ICPPClassTemplatePartialSpecialization[] getPartialSpecializations() throws DOMException {
		return ICPPClassTemplatePartialSpecialization.EMPTY_PARTIAL_SPECIALIZATION_ARRAY;
	}

	public IBinding instantiate(IType[] arguments) {
		return deferredInstance(null, arguments);
	}

	public ICPPSpecialization deferredInstance(ObjectMap argMap, IType[] arguments) {
		ICPPSpecialization instance = getInstance(arguments);
		if (instance == null) {
			instance = new CPPDeferredClassInstance(this, argMap, arguments);
			addSpecialization(arguments, instance);
		}
		return instance;
	}

	public ICPPSpecialization getInstance(IType[] arguments) {
		if (instances == null)
			return null;
		
		int found = -1;
		for (int i = 0; i < instances.size(); i++) {
			IType[] args = (IType[]) instances.keyAt(i);
			if (args.length == arguments.length) {
				int j = 0;
				for (; j < args.length; j++) {
					if (!(args[j].isSameType(arguments[j])))
						break;
				}
				if (j == args.length) {
					found = i;
					break;
				}
			}
		}
		if (found != -1) {
			return (ICPPSpecialization) instances.getAt(found);
		}
		return null;
	}
	
	public void addSpecialization(IType[] types, ICPPSpecialization spec) {
		if (instances == null)
			instances = new ObjectMap(2);
		instances.put(types, spec);
	}

	public ICPPClassType[] getNestedClasses() {
		return ICPPClassType.EMPTY_CLASS_ARRAY;
	}
	
	public IBinding resolvePartially(ICPPUnknownBinding parentBinding, ObjectMap argMap, ICPPScope instantiationScope) {
		return null;
	}

	public IASTName getUnknownName() {
		return new CPPASTName(getNameCharArray());
	}

	public ICPPUnknownBinding getUnknownContainerBinding() {
		return null;
	}
}
