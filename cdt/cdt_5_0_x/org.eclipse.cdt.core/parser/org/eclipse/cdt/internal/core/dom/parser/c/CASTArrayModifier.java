/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Rational Software - Initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.IASTAmbiguityParent;

/**
 * @author jcamelon
 */
public class CASTArrayModifier extends CASTNode implements IASTArrayModifier, IASTAmbiguityParent {

    private IASTExpression exp;

    
    public CASTArrayModifier() {
	}

	public CASTArrayModifier(IASTExpression exp) {
		setConstantExpression(exp);
	}

	
    public IASTExpression getConstantExpression() {
        return exp;
    }

    public void setConstantExpression(IASTExpression expression) {
        this.exp = expression;
        if(expression != null) {
        	expression.setParent(this);
        	expression.setPropertyInParent(CONSTANT_EXPRESSION);
        }
    }

    @Override
	public boolean accept( ASTVisitor action ){      
        if( exp != null ) if( !exp.accept( action ) ) return false;
        return true;
    }

    public void replace(IASTNode child, IASTNode other) {
        if( child == exp )
        {
            other.setPropertyInParent( child.getPropertyInParent() );
            other.setParent( child.getParent() );
            exp = (IASTExpression) other;
        }
    }
}
