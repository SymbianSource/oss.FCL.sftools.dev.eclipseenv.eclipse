/*******************************************************************************
 * Copyright (c) 2000, 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.make.internal.ui.text.makefile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class MakefilePartitionScanner extends RuleBasedPartitionScanner {
	// Partition types
	public final static String MAKEFILE_COMMENT_PARTITION = "makefile_comment"; //$NON-NLS-1$
	public final static String MAKEFILE_OTHER_PARTITION = IDocument.DEFAULT_CONTENT_TYPE;

	public final static String[] MAKE_PARTITIONS =
		new String[] {
			MAKEFILE_COMMENT_PARTITION,
			MAKEFILE_OTHER_PARTITION,
	};

	/** The predefined delimiters of this tracker */
	private char[][] fModDelimiters = { { '\r', '\n' }, { '\r' }, { '\n' } };

	/**
	 * Constructor for MakefilePartitionScanner
	 */
	public MakefilePartitionScanner() {
		super();

		IToken tComment = new Token(MAKEFILE_COMMENT_PARTITION);

		List<EndOfLineRule> rules = new ArrayList<EndOfLineRule>();

		// Add rule for single line comments.

		rules.add(new EndOfLineRule("#", tComment, '\\', true)); //$NON-NLS-1$

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);

	}

	/*
	 * @see ICharacterScanner#getLegalLineDelimiters
	 */
	@Override
	public char[][] getLegalLineDelimiters() {
		return fModDelimiters;
	}

}
