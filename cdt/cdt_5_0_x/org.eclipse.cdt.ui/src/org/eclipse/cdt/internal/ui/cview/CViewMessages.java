/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.cview;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CViewMessages {

	private static final String RESOURCE_BUNDLE= "org.eclipse.cdt.internal.ui.cview.CViewMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle;
	static {
		try {
			fgResourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
		} catch (MissingResourceException x) {
			fgResourceBundle = null;
		}
	}

	private CViewMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		} catch (NullPointerException e) {
			return "#" + key + "#"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Gets a string from the resource bundle and formats it with the argument
	 * 
	 * @param key	the string used to get the bundle value, must not be null
	 */
	public static String getFormattedString(String key, Object arg) {
		String format= null;
		try {
			format= fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
		if (arg == null)
			arg= ""; //$NON-NLS-1$
		return MessageFormat.format(format, new Object[] { arg });
	}

	/**
	 * Gets a string from the resource bundle and formats it with arguments
	 */	
	public static String getFormattedString(String key, String[] args) {
		return MessageFormat.format(fgResourceBundle.getString(key), (Object[]) args);
	}

}
