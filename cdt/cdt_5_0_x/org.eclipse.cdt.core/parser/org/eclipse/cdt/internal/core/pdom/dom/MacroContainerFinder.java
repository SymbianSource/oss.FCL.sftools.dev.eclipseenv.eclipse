/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.pdom.dom;

import org.eclipse.cdt.internal.core.pdom.PDOM;
import org.eclipse.cdt.internal.core.pdom.db.IBTreeVisitor;
import org.eclipse.cdt.internal.core.pdom.db.IString;
import org.eclipse.core.runtime.CoreException;

/**
 * Visitor to find a macro container in a BTree.
 */
public final class MacroContainerFinder implements IBTreeVisitor {
	private final PDOM fPdom;
	private final char[] fName;
	private PDOMMacroContainer fMacroContainer;
		
	/**
	 * Collects all nodes with given name, passing the filter. If prefixLookup is set to
	 * <code>true</code> a binding is considered if its name starts with the given prefix.
	 */
	public MacroContainerFinder(PDOM pdom, char[] name) {
		fName= name;
		fPdom= pdom;
	}
		
	final public int compare(int record) throws CoreException {
		IString name= PDOMNamedNode.getDBName(fPdom, record);
		return compare(name);
	}

	private int compare(IString rhsName) throws CoreException {
		return rhsName.compareCompatibleWithIgnoreCase(fName);
	}
	
	final public boolean visit(int record) throws CoreException {
		if (record == 0)
			return true;
		fMacroContainer= new PDOMMacroContainer(fPdom, record);
		return false; // we are done.
	}
	
	final public PDOMMacroContainer getMacroContainer() {
		return fMacroContainer;
	}
}
