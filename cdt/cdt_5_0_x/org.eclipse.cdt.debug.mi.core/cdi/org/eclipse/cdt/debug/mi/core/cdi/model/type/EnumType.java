/*******************************************************************************
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.debug.mi.core.cdi.model.type;

import org.eclipse.cdt.debug.core.cdi.model.type.ICDIEnumType;
import org.eclipse.cdt.debug.mi.core.cdi.model.Target;

/**
 */
public class EnumType extends IntegralType implements ICDIEnumType {

	/**
	 * @param typename
	 */
	public EnumType(Target target, String typename) {
		this(target, typename, false);
	}

	public EnumType(Target target, String typename, boolean usigned) {
		super(target, typename, usigned);
	}
}
