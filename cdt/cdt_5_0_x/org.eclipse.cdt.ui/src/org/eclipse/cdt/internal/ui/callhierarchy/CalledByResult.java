/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.cdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.model.ICElement;

public class CalledByResult {
	private Map<ICElement, List<IIndexName>> fElementToReferences= new HashMap<ICElement, List<IIndexName>>();

	public ICElement[] getElements() {
		Set<ICElement> elements = fElementToReferences.keySet();
		return elements.toArray(new ICElement[elements.size()]);
	}
	
	public IIndexName[] getReferences(ICElement calledElement) {
		List<IIndexName> references= fElementToReferences.get(calledElement);
		return references.toArray(new IIndexName[references.size()]);
	}

	public void add(ICElement elem, IIndexName ref) {
		List<IIndexName> list= fElementToReferences.get(elem);
		if (list == null) {
			list= new ArrayList<IIndexName>();
			fElementToReferences.put(elem, list);
		}
		list.add(ref);
	}
}
