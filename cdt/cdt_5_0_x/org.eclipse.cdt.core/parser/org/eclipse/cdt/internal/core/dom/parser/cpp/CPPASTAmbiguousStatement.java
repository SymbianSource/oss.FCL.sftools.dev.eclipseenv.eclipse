/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.internal.core.dom.parser.IASTAmbiguousStatement;

public class CPPASTAmbiguousStatement extends CPPASTAmbiguity implements
        IASTAmbiguousStatement {

    private IASTStatement [] stmts = new IASTStatement[2];
    private int stmtsPos=-1;
    
    public CPPASTAmbiguousStatement(IASTStatement... statements) {
		for(IASTStatement s : statements)
			addStatement(s);
	}

	public void addStatement(IASTStatement s) {
    	if (s != null) {
    		stmts = (IASTStatement[]) ArrayUtil.append( IASTStatement.class, stmts, ++stmtsPos, s );
    		s.setParent(this);
			s.setPropertyInParent(STATEMENT);
    	}
    }

    public IASTStatement[] getStatements() {
        stmts = (IASTStatement[]) ArrayUtil.removeNullsAfter( IASTStatement.class, stmts, stmtsPos );
    	return stmts;
    }
    
    @Override
	protected IASTNode[] getNodes() {
        return getStatements();
    }
}
