/*******************************************************************************
 * Copyright (c) 2006, 2008 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX - Initial API and implementation
 * Markus Schorn (Wind River Systems)
 * IBM Corporation
 *******************************************************************************/
 
package org.eclipse.cdt.internal.core.indexer;

import java.util.List;

import org.eclipse.cdt.internal.core.dom.AbstractCodeReaderFactory;
import org.eclipse.cdt.internal.core.dom.IIncludeFileResolutionHeuristics;

/**
 * A task for index updates.
 * 
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will work or
 * that it will remain the same. Please do not use this API without consulting
 * with the CDT team.
 * </p>
 * 
 * @since 4.0
 */
public class StandaloneFullIndexerTask extends StandaloneIndexerTask {
	public StandaloneFullIndexerTask(StandaloneFullIndexer indexer, List<String> added,
			List<String> changed, List<String> removed) {
		super(indexer, added, changed, removed, false);
	}

	@Override
	protected AbstractCodeReaderFactory createReaderFactory() {
		return ((StandaloneFullIndexer)fIndexer).getCodeReaderFactory();
	}

	@Override
	protected IIncludeFileResolutionHeuristics createIncludeHeuristics() {
		return null;
	}
}
