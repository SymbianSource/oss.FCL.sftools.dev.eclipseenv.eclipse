/*******************************************************************************
 * Copyright (c) 2009 Nokia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	Nokia - Initial API and implementation. Nov, 2009.
 *******************************************************************************/
package org.eclipse.cdt.dsf.mi.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.debug.core.model.ICBreakpoint;
import org.eclipse.cdt.debug.core.model.ICBreakpointExtension;
import org.eclipse.cdt.debug.core.model.ICLineBreakpoint;
import org.eclipse.cdt.debug.core.model.ICWatchpoint;
import org.eclipse.cdt.debug.internal.core.breakpoints.BreakpointProblems;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator;
import org.eclipse.cdt.dsf.debug.service.IBreakpointAttributeTranslatorExtension;
import org.eclipse.cdt.dsf.debug.service.IDsfBreakpointExtension;
import org.eclipse.cdt.dsf.debug.service.ISourceLookup;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.BreakpointEventType;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.ITargetBreakpointInfo;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.ISourceLookup.ISourceLookupDMContext;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;

public class MIBreakpointAttributeTranslator implements IBreakpointAttributeTranslatorExtension {

    private final static String GDB_DEBUG_MODEL_ID = "org.eclipse.cdt.dsf.gdb"; //$NON-NLS-1$

    // Extra breakpoint attributes
    private static final String ATTR_DEBUGGER_PATH = GdbPlugin.PLUGIN_ID + ".debuggerPath";   //$NON-NLS-1$
    private static final String ATTR_THREAD_FILTER = GdbPlugin.PLUGIN_ID + ".threadFilter";   //$NON-NLS-1$
//    private static final String ATTR_THREAD_ID     = GdbPlugin.PLUGIN_ID + ".threadID";       //$NON-NLS-1$

	private DsfServicesTracker	dsfServicesTracker;
	private DsfSession			dsfSession;
	private BreakpointsMediator	breakpointsMediator;
	private String				debugModelId;

	/**
	 * Manage breakpoint problem markers. <br>
	 * It's better be done by MIBreakpoints service so that it's accessible by
	 * the MIBreakpoints service too. But to minimize change to MIBreakpoints in
	 * this iteration, I just put it here..... 11/18/09
	 */
    private Map<ICBreakpoint, IMarker> fBreakpointMarkerProblems =
        new HashMap<ICBreakpoint, IMarker>();

	public MIBreakpointAttributeTranslator(DsfSession dsfSession, String debugModelId) {
		super();
		this.dsfSession = dsfSession;
		this.debugModelId = debugModelId;
		
		dsfServicesTracker = new DsfServicesTracker(GdbPlugin.getDefault().getBundle().getBundleContext(), dsfSession.getId());
	}

	public boolean canUpdateAttributes(IBreakpointDMContext bp, Map<String, Object> delta) {
		/*
		 * This method decides whether we need to re-install the breakpoint
		 * based on the attributes change (refer to caller in
		 * BreakpointsMediator).
		 */         
        // Check if there is any modified attribute
        if (delta == null || delta.size() == 0)
            return true;

        // Check the "critical" attributes
        if (delta.containsKey(ATTR_DEBUGGER_PATH)            // File name
        ||  delta.containsKey(MIBreakpoints.LINE_NUMBER)     // Line number
        ||  delta.containsKey(MIBreakpoints.FUNCTION)        // Function name
        ||  delta.containsKey(MIBreakpoints.ADDRESS)         // Absolute address
        ||  delta.containsKey(ATTR_THREAD_FILTER)            // Thread ID
        ||  delta.containsKey(MIBreakpoints.EXPRESSION)      // Watchpoint expression
        ||  delta.containsKey(MIBreakpoints.READ)            // Watchpoint type
        ||  delta.containsKey(MIBreakpoints.WRITE)) {        // Watchpoint type
            return false;
        }

        // for other attrs (ICBreakpoint.INSTALL_COUNT, ICBreakpoint.IGNORE_COUNT,
        // ICBreakpoint.CONDITION, etc), we can update them.
        return true;
	}

	public void dispose() {
		if (dsfServicesTracker != null)
			dsfServicesTracker.dispose();
		dsfSession = null;
		
		clearBreakpointProblemMarkers();
	}

	public List<Map<String, Object>> getBreakpointAttributes(IBreakpoint bp, boolean bpManagerEnabled)
			throws CoreException {
		// deprecated
		List<Map<String, Object>> retVal = new ArrayList<Map<String, Object>>();
		return retVal;
	}

	public void initialize(BreakpointsMediator mediator) {
		breakpointsMediator = mediator;
	}

	public boolean supportsBreakpoint(IBreakpoint bp) {
        if (bp instanceof ICBreakpoint && bp.getModelIdentifier().equals(debugModelId)) {
            IMarker marker = bp.getMarker();
            if (marker != null) {
                return true;
            }
        }
        return false;
	}

	public void updateBreakpointStatus(IBreakpoint bp) {
		// obsolet, do nothing.
	}

	public void updateBreakpointsStatus(Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> bpsInfo,
			BreakpointEventType eventType) {
		for (IBreakpoint bp : bpsInfo.keySet()) {
			if (! (bp instanceof ICBreakpoint))	// not C breakpoints, bail out.
				return;
			
			final ICBreakpoint icbp = (ICBreakpoint) bp;

			Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]> targetBpPerContext = bpsInfo.get(bp);
			
			switch (eventType) {
			case ADDED: {
				int installCountTotal = 0;
				StringBuffer errMsg = new StringBuffer();
				for (ITargetBreakpointInfo[] tbpInfos : targetBpPerContext.values()) {
					// For each BpTargetDMContext, we increment the installCount for each
					// target BP that has been successfully installed.
					int installCountPerContext = 0;
					for (ITargetBreakpointInfo tbp : tbpInfos) {
						if (tbp.getTargetBreakpoint() != null)
							installCountPerContext++;
						else // failure in installation
							errMsg.append(tbp.getStatus().getMessage()).append('\n');
					}
					installCountTotal += installCountPerContext;
				}

				for (int i=0; i < installCountTotal; i++)
					try {
						// this will eventually carried out in a workspace runnable.
						icbp.incrementInstallCount();
					} catch (CoreException e) {
						GdbPlugin.getDefault().getLog().log(e.getStatus());
					}
				
				if (errMsg.length() > 0)
					addBreakpointProblemMarker(icbp, errMsg.toString(), IMarker.SEVERITY_WARNING);
				else // no error, clean message if any.
					removeBreakpointProblemMarker(icbp);
				
				break;
				}
			case MODIFIED:
				break;

			case REMOVED: {
				int removeCountTotal = 0;
				for (ITargetBreakpointInfo[] tbpInfos : targetBpPerContext.values()) {
					// For each BpTargetDMContext, we decrement the installCount for each
					// target BP that we tried to remove, even if the removal failed. That's
					// because I've not seen a way to tell platform that removal fails 
					// and the BP should be kept in UI.
					removeCountTotal += tbpInfos.length;
				}

				for (int i=0; i < removeCountTotal; i++)
					try {
						if (icbp.isRegistered())	// not deleted in UI
							icbp.decrementInstallCount();
					} catch (CoreException e) {
						GdbPlugin.getDefault().getLog().log(e.getStatus());
					}
				break;
				}
			}
		}
	}

	public Map<String, Object> convertAttributes(Map<String, Object> platformAttrs) {
		Map<String, Object> targetAttrs = new HashMap<String, Object>();

		if (platformAttrs.containsKey(MIBreakpoints.BREAKPOINT_TYPE))
			targetAttrs.put(MIBreakpoints.BREAKPOINT_TYPE,      platformAttrs.get(MIBreakpoints.BREAKPOINT_TYPE));
        	
		if (platformAttrs.containsKey(ICWatchpoint.EXPRESSION))
			targetAttrs.put(MIBreakpoints.EXPRESSION,      platformAttrs.get(ICWatchpoint.EXPRESSION));
		if (platformAttrs.containsKey(ICWatchpoint.READ))
			targetAttrs.put(MIBreakpoints.READ,            platformAttrs.get(ICWatchpoint.READ));
		if (platformAttrs.containsKey(ICWatchpoint.WRITE))
			targetAttrs.put(MIBreakpoints.WRITE,           platformAttrs.get(ICWatchpoint.WRITE));

		if (platformAttrs.containsKey(ICBreakpoint.SOURCE_HANDLE))
			targetAttrs.put(MIBreakpoints.FILE_NAME,       platformAttrs.get(ICBreakpoint.SOURCE_HANDLE));

		if (platformAttrs.containsKey(IMarker.LINE_NUMBER))
			targetAttrs.put(MIBreakpoints.LINE_NUMBER,     platformAttrs.get(IMarker.LINE_NUMBER));
		if (platformAttrs.containsKey(ICLineBreakpoint.FUNCTION))
			targetAttrs.put(MIBreakpoints.FUNCTION,        platformAttrs.get(ICLineBreakpoint.FUNCTION));
		if (platformAttrs.containsKey(ICLineBreakpoint.ADDRESS))
			targetAttrs.put(MIBreakpoints.ADDRESS,         platformAttrs.get(ICLineBreakpoint.ADDRESS));
		
		if (platformAttrs.containsKey(ICBreakpoint.CONDITION))
			targetAttrs.put(MIBreakpoints.CONDITION,           platformAttrs.get(ICBreakpoint.CONDITION));
		if (platformAttrs.containsKey(ICBreakpoint.IGNORE_COUNT))
			targetAttrs.put(MIBreakpoints.IGNORE_COUNT,        platformAttrs.get(ICBreakpoint.IGNORE_COUNT));
		if (platformAttrs.containsKey(ICBreakpoint.ENABLED))
			targetAttrs.put(MIBreakpoints.IS_ENABLED,          platformAttrs.get(ICBreakpoint.ENABLED));
		
		return targetAttrs;
	}

    @SuppressWarnings("unchecked")
	public void resolveBreakpoint(IBreakpointsTargetDMContext context, IBreakpoint breakpoint, 
    		Map<String, Object> bpAttributes, final DataRequestMonitor<List<Map<String, Object>>> drm) {
		
    	assert dsfSession.getExecutor().isInExecutorThread();
    	
    	// Create a copy as we don't want to change "bpAttributes".
		final Map<String, Object>	targetBPAttrBase = new HashMap<String, Object>(bpAttributes);
		
		final Set<String> threads = (Set<String>) targetBPAttrBase.get(ATTR_THREAD_FILTER);

		final List<Map<String, Object>>	targetBPList = new ArrayList<Map<String, Object>>();

		// get debugger path (compilation-path) for source file, if any.
		determineDebuggerPath(context, targetBPAttrBase, new RequestMonitor(dsfSession.getExecutor(), drm){
			@Override
			protected void handleSuccess() {
				// Create attribute list for each thread
				for (String thread : threads) {
					Map<String, Object> targetBP = new HashMap<String, Object>(targetBPAttrBase);
					targetBP.put(MIBreakpointDMData.THREAD_ID, thread);
					targetBPList.add(targetBP);
				}

				drm.setData(targetBPList);
				drm.done();
			}});
	}

    /**
     * determineDebuggerPath
     * 
     * Adds the path to the source file to the set of attributes
     * (for the debugger).
     * 
     * @param dmc
     * @param targetAttrs
     * @param rm
     */
    private void determineDebuggerPath(IBreakpointsTargetDMContext dmc,
            final Map<String, Object> targetAttrs, final RequestMonitor rm)
    {
        String hostPath = (String) targetAttrs.get(MIBreakpoints.FILE_NAME);

        if (hostPath != null) {
            ISourceLookup sourceService   = dsfServicesTracker.getService(ISourceLookup.class);

            ISourceLookupDMContext srcDmc = DMContexts.getAncestorOfType(dmc, ISourceLookupDMContext.class);
            if (srcDmc != null) {
                sourceService.getDebuggerPath(srcDmc, hostPath,
                    new DataRequestMonitor<String>(dsfSession.getExecutor(), rm) {
                        @Override
                        protected void handleSuccess() {
                            targetAttrs.put(ATTR_DEBUGGER_PATH, adjustDebuggerPath(getData()));
                            rm.done();
                        }
                    });
            } else {
                // Source lookup not available for given context, use the host
                // path for the debugger path.
                targetAttrs.put(ATTR_DEBUGGER_PATH, adjustDebuggerPath(hostPath));
                rm.done();
            }
        } else {
            // Some types of breakpoints do not require a path
            // (e.g. watchpoints)
            rm.done();
        }
    }

    /**
     * See bug232415
     * 
     * @param path	the absolute path to the source file
     * @return
     */
    private String adjustDebuggerPath(String path) {
    	String result = path;
    	// Make it MinGW-specific
    	if (Platform.getOS().startsWith("win")) { //$NON-NLS-1$
        	if (!path.startsWith("/")) { //$NON-NLS-1$
        		result = path.substring(path.lastIndexOf('\\') + 1);
        	}
    	}
    	return result;
    }

	/**
	 * Get the list of threads from the platform breakpoint attributes
	 * 
	 * @param context
	 *            if the context is not null, only get threads that are children
	 *            of this context. otherwise get all threads.
	 * @param breakpoint
	 * @return
	 */
    private Set<String> extractThreads(IBreakpointsTargetDMContext context, ICBreakpoint breakpoint) {
        Set<String> results = new HashSet<String>();

        // Find the ancestor
        List<IExecutionDMContext[]> threads = new ArrayList<IExecutionDMContext[]>(1);

        try {
            // Retrieve the targets
            IDsfBreakpointExtension filterExtension = getFilterExtension(breakpoint);
            IContainerDMContext[] targets = filterExtension.getTargetFilters();

            // If no target is present, breakpoint applies to all.
            if (targets.length == 0) {
                results.add("0"); //$NON-NLS-1$    
                return results;
            }

            // Extract the thread IDs (if there is none, we are covered)
            for (IContainerDMContext ctxt : targets) {
                if (context == null || 
                	DMContexts.isAncestorOf(ctxt, context)) {
                    threads.add(filterExtension.getThreadFilters(ctxt));
                }
            }
        } catch (CoreException e1) {
        }

        if (supportsThreads(breakpoint)) {
            for (IExecutionDMContext[] targetThreads : threads) {
                if (targetThreads != null) {
                    for (IExecutionDMContext thread : targetThreads) {
                        if (thread instanceof IMIExecutionDMContext) {
                        	IMIExecutionDMContext dmc = (IMIExecutionDMContext) thread;
                            results.add(((Integer) dmc.getThreadId()).toString());
                        }
                    }
                } else {
                    results.add("0"); //$NON-NLS-1$    
                    break;
                }
            }
        } else {
            results.add("0"); //$NON-NLS-1$
        }

        return results;
    }

    /**
     * Indicates if the back-end supports multiple threads for
     * this type of breakpoint
     * 
     * @param breakpoint
     */
    protected boolean supportsThreads(ICBreakpoint breakpoint) {

        return !(breakpoint instanceof ICWatchpoint);
    }

    /**
     * @param bp
     * @return
     * @throws CoreException
     */
    private IDsfBreakpointExtension getFilterExtension(ICBreakpoint bp) throws CoreException {
        return (IDsfBreakpointExtension) bp.getExtension(GDB_DEBUG_MODEL_ID, ICBreakpointExtension.class);
    }

	public Map<String, Object> getAllBreakpointAttributes(IBreakpoint breakpoint, boolean bpManagerEnabled) throws CoreException {

		assert ! dsfSession.getExecutor().isInExecutorThread();

		// Check that the marker exists and retrieve its attributes.
		// Due to accepted race conditions, the breakpoint marker may become
		// null while this method is being invoked. In this case throw an exception
		// and let the caller handle it.
		IMarker marker = breakpoint.getMarker();
		if (marker == null || !marker.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, DebugException.REQUEST_FAILED,
					"Breakpoint marker does not exist", null));
		}
		
		// Suppress cast warning: platform is still on Java 1.3
		@SuppressWarnings("unchecked")
		Map<String, Object> attributes = marker.getAttributes();

		Map<String, Object>	targetAttrs = convertAttributes(attributes);

		// Determine breakpoint type.
        if (breakpoint instanceof ICWatchpoint)
            targetAttrs.put(MIBreakpoints.BREAKPOINT_TYPE, MIBreakpoints.WATCHPOINT);
        else if (breakpoint instanceof ICLineBreakpoint)
            targetAttrs.put(MIBreakpoints.BREAKPOINT_TYPE, MIBreakpoints.BREAKPOINT);
        else {
	    	// catchpoint?
	    }
        	
		// Adjust for "skip-all"
		if (!bpManagerEnabled) {
		    targetAttrs.put(MIBreakpoints.IS_ENABLED, false);
		}

		Set<String> threads = extractThreads(null, (ICBreakpoint) breakpoint);
        targetAttrs.put(ATTR_THREAD_FILTER, threads);
        
        return targetAttrs;
	}

	public boolean canUpdateAttributes(IBreakpoint bp, IBreakpointsTargetDMContext context, Map<String, Object> attrDelta) {
		boolean yesWeCan;
		
		if (attrDelta.containsKey(MIBreakpoints.IS_ENABLED) && bp != null) {
			// GDB special: 
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=261082
			//
			Map<String, Object> delta = new HashMap<String, Object>(attrDelta);
			
			delta.remove(MIBreakpoints.IS_ENABLED);
			
			yesWeCan = canUpdateAttributes(null, delta);
			
			if (yesWeCan) {
				// other attribute change indicates we can. Now check the ENABLE.
				// if the breakpoint is already installed in the "context" (a process), 
				// we can. Otherwise no.
				ITargetBreakpointInfo[] targetBPs = breakpointsMediator.getTargetBreakpoints(context, bp);
				yesWeCan = targetBPs != null && targetBPs.length > 0;
			}
		}
		else 
			yesWeCan = canUpdateAttributes(null, attrDelta);
		
		return yesWeCan;
	}

    private void addBreakpointProblemMarker(final ICBreakpoint breakpoint, final String description, final int severity) {

        new Job("Add Breakpoint Problem Marker") { //$NON-NLS-1$
        	{setSystem(true); };
        	
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                
                if (breakpoint instanceof ICLineBreakpoint) {
                	// If we have already have a problem marker on this breakpoint
                	// we should remove it first.
                    IMarker marker = fBreakpointMarkerProblems.remove(breakpoint);
                    if (marker != null) {
                        try {
                            marker.delete();
                        } catch (CoreException e) {
                        }
                	}

                    ICLineBreakpoint lineBreakpoint = (ICLineBreakpoint) breakpoint;
                    try {
                        // Locate the workspace resource via the breakpoint marker
                        IMarker breakpoint_marker = lineBreakpoint.getMarker();
                        IResource resource = breakpoint_marker.getResource();

                        // Add a problem marker to the resource
                        IMarker problem_marker = resource.createMarker(BreakpointProblems.BREAKPOINT_PROBLEM_MARKER_ID);
                        int line_number = lineBreakpoint.getLineNumber();
                        problem_marker.setAttribute(IMarker.LOCATION,    String.valueOf(line_number));
                        problem_marker.setAttribute(IMarker.MESSAGE,     description);
                        problem_marker.setAttribute(IMarker.SEVERITY,    severity);
                        problem_marker.setAttribute(IMarker.LINE_NUMBER, line_number);

                        // And save the baby
                        fBreakpointMarkerProblems.put(breakpoint, problem_marker);
                    } catch (CoreException e) {
                    }
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void removeBreakpointProblemMarker(final ICBreakpoint breakpoint) {

        new Job("Remove Breakpoint Problem Marker") { //$NON-NLS-1$
        	{setSystem(true); };
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                
                IMarker marker = fBreakpointMarkerProblems.remove(breakpoint);
                if (marker != null) {
                    try {
                        marker.delete();
                    } catch (CoreException e) {
                    }
                }

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /**
     */
    private void clearBreakpointProblemMarkers()
    {
        new Job("Clear Breakpoint problem markers") { //$NON-NLS-1$
        	{ setSystem(true); };
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                for (IMarker marker : fBreakpointMarkerProblems.values()) {
                	if (marker != null) {
                		try {
							marker.delete();
						} catch (CoreException e) {
						}
                	}
                }
                fBreakpointMarkerProblems.clear();
                return Status.OK_STATUS;
            }
        }.schedule();
    }
}
 