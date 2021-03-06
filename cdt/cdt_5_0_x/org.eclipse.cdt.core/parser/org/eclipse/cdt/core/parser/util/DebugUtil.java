/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.parser.util;


/**
 * This class contains several convenience methods 
 * mainly for debugging purposes.
 * 
 * @author Mike Kucera
 *
 */
@SuppressWarnings("nls")
public class DebugUtil {

	private DebugUtil() { // class just contains static methods
	}
	
	
	/**
	 * Prints a trace message to stdout that gives info
	 * about the method that calls this method.
	 */
	public static void printMethodTrace() {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		printMethodTrace(trace, null);
	}
	
	/**
	 * Prints a trace message to stdout that gives info
	 * about the method that calls this method.
	 * 
	 * The output is in a format that will show up as a hyperlink in the eclipse console.
	 */
	public static void printMethodTrace(String extraMessage) {
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		printMethodTrace(trace, extraMessage);
	}
	
	private static void printMethodTrace(StackTraceElement[] trace, String extraMessage) {
		StackTraceElement caller = trace[3];
		
		String className = caller.getClassName();
		className = className.substring(className.lastIndexOf(".") + 1);
		
		String message = String.format("%s.%s(%s:%d)",
				className, caller.getMethodName(), caller.getFileName(), caller.getLineNumber());
		
		if(extraMessage != null)
			message += ": " + extraMessage;
		
		System.out.println(message);
	}

	public static String safeClassName(Object obj) {
		return obj != null ? obj.getClass().getSimpleName() : "";
	}
	
	public static String toStringWithClass(Object obj) {
		return obj != null ?
				String.valueOf(obj) + " " + obj.getClass().getSimpleName() :
				"null";
	}
}
