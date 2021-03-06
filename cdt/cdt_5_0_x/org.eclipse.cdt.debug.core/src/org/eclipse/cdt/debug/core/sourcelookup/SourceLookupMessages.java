/*******************************************************************************
 * Copyright (c) 2004, 2005 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.core.sourcelookup;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class SourceLookupMessages {

	private static final String BUNDLE_NAME = "org.eclipse.cdt.debug.core.sourcelookup.SourceLookupMessages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle( BUNDLE_NAME );

	private SourceLookupMessages() {
	}

	public static String getString( String key ) {
		// TODO Auto-generated method stub
		try {
			return RESOURCE_BUNDLE.getString( key );
		}
		catch( MissingResourceException e ) {
			return '!' + key + '!';
		}
	}
}
