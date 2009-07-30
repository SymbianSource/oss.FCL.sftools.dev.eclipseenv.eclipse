/*******************************************************************************
 * Copyright (c) 2006, 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Doug Schaefer (QNX) - Initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.pdom.dom.cpp;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.parser.util.CharArrayUtils;
import org.eclipse.cdt.internal.core.Util;
import org.eclipse.cdt.internal.core.dom.parser.ITypeContainer;
import org.eclipse.cdt.internal.core.index.CPPTypedefClone;
import org.eclipse.cdt.internal.core.index.IIndexCPPBindingConstants;
import org.eclipse.cdt.internal.core.index.IIndexType;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMBinding;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMLinkage;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMNode;
import org.eclipse.core.runtime.CoreException;

/**
 * Typedefs for c++
 */
class PDOMCPPTypedef extends PDOMCPPBinding implements ITypedef, ITypeContainer, IIndexType {

	private static final int TYPE = PDOMBinding.RECORD_SIZE + 0;
	
	@SuppressWarnings("hiding")
	protected static final int RECORD_SIZE = PDOMBinding.RECORD_SIZE + 4;
	
	public PDOMCPPTypedef(PDOMLinkage linkage, PDOMNode parent, ITypedef typedef)	throws CoreException {
		super(linkage, parent, typedef.getNameCharArray());
		try {
			setType(parent.getLinkage(), typedef.getType());
		} catch (DOMException e) {
			throw new CoreException(Util.createStatus(e));
		}
	}

	public PDOMCPPTypedef(PDOMLinkage linkage, long record) {
		super(linkage, record);
	}

	@Override
	public void update(final PDOMLinkage linkage, IBinding newBinding) throws CoreException {
		if (newBinding instanceof ITypedef) {
			ITypedef td= (ITypedef) newBinding;
			IType mytype= getType();
			try {
				IType newType= td.getType();
				setType(linkage, newType);
				if (mytype != null) {
					linkage.deleteType(mytype, record);
				}				
			} catch (DOMException e) {
				throw new CoreException(Util.createStatus(e));
			}
		}
	}

	private void setType(final PDOMLinkage linkage, IType newType) throws CoreException, DOMException {
		PDOMNode typeNode = linkage.addType(this, newType);
		if (introducesRecursion((IType) typeNode, getParentNodeRec(), getNameCharArray())) {
			linkage.deleteType((IType) typeNode, record);
			typeNode= null;
		}
		getDB().putRecPtr(record + TYPE, typeNode != null ? typeNode.getRecord() : 0);
	}

	private boolean introducesRecursion(IType type, long parentRec, char[] tdname) throws DOMException {
		int maxDepth= 50;
		while (--maxDepth > 0) {
			if (type instanceof ITypedef) {
				try {
					if ((!(type instanceof PDOMNode) || // this should not be the case anyhow
							((PDOMNode) type).getParentNodeRec() == parentRec) &&
							CharArrayUtils.equals(((ITypedef) type).getNameCharArray(), tdname)) {
						return true;
					}
				} catch (CoreException e) {
					return true;
				}
			}
			if (type instanceof ITypeContainer) {
				type= ((ITypeContainer) type).getType();
			}
			else if (type instanceof IFunctionType) {
				IFunctionType ft= (IFunctionType) type;
				if (introducesRecursion(ft.getReturnType(), parentRec, tdname)) {
					return true;
				}
				IType[] params= ft.getParameterTypes();
				for (IType param : params) {
					if (introducesRecursion(param, parentRec, tdname)) {
						return true;
					}
				}
				return false;
			}
			else {
				return false;
			}
		}
		return true;
	}

	@Override
	protected int getRecordSize() {
		return RECORD_SIZE;
	}
	
	@Override
	public int getNodeType() {
		return IIndexCPPBindingConstants.CPPTYPEDEF;
	}

	public IType getType() {
		try {
			PDOMNode node = getLinkage().getNode(getDB().getRecPtr(record + TYPE));
			return node instanceof IType ? (IType)node : null;
		} catch (CoreException e) {
			CCorePlugin.log(e);
			return null;
		}
	}

	public boolean isSameType(IType type) {
		IType myrtype = getType();
		if (myrtype == null)
			return false;
		
		if (type instanceof ITypedef) {
			type= ((ITypedef)type).getType();
			if (type == null) {
				return false;
			}
		}
		return myrtype.isSameType(type);
	}
	
	@Override
	protected String toStringBase() {
		return getName() + ": " + super.toStringBase(); //$NON-NLS-1$
	}

	public void setType(IType type) { fail(); }

	@Override
	public Object clone() {
		return new CPPTypedefClone(this);
	}
}