/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.internal.core.pdom.dom.cpp;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMNode;
import org.eclipse.cdt.core.dom.IPDOMVisitor;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespace;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPNamespaceScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDirective;
import org.eclipse.cdt.internal.core.pdom.db.Database;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMBinding;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMLinkage;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMNode;
import org.eclipse.core.runtime.CoreException;

/**
 * Stores using directives for global or namespace scope. Directives for block-scopes 
 * are not persisted in the index.
 * For performance reasons the directives are not stored with their container. Rather
 * than that they are stored with the file, in which they are encountered. 
 * When parsing a file the directives from headers that are skipped are collected.
 */
public class PDOMCPPUsingDirective implements ICPPUsingDirective, IPDOMNode {
	private static final int CONTAINER_NAMESPACE 	= 0;
	private static final int NOMINATED_NAMESPACE    = 4;
	private static final int PREV_DIRECTIVE_OF_FILE	= 8;
	private static final int RECORD_SIZE 			= 12;

	private final PDOMCPPLinkage fLinkage;
	private final int fRecord;

	PDOMCPPUsingDirective(PDOMCPPLinkage pdom, int record) {
		fLinkage= pdom;
		fRecord= record;
	}

	public PDOMCPPUsingDirective(PDOMCPPLinkage linkage, int prevRecInFile, PDOMCPPNamespace containerNS, PDOMBinding nominated) throws CoreException {
		final Database db= linkage.getPDOM().getDB();
		final int containerRec= containerNS == null ? 0 : containerNS.getRecord();
		final int nominatedRec= nominated.getRecord();
		
		fLinkage= linkage;
		fRecord= db.malloc(RECORD_SIZE);
		db.putInt(fRecord + CONTAINER_NAMESPACE, containerRec);
		db.putInt(fRecord + NOMINATED_NAMESPACE, nominatedRec);
		db.putInt(fRecord + PREV_DIRECTIVE_OF_FILE, prevRecInFile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDirective#getNamespace()
	 */
	public ICPPNamespaceScope getNominatedScope() {
		try {
			int rec = fLinkage.getPDOM().getDB().getInt(fRecord + NOMINATED_NAMESPACE);
			PDOMNode node= fLinkage.getNode(rec);
			if (node instanceof ICPPNamespace) {
				return ((ICPPNamespace) node).getNamespaceScope();
			}
		} catch (CoreException e) {
			CCorePlugin.log(e);
		} catch (DOMException e) {
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDirective#getContainingScope()
	 */
	public IScope getContainingScope() {
		try {
			int rec = fLinkage.getPDOM().getDB().getInt(fRecord + CONTAINER_NAMESPACE);
			if (rec != 0) {
				PDOMNode node= fLinkage.getNode(rec);
				if (node instanceof PDOMCPPNamespace) {
					return (PDOMCPPNamespace) node;
				}
			}
		} catch (CoreException e) {
			CCorePlugin.log(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.ast.cpp.ICPPUsingDirective#getPointOfDeclaration()
	 */
	public int getPointOfDeclaration() {
		return 0;
	}

	public int getRecord() {
		return fRecord;
	}

	public int getPreviousRec() throws CoreException {
		final Database db= fLinkage.getPDOM().getDB();
		return db.getInt(fRecord + PREV_DIRECTIVE_OF_FILE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.dom.IPDOMNode#accept(org.eclipse.cdt.core.dom.IPDOMVisitor)
	 */
	public void accept(IPDOMVisitor visitor) throws CoreException {
	}

	public void delete(PDOMLinkage linkage) throws CoreException {
		fLinkage.getPDOM().getDB().free(fRecord);
	}
}
