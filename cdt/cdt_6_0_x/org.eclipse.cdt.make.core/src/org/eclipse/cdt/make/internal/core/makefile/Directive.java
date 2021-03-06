/*******************************************************************************
 * Copyright (c) 2000, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.make.internal.core.makefile;

import org.eclipse.cdt.make.core.makefile.IDirective;
import org.eclipse.cdt.make.core.makefile.IMakefile;

public abstract class Directive implements IDirective {

	int endLine;
	int startLine;
	Directive parent;

	public Directive(Directive owner) {
		parent = owner;
	}

	public Directive(int start, int end) {
		setLines(start, end);
	}

	public abstract String toString();

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.core.makefile.IDirective#getEndLine()
	 */
	public int getEndLine() {
		return endLine;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.core.makefile.IDirective#getStartLine()
	 */
	public int getStartLine() {
		return startLine;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.core.makefile.IDirective#getParent()
	 */
	public IDirective getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.core.makefile.IDirective#getMakefile()
	 */
	public IMakefile getMakefile() {
		return parent.getMakefile();
	}

	public void setParent(Directive owner) {
		parent = owner;
	}

	public void setStartLine(int lineno) {
		startLine = lineno;
	}

	public void setEndLine(int lineno) {
		endLine = lineno;
	}

	public void setLines(int start, int end) {
		setStartLine(start);
		setEndLine(end);
	}
}
