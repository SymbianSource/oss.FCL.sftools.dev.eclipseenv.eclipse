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
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.parser.util.ArrayUtil;

/**
 * @author jcamelon
 */
public class CASTFunctionDeclarator extends CASTDeclarator implements IASTStandardFunctionDeclarator {

    private IASTParameterDeclaration [] parameters = null;
    private int parametersPos=-1;
    private boolean varArgs;
    
    public CASTFunctionDeclarator() {
	}

	public CASTFunctionDeclarator(IASTName name) {
		super(name);
	}

	public IASTParameterDeclaration[] getParameters() {
        if( parameters == null ) return IASTParameterDeclaration.EMPTY_PARAMETERDECLARATION_ARRAY;
        parameters = (IASTParameterDeclaration[]) ArrayUtil.removeNullsAfter( IASTParameterDeclaration.class, parameters, parametersPos );
        return parameters;
    }

    public void addParameterDeclaration(IASTParameterDeclaration parameter) {
    	if (parameter != null) {
    		parameter.setParent(this);
			parameter.setPropertyInParent(FUNCTION_PARAMETER);
    		parameters = (IASTParameterDeclaration[]) ArrayUtil.append( IASTParameterDeclaration.class, parameters, ++parametersPos, parameter );
    	}        
    }

    public boolean takesVarArgs() {
        return varArgs;
    }

    public void setVarArgs(boolean value) {
        varArgs = value;
    }

    @Override
	protected boolean postAccept( ASTVisitor action ){
        IASTParameterDeclaration [] params = getParameters();
        for ( int i = 0; i < params.length; i++ ) {
            if( !params[i].accept( action ) ) return false;
        }
        return true;
    }

	@Override
	public void replace(IASTNode child, IASTNode other) {
        if( parameters != null ) {
        	for (int i = 0; i < parameters.length; ++i) {
        		if (child == parameters[i]) {
        			other.setPropertyInParent(child.getPropertyInParent());
        			other.setParent(child.getParent());
        			parameters[i]= (IASTParameterDeclaration) other;
        			return;
        		}
        	}
        }
        super.replace(child, other);
	}
}
