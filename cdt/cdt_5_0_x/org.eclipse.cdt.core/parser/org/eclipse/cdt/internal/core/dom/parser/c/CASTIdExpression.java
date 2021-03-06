/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Rational Software - Initial API and implementation
 *     Yuan Zhang / Beth Tibbitts (IBM Research)
 *     Bryan Wilkinson (QNX)
 *     Anton Leherbauer (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompletionContext;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.parser.util.ArrayUtil;

/**
 * @author jcamelon
 */
public class CASTIdExpression extends CASTNode implements IASTIdExpression, IASTCompletionContext {

    private IASTName name;

    
    public CASTIdExpression() {
	}

	public CASTIdExpression(IASTName name) {
		setName(name);
	}

	public IASTName getName() {
        return name;
    }

    public void setName(IASTName name) {
        this.name = name;
        if (name != null) {
			name.setParent(this);
			name.setPropertyInParent(ID_NAME);
		}
    }

    @Override
	public boolean accept( ASTVisitor action ){
        if( action.shouldVisitExpressions ){
		    switch( action.visit( this ) ){
	            case ASTVisitor.PROCESS_ABORT : return false;
	            case ASTVisitor.PROCESS_SKIP  : return true;
	            default : break;
	        }
		}
      
        if( name != null ) if( !name.accept( action ) ) return false;

        if( action.shouldVisitExpressions ){
		    switch( action.leave( this ) ){
	            case ASTVisitor.PROCESS_ABORT : return false;
	            case ASTVisitor.PROCESS_SKIP  : return true;
	            default : break;
	        }
		}
        return true;
    }

	public int getRoleForName(IASTName n) {
		if( n == name )	return r_reference;
		return r_unclear;
	}
	
	public IType getExpressionType() {
		return CVisitor.getExpressionType(this);
	}
	
	public IBinding[] findBindings(IASTName n, boolean isPrefix) {
		IBinding[] bindings = CVisitor.findBindingsForContentAssist(n, isPrefix);

		for (int i = 0; i < bindings.length; i++) {
			if (bindings[i] instanceof IEnumeration || bindings[i] instanceof ICompositeType) {
				bindings[i]= null;
			}
		}
		
		return (IBinding[]) ArrayUtil.removeNulls(IBinding.class, bindings);
	}
}
