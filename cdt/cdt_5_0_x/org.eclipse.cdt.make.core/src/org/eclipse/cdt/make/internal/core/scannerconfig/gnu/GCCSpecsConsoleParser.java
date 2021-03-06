/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - Initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.make.internal.core.scannerconfig.gnu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector;
import org.eclipse.cdt.make.core.scannerconfig.IScannerInfoConsoleParser;
import org.eclipse.cdt.make.core.scannerconfig.ScannerInfoTypes;
import org.eclipse.cdt.make.internal.core.scannerconfig.util.TraceUtil;
import org.eclipse.cdt.make.internal.core.scannerconfig2.PerProjectSICollector;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 * Parses output of gcc -c -v specs.c or
 *                  g++ -c -v specs.cpp
 * command
 * 
 * @author vhirsl
 */
public class GCCSpecsConsoleParser implements IScannerInfoConsoleParser {
	private final String INCLUDE = "#include"; //$NON-NLS-1$
	private final String DEFINE = "#define"; //$NON-NLS-1$

	private IProject fProject = null;
	private IScannerInfoCollector fCollector = null;
	
	private boolean expectingIncludes = false;
	private List<String> symbols = new ArrayList<String>();
	private List<String> includes = new ArrayList<String>();

    /* (non-Javadoc)
     * @see org.eclipse.cdt.make.core.scannerconfig.IScannerInfoConsoleParser#startup(org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IPath, org.eclipse.cdt.make.core.scannerconfig.IScannerInfoCollector, org.eclipse.cdt.core.IMarkerGenerator)
     */
    public void startup(IProject project, IPath workingDirectory, IScannerInfoCollector collector, IMarkerGenerator markerGenerator) {
		this.fProject = project;
		this.fCollector = collector;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.internal.core.scannerconfig.IScannerInfoConsoleParser#processLine(java.lang.String)
	 */
	public boolean processLine(String line) {
		boolean rc = false;
		line= line.trim();
		TraceUtil.outputTrace("GCCSpecsConsoleParser parsing line: [", line, "]");	//$NON-NLS-1$ //$NON-NLS-2$

		// contribution of -dD option
		if (line.startsWith(DEFINE)) {
			String[] defineParts = line.split("\\s+", 3); //$NON-NLS-1$
			if (defineParts[0].equals(DEFINE)) {
				String symbol = null;
				switch (defineParts.length) {
					case 2:
						symbol = defineParts[1];
						break;
					case 3:
						symbol = defineParts[1] + "=" + defineParts[2]; //$NON-NLS-1$
						break;
				}
				if (symbol != null && !symbols.contains(symbol)) { 
					symbols.add(symbol);
				}
			}
		}
		// now get all the includes
		else if (line.startsWith(INCLUDE) && line.endsWith("search starts here:")) { //$NON-NLS-1$
			expectingIncludes = true;
		}
		else if (line.startsWith("End of search list.")) {	//$NON-NLS-1$
			expectingIncludes = false;
		}
		else if (expectingIncludes) {
			if (!includes.contains(line))
				includes.add(line);
		}
			
		return rc;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.make.internal.core.scannerconfig.IScannerInfoConsoleParser#shutdown()
	 */
	public void shutdown() {
		Map<ScannerInfoTypes, List<String>> scannerInfo = new HashMap<ScannerInfoTypes, List<String>>();
		scannerInfo.put(ScannerInfoTypes.INCLUDE_PATHS, includes);
		scannerInfo.put(ScannerInfoTypes.SYMBOL_DEFINITIONS, symbols);
		if (fCollector != null) {
			if (fCollector instanceof PerProjectSICollector) {
				((PerProjectSICollector) fCollector).contributeToScannerConfig(fProject, scannerInfo, true);
			} else {
				fCollector.contributeToScannerConfig(fProject, scannerInfo);
			}
		}
		TraceUtil.outputTrace("Scanner info from \'specs\' file",	//$NON-NLS-1$
				"Include paths", includes, Collections.emptyList(), "Defined symbols", symbols);	//$NON-NLS-1$ //$NON-NLS-2$);
	}

}
