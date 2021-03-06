/*******************************************************************************
 * Copyright (c) 2004, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.browser;

import org.eclipse.core.runtime.IPath;

public class UnknownTypeInfo extends TypeInfo {
    
    public UnknownTypeInfo(String name, IPath path) {
		this(new QualifiedTypeName(name));
		if (path != null) {
		    addReference(new TypeReference(path, null));
		}
    }

	public UnknownTypeInfo(IQualifiedTypeName typeName) {
		super(0, typeName);
	}
	
	@Override
	public boolean isUndefinedType() {
		return true;
	}

	@Override
	public boolean canSubstituteFor(ITypeInfo info) {
		int compareType = info.getCElementType();
		if (fElementType == 0 || compareType == 0 || fElementType == compareType) {
			return fQualifiedName.equals(info.getQualifiedTypeName());
		}
		return false;
	}
}
