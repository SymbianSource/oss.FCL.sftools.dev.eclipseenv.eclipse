/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.text;

import org.eclipse.jface.text.rules.IToken;

/**
 * A simple whitespace rule with configurable token.
 */
public class CWhitespaceRule extends SingleCharRule {

	public CWhitespaceRule(IToken token) {
		super(token);
	}

	@Override
	protected boolean isRuleChar(int ch) {
		switch (ch) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			return true;
		default:
			return false;
		}
	}

}
