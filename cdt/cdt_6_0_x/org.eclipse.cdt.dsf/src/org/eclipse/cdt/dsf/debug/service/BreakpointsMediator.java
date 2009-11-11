/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - Initial API and implementation
 *     Ericsson   - Low-level breakpoints integration  
 *******************************************************************************/

package org.eclipse.cdt.dsf.debug.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.dsf.concurrent.CountingRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.IDsfStatusConstants;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ThreadSafe;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.internal.DsfPlugin;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.osgi.framework.BundleContext;

/**
 * 
 */
public class BreakpointsMediator extends AbstractDsfService implements IBreakpointManagerListener, IBreakpointsListener
{
	public enum BreakpointEventType {ADDED, REMOVED, MODIFIED}; 	
	
    /**
     * The attribute translator that this service will use to map the platform
     * breakpoint attributes to the corresponding target attributes, and vice
     * versa.
     */
    private IBreakpointAttributeTranslator fAttributeTranslator;
    
    /**
     * If the attribute translator implements the {@link IBreakpointAttributeTranslatorExtension},
     * this field will be valid, otherwise it is null.
     */
    private IBreakpointAttributeTranslatorExtension fAttributeTranslator2;

    /**
     * DSF Debug service for creating breakpoints.
     */
    IBreakpoints fBreakpoints;
    
    /**
     * Platform breakpoint manager
     */
    IBreakpointManager fBreakpointManager;

    /**
     * Object describing the information about a single target breakpoint  
     * corresponding to specific platform breakpoint and breakpoint target 
     * context.
     * 
     * @since 2.1
     */
    public interface ITargetBreakpointInfo {

    	/**
    	 * Returns the breakpoint attributes as returned by the attribute translator.
    	 */
    	public Map<String, Object> getAttributes();

    	/**
    	 * Returns the target breakpoint context.  May be <code>null</code> if the 
    	 * breakpoint failed to install on target. 
    	 */
    	public IBreakpointDMContext getTargetBreakpoint();

    	/**
    	 * Returns the status result of the last breakpoint operation (install/remove). 
    	 */
    	public IStatus getStatus();
    }
    
    private static class TargetBP implements ITargetBreakpointInfo {
    	
    	private Map<String, Object> fAttributes;
    	private Map<String, Object> fAttributesDelta; // not really useful ?
    	private IBreakpointDMContext fTargetBPContext;
    	private IStatus fStatus;
    	
    	public TargetBP(Map<String, Object> attrs) {
    		fAttributes = attrs;
    	}
    	
    	public Map<String, Object> getAttributes() {
			return fAttributes;
		}
    	
    	public IBreakpointDMContext getTargetBreakpoint() {
			return fTargetBPContext;
		}
    	
    	public IStatus getStatus() {
			return fStatus;
		}

		public void setTargetBreakpoint(IBreakpointDMContext fTargetBPContext) {
			this.fTargetBPContext = fTargetBPContext;
		}

		public void setStatus(IStatus status) {
			this.fStatus = status;
		}
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Breakpoints tracking
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Holds the set of platform breakpoints with their breakpoint information 
     * structures, per context (i.e. each platform breakpoint is
     * replicated for each execution context).
     * - Context entry added/removed on start/stopTrackingBreakpoints()
     * - Augmented on breakpointAdded()
     * - Modified on breakpointChanged()
     * - Diminished on breakpointRemoved()
     */
	private Map<IBreakpointsTargetDMContext, Map<IBreakpoint, List<TargetBP>>> fPlatformBPs = 
		new HashMap<IBreakpointsTargetDMContext, Map<IBreakpoint, List<TargetBP>>>();

    /**
     * Due to the very asynchronous nature of DSF, a new breakpoint request can
     * pop up at any time before an ongoing one is completed. The following set
     * is used to store requests until the ongoing operation completes.
     */
	private Set<IBreakpoint> fRunningEvents    = new HashSet<IBreakpoint>();

	private static class PendingEventInfo {
		PendingEventInfo(BreakpointEventType eventType, IBreakpointsTargetDMContext bpsTargetDmc, RequestMonitor rm) {
			fEventType = eventType;
			fBPsTargetDmc = bpsTargetDmc;
			fRequestMonitor = rm;
		}
		
		RequestMonitor fRequestMonitor;
		BreakpointEventType fEventType;
		IBreakpointsTargetDMContext fBPsTargetDmc;
	}
	
	/**
	 * @see fPendingRequests
	 */
	private Map<IBreakpoint, LinkedList<PendingEventInfo>> fPendingEvents = 
		Collections.synchronizedMap(new HashMap<IBreakpoint, LinkedList<PendingEventInfo>>());
	
    ///////////////////////////////////////////////////////////////////////////
    // AbstractDsfService    
    ///////////////////////////////////////////////////////////////////////////

	/**
	 * The service constructor
	 * 
	 * @param session
	 * @param debugModelId
	 */
	public BreakpointsMediator(DsfSession session, IBreakpointAttributeTranslator attributeTranslator) {
        super(session);
        fAttributeTranslator = attributeTranslator;
        
        fAttributeTranslator2 = null;
        if (attributeTranslator instanceof IBreakpointAttributeTranslatorExtension)
        	fAttributeTranslator2 = (IBreakpointAttributeTranslatorExtension)attributeTranslator;
	}

    @Override
    public void initialize(final RequestMonitor rm) {
        // - Collect references for the services we interact with
        // - Register to interesting events
        // - Obtain the list of platform breakpoints   
        // - Register the service for interested parties
        super.initialize(
            new RequestMonitor(getExecutor(), rm) { 
                @Override
                protected void handleSuccess() {
                    doInitialize(rm);
                }});
    }

    /**
     * Asynchronous service initialization 
     * 
     * @param requestMonitor
     */
    private void doInitialize(RequestMonitor rm) {
    	
    	// Get the services references
        fBreakpoints  = getServicesTracker().getService(IBreakpoints.class);
        fBreakpointManager = DebugPlugin.getDefault().getBreakpointManager();
        fAttributeTranslator.initialize(this);

        // Register to the useful events
        fBreakpointManager.addBreakpointListener(this);
        fBreakpointManager.addBreakpointManagerListener( this );

        // Register this service
        register(new String[] { BreakpointsMediator.class.getName() },
				 new Hashtable<String, String>());

        rm.done();
    }

    @Override
    public void shutdown(final RequestMonitor rm) {
        // - Un-register the service
        // - Stop listening to events
        // - Remove the breakpoints installed by this service
        // 
        //  Since we are shutting down, there is no overwhelming need
        //  to keep the maps coherent...

        // Stop accepting requests and events
    	unregister();
        fBreakpointManager.removeBreakpointListener(this);
        fBreakpointManager.removeBreakpointManagerListener( this );
        fAttributeTranslator.dispose();

        // Cleanup the breakpoints that are still installed by the service.
        // Use a counting monitor which will call mom to complete the shutdown
        // after the breakpoints are un-installed (successfully or not).
        CountingRequestMonitor countingRm = new CountingRequestMonitor(getExecutor(), rm) {
            @Override
            protected void handleCompleted() {
                BreakpointsMediator.super.shutdown(rm);
            }
        };

        // We have to make a copy of the fPlatformBPs keys because uninstallBreakpoints()
        // modifies the map as it walks through it.
        List<IBreakpointsTargetDMContext> platformBPKeysCopy = new ArrayList<IBreakpointsTargetDMContext>(fPlatformBPs.size());
        platformBPKeysCopy.addAll(0, fPlatformBPs.keySet());
        for (IBreakpointsTargetDMContext dmc : platformBPKeysCopy) {
            stopTrackingBreakpoints(dmc, countingRm);
        }
        countingRm.setDoneCount(platformBPKeysCopy.size());
    }
    
	@Override
    protected BundleContext getBundleContext() {
        return DsfPlugin.getBundleContext();
    }

    ///////////////////////////////////////////////////////////////////////////
    // IBreakpointsManager
    ///////////////////////////////////////////////////////////////////////////


	/**
     * Install and begin tracking breakpoints for given context.  The service 
     * will keep installing new breakpoints that appear in the IDE for this 
     * context until {@link #uninstallBreakpoints(IDMContext)} is called for that
     * context.
     * @param dmc Context to start tracking breakpoints for.
     * @param rm Completion callback.
     */
    public void startTrackingBreakpoints(final IBreakpointsTargetDMContext dmc, final RequestMonitor rm) {
        // - Augment the maps with the new execution context
        // - Install the platform breakpoints on the selected target
            
        // Make sure a mapping for this execution context does not already exist
		Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(dmc);
		if (platformBPs != null) {
            rm.setStatus(new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, INTERNAL_ERROR, "Context already initialized", null)); //$NON-NLS-1$
            rm.done();            
            return;
		}

        // Create entries in the breakpoint tables for the new context. These entries should only
        // be removed when this service stops tracking breakpoints for the given context.
        fPlatformBPs.put(dmc, new HashMap<IBreakpoint, List<TargetBP>>());

        // Install the platform breakpoints (stored in fPlatformBPs) on the target.
		// We need to use a background thread for this operation because we are 
		// accessing the resources system to retrieve the breakpoint attributes.
		// Accessing the resources system potentially requires using global locks.
		// Also we will be calling IBreakpointAttributeTranslator which is prohibited
		// from being called on the session executor thread.
		new Job("Install initial breakpoint list.") { //$NON-NLS-1$
            { setSystem(true); }

			// Get the stored breakpoints from the platform BreakpointManager
			// and install them on the target
        	@Override
            protected IStatus run(IProgressMonitor monitor) {
        		doBreakpointsAdded(DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(), dmc, rm);
                return Status.OK_STATUS;
            }
        }.schedule();    
    }

    public void stopTrackingBreakpoints(final IBreakpointsTargetDMContext dmc, final RequestMonitor rm) {
        // - Remove the target breakpoints for the given execution context
        // - Update the maps

    	// Remove the breakpoints for given DMC from the internal maps.
        Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(dmc);
        if (platformBPs == null) {
            rm.setStatus(new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, INTERNAL_ERROR, "Breakpoints not installed for given context", null)); //$NON-NLS-1$
            rm.done();
            return;
        }

		new Job("Uninstall target breakpoints list.") { //$NON-NLS-1$
            { setSystem(true); }

			// Get the stored breakpoints from the platform BreakpointManager
			// and install them on the target
        	@Override
            protected IStatus run(IProgressMonitor monitor) {
        		doBreakpointsRemoved(DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(), dmc, rm);
                return Status.OK_STATUS;
            }
        }.schedule();    
    }
    
    /**
     * Find target breakpoints installed in the given context that are resolved 
     * from the given platform breakpoint.
     *  
     * @param dmc - context
     * @param platformBp - platform breakpoint
     * @return array of target breakpoints. 
     */
    public ITargetBreakpointInfo[] getTargetBreakpoints(IBreakpointsTargetDMContext dmc, IBreakpoint platformBp) {
        Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(dmc);

        if (platformBPs != null)
        {
        	List<TargetBP> bpInfo = platformBPs.get(platformBp);
            if (bpInfo != null) {
            	return bpInfo.toArray(new ITargetBreakpointInfo[bpInfo.size()]);
            }
        }
        return null;
    }
    
    /**
     * Find the platform breakpoint that's mapped to the given target breakpoint.
     * 
     * @param dmc - context of the target breakpoint, can be null.
     * @param bp - target breakpoint
     * @return platform breakpoint. null if not found. 
     */
    public IBreakpoint getPlatformBreakpoint(IBreakpointsTargetDMContext dmc, IBreakpointDMContext bp) {
    	for (IBreakpointsTargetDMContext bpContext : fPlatformBPs.keySet()) {
    		if (dmc != null && !dmc.equals(bpContext))
    			continue;
    		
	        Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(bpContext);
	
	        if (platformBPs != null && platformBPs.size() > 0)
	        {
	            for(Map.Entry<IBreakpoint, List<TargetBP>> e: platformBPs.entrySet())
	            {
	                // Stop at the first occurrence
	            	for (TargetBP tbp : e.getValue())
	            		if(tbp.getTargetBreakpoint().equals(bp))
	            			return e.getKey();
	            }    
	        }
    	}

    	return null;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Back-end interface functions
    ///////////////////////////////////////////////////////////////////////////

	/**
	 * Install a new platform breakpoint on the back-end. A platform breakpoint
	 * can resolve into multiple back-end breakpoints, e.g. when threads are taken
	 * into account.
	 *  
	 * @param dmc
	 * @param breakpoint
	 * @param attrsList - list of attribute map, each mapping to a potential target BP.
	 * @param rm
	 */
	private void installBreakpoint(IBreakpointsTargetDMContext dmc, final IBreakpoint breakpoint,
			final List<Map<String, Object>> attrsList, final DataRequestMonitor<List<TargetBP>> rm)
	{
    	// Retrieve the set of breakpoints for this context
        final Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(dmc);
        assert platformBPs != null;

        // Ensure the breakpoint is not already installed
        assert !platformBPs.containsKey(breakpoint);

        final ArrayList<TargetBP> targetBPsAttempted = new ArrayList<TargetBP>(attrsList.size());
        for (int i = 0; i < attrsList.size(); i++) {
        	targetBPsAttempted.add(new TargetBP(attrsList.get(i)));
        }
        
        final ArrayList<TargetBP> targetBPsInstalled = new ArrayList<TargetBP>(attrsList.size());

        // Update the breakpoint status when all back-end breakpoints have been installed
    	final CountingRequestMonitor installRM = new CountingRequestMonitor(getExecutor(), rm) {
			@Override
			protected void handleCompleted() {
				// Store successful targetBPs with the platform breakpoint
				if (targetBPsInstalled.size() > 0)
					platformBPs.put(breakpoint, targetBPsInstalled);
				
				// Store all targetBPs, success or failure, in the rm.
				rm.setData(targetBPsAttempted);
		        rm.done();
			}
		};

        // A back-end breakpoint needs to be installed for each specified attributes map.
		installRM.setDoneCount(attrsList.size());

		// Install the back-end breakpoint(s)
		for (int _i = 0; _i < attrsList.size(); _i++) {
			final int i = _i;
            fBreakpoints.insertBreakpoint(
                dmc, attrsList.get(i), 
				new DataRequestMonitor<IBreakpointDMContext>(getExecutor(), installRM) {
				@Override
                protected void handleCompleted() {
					TargetBP targetBP = targetBPsAttempted.get(i);
                    if (isSuccess()) {
						// Add the breakpoint back-end mapping
                    	targetBP.setTargetBreakpoint(getData());
                    	
                    	targetBPsInstalled.add(targetBP);
					} 
                    targetBP.setStatus(getStatus());
                    
					installRM.done();
                }
            });
		}
	}

    /**
     * Un-install an individual breakpoint on the back-end. For one platform
     * breakpoint, there could be multiple corresponding back-end breakpoints.
     * 
     * @param dmc
     * @param breakpoint
     * @param drm
     */
    private void uninstallBreakpoint(final IBreakpointsTargetDMContext dmc, final IBreakpoint breakpoint, 
        final DataRequestMonitor<List<TargetBP>> drm)
    {
		// Remove the back-end breakpoints
		final Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(dmc);
        if (platformBPs == null) {
            drm.setStatus(new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, INVALID_HANDLE, "Invalid breakpoint", null)); //$NON-NLS-1$
            drm.done();
            return;
        }

        final List<TargetBP> bpList = platformBPs.get(breakpoint);
        assert bpList != null;

        // Only try to remove those targetBPs that are successfully installed.
        
  		// Remove completion monitor
    	final CountingRequestMonitor countingRm = new CountingRequestMonitor(getExecutor(), drm) {
			@Override
			protected void handleCompleted() {
				platformBPs.remove(breakpoint);

		        // Complete the request monitor.
		        drm.setData(bpList);
		        drm.done();
			}
		};

        int count = 0;
        for (int i = 0; i < bpList.size(); i++) {
        	final TargetBP bp = bpList.get(i);
        	if (bp.getTargetBreakpoint() != null) {
        		fBreakpoints.removeBreakpoint(
        				bp.getTargetBreakpoint(), 
        				new RequestMonitor(getExecutor(), countingRm) {
        					@Override
        					protected void handleCompleted() {
        				        bp.setStatus(getStatus());
        				        bp.fAttributesDelta = bp.fAttributes;
        				        if (isSuccess()) {
            						bp.setTargetBreakpoint(null);
        				        	bp.fAttributes = null;
        				        } 
        				        countingRm.done();
        					}
        				});
        		count++;
        	} else {
        		bp.setStatus(Status.OK_STATUS);
        	}
        }
        countingRm.setDoneCount(count);
    }
	
	/**
	 * Modify an individual breakpoint
	 * 
	 * @param context
	 * @param breakpoint
	 * @param attributes
	 * @param drm
	 * @throws CoreException
	 */
	private void modifyBreakpoint(final IBreakpointsTargetDMContext context, final IBreakpoint breakpoint,
			final List<Map<String, Object>> newAttrsList, final DataRequestMonitor<List<TargetBP>> drm)
	{
	    // This method uses several lists to track the changed breakpoints:
	    // commonAttrsList - attributes which have not changed 
	    // oldAttrsList - attributes for the breakpoint before the change
	    // newAttrsList - attributes for the breakpoint after the change
	    // oldBpContexts - target-side breakpoints from before the change
	    // newBpContexts - target-side breakpoints after the change
	    // attrDeltasList - changes in the attributes for each attribute map in 
	    //     oldAttrsList and newAttrsList
	    
    	// Get the maps
        final Map<IBreakpoint, List<TargetBP>> platformBPs = fPlatformBPs.get(context);
        if (platformBPs == null) {
            drm.setStatus(new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, INVALID_HANDLE, "Invalid context", null)); //$NON-NLS-1$
            drm.done();
            return;
        }

        final List<TargetBP> oldBpList = platformBPs.get(breakpoint);
        
        final List<TargetBP> bpList = new ArrayList<TargetBP>(newAttrsList.size());
        
        if (oldBpList == null || oldBpList.size() == 0) { // not targetBP installed
            drm.setData(bpList);
        	drm.done();
        	return;
        }
        
        for (int i = 0; i < newAttrsList.size(); i++) {
        	bpList.add(new TargetBP(newAttrsList.get(i)));
        }
        
        // Create a list of attribute changes.  The length of this list will
        // always be max(oldAttrList.size(), newAttrsList.size()), padded with
        // null's if oldAttrsList was longer.
        calcBPsAttrs(oldBpList, bpList);
        
        // Create the request monitor that will be called when all
        // modifying/inserting/removing is complete.
        final CountingRequestMonitor countingRM = new CountingRequestMonitor(getExecutor(), drm) {
            @Override
            protected void handleCompleted() {
                // Save the new list of target breakpoints 
            	platformBPs.put(breakpoint, bpList);
                drm.setData(bpList);
                drm.done();
            }
        };
        
        // Set the count, if could be zero if no breakpoints have actually changed.
        int coutingRmCount = 0;
        
        // Process the changed breakpoints.
        for (int _i = 0; _i < bpList.size(); _i++) {
        	final int i = _i;
        	final TargetBP bp = bpList.get(i);
            if (bp.fAttributes == null) {
                // The list of new attribute maps was shorter than the old.
                // Remove the corresponding target-side bp.  
            	// Note the target BP context may be null if the target
            	// BP failed to insert in the first place.
            	if (bp.getTargetBreakpoint() != null) {
            		fBreakpoints.removeBreakpoint(
            				bp.getTargetBreakpoint(), 
            				new RequestMonitor(getExecutor(), countingRM) {
            					@Override
            					protected void handleCompleted() {
            						bp.fStatus = getStatus();
            						countingRM.done();
            					}
            				});
                    coutingRmCount++;
            	}
            } else if ( bp.getTargetBreakpoint() == null) {
                // The list of new attribute maps was longer, just insert
                // the new breakpoint
                final Map<String, Object> attrs = newAttrsList.get(i);
                fBreakpoints.insertBreakpoint(
                    context, attrs, 
                    new DataRequestMonitor<IBreakpointDMContext>(getExecutor(), countingRM) {
                        @Override
                        protected void handleCompleted() {
                        	if (isSuccess()) {
                        		bp.fTargetBPContext = getData();
                        	}
                        	bp.fStatus = getStatus();
                            countingRM.done();
                        }
                    });
                coutingRmCount++;
            } else if (bp.fAttributesDelta.size() == 0) { 
            	// Breakpoint attributes have not changed, only copy over the old status.
            	bp.fStatus = oldBpList.get(i).fStatus;
            } else if ( !fAttributeTranslator.canUpdateAttributes(bp.getTargetBreakpoint(), bp.fAttributesDelta) ) {
                // The attribute translator tells us that the debugger cannot modify the 
                // breakpoint to change the given attributes.  Remove the breakpoint
                // and insert a new one.
                RequestMonitor removeRm = new RequestMonitor(getExecutor(), countingRM) {
                    @Override
                    protected void handleCompleted() {
                    	if (isSuccess()) {
                    		bp.fTargetBPContext = null;
	                        fBreakpoints.insertBreakpoint(
	                            context, newAttrsList.get(i),
	                            new DataRequestMonitor<IBreakpointDMContext>(getExecutor(), countingRM) {
	                                @Override
	                                protected void handleCompleted() {
	                                    if (isSuccess()) {
	                                        bp.fTargetBPContext = getData();
	                                    } 
	                                    bp.fStatus = getStatus();
	                                    countingRM.done();
	                                }
	                            });
                    	} else {
                    		// Failed to remove old breakpoint, do not proceed to insert a new one
                    		// just save the error from target with the old context.
                            bp.fStatus = getStatus();
                            countingRM.done();
                    	}
                    }
                };

            	fBreakpoints.removeBreakpoint(bp.getTargetBreakpoint(), removeRm);
                coutingRmCount++;
            } else {
                // The back end can modify the breakpoint.  Update the breakpoint with the 
                // new attributes.
                fBreakpoints.updateBreakpoint(
                    bp.getTargetBreakpoint(), bp.fAttributes, 
                    new RequestMonitor(getExecutor(), countingRM) {
                        @Override
                        protected void handleCompleted() {
                            bp.fStatus = getStatus();
                            countingRM.done();
                        }
                    });
                coutingRmCount++;
            }
        }
        countingRM.setDoneCount(coutingRmCount);
	} 
	
	/**
	 * Determine the set of modified attributes
	 * 
	 * @param oldAttributes
	 * @param newAttributes
	 * @return
	 */
	private void calcBPsAttrs(List<TargetBP> oldBpList, List<TargetBP> bpList) {
	    // Go through the bp attributes common to the old and the new lists and calculate
	    // their deltas.
		int i = 0;
	    for (i = 0; i < oldBpList.size() && i < bpList.size(); i++) {
    		TargetBP newBp = bpList.get(i);
    		TargetBP oldBp = oldBpList.get(i);
    		newBp.fTargetBPContext = oldBp.getTargetBreakpoint();
	        Map<String, Object> oldAttributes = oldBp.fAttributes; 
	        Map<String, Object> newAttributes = newBp.fAttributes;
    	    
	        if (oldAttributes == null) {
	        	// Reached a point in the old BP list where breakpoints were 
	        	// removed.  Break out of the loop.
	        	break;
	        }
	        
    		bpList.get(i).fAttributesDelta = getAttributesDelta(oldAttributes, newAttributes);
	    } 
	    
	    // Add all the new attributes as deltas
	    for (; i < bpList.size(); i++) {
	    	TargetBP newBP = bpList.get(i); 
	        newBP.fAttributesDelta =  newBP.fAttributes;
	    }
	    
	    // For breakpoints that were removed create TargetBP entry with a 
	    // null set of attributes
	    for (; i < oldBpList.size(); i++) {
	    	TargetBP oldBp = oldBpList.get(i);
	    	if (oldBp.fAttributes == null) {
	    		// Guard against old removed breakpoints
	    		break;
	    	}
	    	TargetBP newBp = new TargetBP(null);
	    	newBp.fTargetBPContext = oldBp.getTargetBreakpoint();
	    	newBp.fAttributesDelta = oldBpList.get(i).fAttributes;
	    	bpList.add(newBp);
	    }	    
	}

    ///////////////////////////////////////////////////////////////////////////
    // IBreakpointManagerListener implementation
    ///////////////////////////////////////////////////////////////////////////

	public void breakpointManagerEnablementChanged(boolean enabled) {
		Map<String, Object> platformAttrDelta = new HashMap<String, Object>(1);
		platformAttrDelta.put(IBreakpoint.ENABLED, enabled);
		
		Map<IBreakpoint, Map<String, Object>> bp2DeltaMap = new HashMap<IBreakpoint, Map<String, Object>>(); 
		for (IBreakpoint bp : fBreakpointManager.getBreakpoints()) {
			if (! fAttributeTranslator.supportsBreakpoint(bp))
				continue;

			bp2DeltaMap.put(bp, platformAttrDelta);
		}
		
		doBreakpointsChanged(bp2DeltaMap);
	}

	@ThreadSafe
	public void breakpointsAdded(final IBreakpoint[] bps) {
		doBreakpointsAdded(bps, null, null);
	}
	
	@SuppressWarnings("unchecked")
	private void doBreakpointsAdded(final IBreakpoint[] bps, final IBreakpointsTargetDMContext bpsTargetDmc, final RequestMonitor rm) {
		final List<IBreakpoint> bpCandidates = new ArrayList<IBreakpoint>(bps.length);
		
		for (int i = 0; i < bps.length; i++) {
			IBreakpoint bp = bps[i];
			
			if (fAttributeTranslator.supportsBreakpoint(bp)) {
				try {
					if (fAttributeTranslator2 != null) {
						if (bp.getMarker() == null)
							continue;
						
						// if the breakpoint is not enabled, ask translator2 if it can set (and manage)
						// disabled breakpoint itself. If not, just bail out.
						//
						Map<String, Object> platformAttrs = bp.getMarker().getAttributes();
						
						if (! (Boolean)platformAttrs.get(IBreakpoint.ENABLED) || ! fBreakpointManager.isEnabled()) {
							Map<String, Object> platformAttr = new HashMap<String, Object>(1);
							platformAttr.put(IBreakpoint.ENABLED, Boolean.FALSE);
							Map<String, Object> targetAttr = fAttributeTranslator2.convertAttributeDelta(platformAttr);
							if (! fAttributeTranslator2.canUpdateAttributes(null, targetAttr)) {
								// bail out.
								continue;
							}
						}
					}
					
					bpCandidates.add(bp);
				} catch (CoreException e) {
					DsfPlugin.getDefault().getLog().log(e.getStatus());
				}
			}
		}
		
		// Nothing to do
		if (bpCandidates.isEmpty()) {
			if (rm != null) {
				rm.done();
			}
			return;
		}
				
		try {
            getExecutor().execute(new DsfRunnable() {
				public void run() {
					final Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> eventBPs =  
						new HashMap<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>>(bpCandidates.size(), 1);
					
	                CountingRequestMonitor processPendingCountingRm = new CountingRequestMonitor(getExecutor(), rm) {
		                    @Override
		                    protected void handleCompleted() {
		                    	processPendingRequests();
		                    	fireUpdateBreakpointsStatus(eventBPs, BreakpointEventType.ADDED);
		                        super.handleCompleted();
		                    }
		                };	            	
	                int processPendingCountingRmCount = 0;
	            	
	            	for (final IBreakpoint breakpoint : bpCandidates) {
	            		final Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]> targetBPs = 
	            			new HashMap<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>(fPlatformBPs.size(), 1);
	            		eventBPs.put(breakpoint, targetBPs);	
	            			
						if (fRunningEvents.contains(breakpoint)) {
							PendingEventInfo pendingEvent = new PendingEventInfo(BreakpointEventType.ADDED, bpsTargetDmc, processPendingCountingRm);
							processPendingCountingRmCount++;
							updatePendingRequest(breakpoint, pendingEvent);
							continue;
						}
		                // Mark the breakpoint as being updated and go
		                fRunningEvents.add(breakpoint);

	            		final CountingRequestMonitor bpTargetsCountingRm = new CountingRequestMonitor(getExecutor(), processPendingCountingRm) {
								@Override
								protected void handleCompleted() {
			                    	// Indicate that the running event has completed
			                    	fRunningEvents.remove(breakpoint);
			                    	super.handleCompleted();
								}
							};
						int bpTargetsCountingRmCount = 0;
						processPendingCountingRmCount++;
	
						
						// Install the breakpoint in all the execution contexts
						for (final IBreakpointsTargetDMContext dmc : fPlatformBPs.keySet()) {
							if (bpsTargetDmc != null && !bpsTargetDmc.equals(dmc)) {
								continue;
							}
							
			                // Now ask lower level to set the bp.
							//
							if (fAttributeTranslator2 != null) {
								fAttributeTranslator2.getTargetBreakpointAttributes(dmc, breakpoint, fBreakpointManager.isEnabled(),
										new DataRequestMonitor<List<Map<String,Object>>>(getExecutor(), bpTargetsCountingRm){
											@Override
											protected void handleSuccess() {
												installBreakpoint(
											    		dmc, breakpoint, getData(), 
											    		new DataRequestMonitor<List<TargetBP>>(getExecutor(), bpTargetsCountingRm) {
											    			@Override
															protected void handleSuccess() {
											    				targetBPs.put(dmc, getData().toArray(new ITargetBreakpointInfo[getData().size()]));
											    				super.handleSuccess();
											    			};
											    		});
											}});
							}
							else {	// Old way
								List<Map<String, Object>> attrsArray;
								try {
									attrsArray = fAttributeTranslator.getBreakpointAttributes(breakpoint, fBreakpointManager.isEnabled());
								} catch (CoreException e) {
									attrsArray = new ArrayList<Map<String, Object>>();
									DsfPlugin.getDefault().getLog().log(e.getStatus());
								}
				        		
								installBreakpoint(
						    		dmc, breakpoint, attrsArray, 
						    		new DataRequestMonitor<List<TargetBP>>(getExecutor(), bpTargetsCountingRm) {
						    			@Override
										protected void handleSuccess() {
						    				targetBPs.put(dmc, getData().toArray(new ITargetBreakpointInfo[getData().size()]));
						    				super.handleSuccess();
						    			};
						    		});
							}
							
							bpTargetsCountingRmCount++;
						}
						bpTargetsCountingRm.setDoneCount(bpTargetsCountingRmCount);
	            	}
	            	processPendingCountingRm.setDoneCount(processPendingCountingRmCount);	            	
				}
			});
		} catch (RejectedExecutionException e) {
			IStatus status = new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, IDsfStatusConstants.INTERNAL_ERROR, "Request for monitor: '" + toString() + "' resulted in a rejected execution exception.", e);//$NON-NLS-1$ //$NON-NLS-2$
			if (rm != null) {
				rm.setStatus(status);
				rm.done();
			} else {
				DsfPlugin.getDefault().getLog().log(status); 
			}
		}
	}
	
    ///////////////////////////////////////////////////////////////////////////
    // IBreakpointListener implementation
    ///////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked")
	public void breakpointsChanged(IBreakpoint[] bps, IMarkerDelta[] deltas) {
		Map<IBreakpoint, Map<String, Object>> bp2DeltaMap = new HashMap<IBreakpoint, Map<String, Object>>(); 
		for (int i = 0; i < bps.length; i++) {
			IBreakpoint bp = bps[i];
			
			if (deltas[i] == null)
				continue;
			
			if (bp.getMarker() == null)
				continue;
			
			if (! fAttributeTranslator.supportsBreakpoint(bp))
				continue;

			try {
				Map<String, Object> oldAttrs = deltas[i].getAttributes();
				Map<String, Object> newAttrs = bp.getMarker().getAttributes();
				
				Map<String, Object> platformAttrDelta = getAttributesDelta(oldAttrs, newAttrs);
				
				if (platformAttrDelta.size() == 0) // no change. possible when user cancels breakpoint properties dialog.
					continue;

				bp2DeltaMap.put(bp, platformAttrDelta);
			} catch (CoreException e) {
				DsfPlugin.getDefault().getLog().log(e.getStatus());
			}
		}
		
		doBreakpointsChanged(bp2DeltaMap);
	}
	
	/**
	 * 
	 * @param bp2DeltaMap - pairs of (breakpoint, attrDelta), where attrDelta contains changed 
	 * and new attributes for the breakpoint.  
	 */
	private void doBreakpointsChanged(Map<IBreakpoint, Map<String, Object>> bp2DeltaMap) {

		final Map<IBreakpoint, List<Map<String, Object>>> bpsAttrs = 
			new HashMap<IBreakpoint, List<Map<String, Object>>>(bp2DeltaMap.size() * 4/3);

		for (IBreakpoint bp : bp2DeltaMap.keySet()) {
			try {
				Map<String, Object> platformAttrDelta = bp2DeltaMap.get(bp);
				
				if (fAttributeTranslator2 != null) {
					Map<String, Object> targetAttrDelta = fAttributeTranslator2.convertAttributeDelta(platformAttrDelta);
		
					if (! fAttributeTranslator2.canUpdateAttributes(null, targetAttrDelta)) {
						// DSF client cannot handle at least one of the attribute change, just remove
						// old target BPs and install new ones.
						final IBreakpoint[] platformBPs = new IBreakpoint[] {bp};
						
						if (platformAttrDelta.containsKey(IBreakpoint.ENABLED)) {
							if ((Boolean)platformAttrDelta.get(IBreakpoint.ENABLED))
								// platform BP changed from disabled to enabled
								doBreakpointsAdded(platformBPs, null, null);
							else
								doBreakpointsRemoved(platformBPs, null, null);
						}
						else {
							// other attribute change, remove old and install new.
							doBreakpointsRemoved(platformBPs, null, new RequestMonitor(getExecutor(), null) {
								@Override
								protected void handleSuccess() {
									doBreakpointsAdded(platformBPs, null, null);
								}});
						}
					}
					else 
						updateBreakpoint(bp, targetAttrDelta);
				}
				else { // old way
					
	                // Retrieve the breakpoint attributes
	        		List<Map<String, Object>> attrsArray = 
	        		    fAttributeTranslator.getBreakpointAttributes(bp, fBreakpointManager.isEnabled());
	        		
	        		bpsAttrs.put(bp, attrsArray);
				}
			} catch (CoreException e) {
				DsfPlugin.getDefault().getLog().log(e.getStatus());
			}
		}
		
		if (bpsAttrs.isEmpty()) return; // nothing to do
		
		try {
			// Modify the breakpoint in all the target contexts
	        getExecutor().execute( new DsfRunnable() { 
	            public void run() {
					final Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> eventBPs =  
						new HashMap<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>>(bpsAttrs.size(), 1);

					CountingRequestMonitor processPendingCountingRm = new CountingRequestMonitor(getExecutor(), null) {
	                    @Override
	                    protected void handleCompleted() {
	                    	processPendingRequests();
	                    	fireUpdateBreakpointsStatus(eventBPs, BreakpointEventType.MODIFIED);
	                    }
	                };	            	
	                int processPendingCountingRmCount = 0;
	            	
	            	for (final IBreakpoint breakpoint : bpsAttrs.keySet()) {
	            		final Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]> targetBPs = 
	            			new HashMap<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>(fPlatformBPs.size(), 1);
	            		eventBPs.put(breakpoint, targetBPs);	

	            		// If the breakpoint is currently being updated, queue the request and exit
		            	if (fRunningEvents.contains(breakpoint)) {
		            		updatePendingRequest(breakpoint, new PendingEventInfo(BreakpointEventType.MODIFIED, null, null));
							continue;
		            	}
		            	
		                // Keep track of the updates
		                CountingRequestMonitor bpTargetsCountingRm = 
		                	new CountingRequestMonitor(getExecutor(), processPendingCountingRm) {
			                    @Override
			                    protected void handleCompleted() {
			                    	// Indicate that the running event has completed
			                    	fRunningEvents.remove(breakpoint);
			                    	super.handleCompleted();
			                    }
			                };
			            processPendingCountingRmCount++;
		                bpTargetsCountingRm.setDoneCount(fPlatformBPs.size());
	
		                // Mark the breakpoint as being updated and go
		                fRunningEvents.add(breakpoint);
		                
		                // Modify the breakpoint in all the execution contexts
		                for (final IBreakpointsTargetDMContext dmc : fPlatformBPs.keySet()) {
		                    modifyBreakpoint(
		                    		dmc, breakpoint, bpsAttrs.get(breakpoint), 
		                    		new DataRequestMonitor<List<TargetBP>>(getExecutor(), bpTargetsCountingRm) {
						    			@Override
										protected void handleSuccess() {
						    				targetBPs.put(dmc, getData().toArray(new ITargetBreakpointInfo[getData().size()]));
						    				super.handleSuccess();
						    			};
		                    		});
		                }
	            	}
	            	processPendingCountingRm.setDoneCount(processPendingCountingRmCount);
	            }
	        });
	    } catch (RejectedExecutionException e) {
			DsfPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, IDsfStatusConstants.INTERNAL_ERROR, "Request for monitor: '" + toString() + "' resulted in a rejected execution exception.", e)); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	}

	/**
	 * For the given platform BP, update all its target BPs with the given attribute change.
	 * 
	 * @param bp
	 * @param targetAttrDelta - target attribute change.  
	 */
	private void updateBreakpoint(final IBreakpoint bp, final Map<String, Object> targetAttrDelta) {
        getExecutor().execute(new DsfRunnable() {
        	public void run() {
				for (IBreakpointsTargetDMContext dmc : fPlatformBPs.keySet()) {
					List<TargetBP> targetBPs = fPlatformBPs.get(dmc).get(bp);
					if (targetBPs != null) {
						for (TargetBP tbp : targetBPs) {
							// this must be an installed bp.
							assert (tbp.getTargetBreakpoint() != null);
							
							fBreakpoints.updateBreakpoint(tbp.getTargetBreakpoint(), targetAttrDelta, new RequestMonitor(getExecutor(), null) {});
						}
					}
				}
        	}});
	}

	public void breakpointsRemoved(final IBreakpoint[] bps, IMarkerDelta delta[]) {
		doBreakpointsRemoved(bps, null, null);
	}
	
	public void doBreakpointsRemoved(final IBreakpoint[] bps, final IBreakpointsTargetDMContext bpsTargetDmc, final RequestMonitor rm) {
	
		final List<IBreakpoint> bpCandidates = new ArrayList<IBreakpoint>();
		
		for (int i = 0; i < bps.length; i++) {
			IBreakpoint bp = bps[i];
			
			if (fAttributeTranslator.supportsBreakpoint(bp)) {
				if (bpsTargetDmc == null)
					bpCandidates.add(bp);
				else if (fPlatformBPs.get(bpsTargetDmc).containsKey(bp))	// target BPs are installed in the context 
					bpCandidates.add(bp);
			}
		}
		
		if (bpCandidates.isEmpty()) { // nothing to do
			if (rm != null)
				rm.done();
			return;
		}
		
		try {
	        getExecutor().execute(new DsfRunnable() {
	        	public void run() {
					final Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> eventBPs =  
						new HashMap<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>>(bpCandidates.size(), 1);

					CountingRequestMonitor processPendingCountingRm = new CountingRequestMonitor(getExecutor(), rm) {
            			@Override
            			protected void handleCompleted() {
            				processPendingRequests();
	                    	fireUpdateBreakpointsStatus(eventBPs, BreakpointEventType.REMOVED);
            				super.handleCompleted();
            			}
            		};
            		int processPendingCountingRmCount = 0;
            		
					for (final IBreakpoint breakpoint : bpCandidates) {
	            		final Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]> targetBPs = 
	            			new HashMap<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>(fPlatformBPs.size(), 1);
	            		eventBPs.put(breakpoint, targetBPs);	
				
		            	// If the breakpoint is currently being updated, queue the request and exit
		            	if (fRunningEvents.contains(breakpoint)) {
		            		PendingEventInfo pendingEvent = new PendingEventInfo(BreakpointEventType.REMOVED, bpsTargetDmc, processPendingCountingRm);
            				processPendingCountingRmCount++;
		            		updatePendingRequest(breakpoint, pendingEvent);
							continue;
		            	}
		            	
	            		CountingRequestMonitor bpTargetDmcCRM = new CountingRequestMonitor(getExecutor(), processPendingCountingRm) {
							@Override
							protected void handleCompleted() {
		                    	// Indicate that the running event has completed
		                    	fRunningEvents.remove(breakpoint);
		                    	super.handleCompleted();
							}
						};
						processPendingCountingRmCount++;
						int bpTargetDmcCRMCount = 0;

	            		// Remove the breakpoint in all the execution contexts
	            		for (final IBreakpointsTargetDMContext dmc : fPlatformBPs.keySet()) {
	            			if (bpsTargetDmc != null && !bpsTargetDmc.equals(dmc)) {
	            				continue;
	            			}
	            			
	            			if (fPlatformBPs.get(dmc).containsKey(breakpoint)) {		// there are targetBPs installed 
	            				uninstallBreakpoint(
	            						dmc, breakpoint,
	            						new DataRequestMonitor<List<TargetBP>>(getExecutor(), bpTargetDmcCRM) {
							    			@Override
											protected void handleSuccess() {
							    				targetBPs.put(dmc, getData().toArray(new ITargetBreakpointInfo[getData().size()]));
							    				super.handleSuccess();
							    			};
	            						});
	            				bpTargetDmcCRMCount++;
	            			} else {
	            				// Breakpoint not installed for given context, do nothing.
	            			}
	            		}
	            		bpTargetDmcCRM.setDoneCount(bpTargetDmcCRMCount);
					}
					
					processPendingCountingRm.setDoneCount(processPendingCountingRmCount);
	        	}
	        });
        } catch (RejectedExecutionException e) {
			IStatus status = new Status(IStatus.ERROR, DsfPlugin.PLUGIN_ID, IDsfStatusConstants.INTERNAL_ERROR, "Request for monitor: '" + toString() + "' resulted in a rejected execution exception.", e);//$NON-NLS-1$ //$NON-NLS-2$
			if (rm != null) {
				rm.setStatus(status);
				rm.done();
			} else {
				DsfPlugin.getDefault().getLog().log(status); 
			}
        }
	}
	
	private void updatePendingRequest(IBreakpoint breakpoint, PendingEventInfo pendingEvent) {
		LinkedList<PendingEventInfo> pendingEventsList = fPendingEvents.get(breakpoint);
		if (pendingEventsList == null) {
			pendingEventsList = new LinkedList<PendingEventInfo>();
			fPendingEvents.put(breakpoint, pendingEventsList);
		}
		if (pendingEventsList.size() > 0 &&
				pendingEventsList.getLast().fEventType == BreakpointEventType.MODIFIED) {
			pendingEventsList.removeLast();
		}
		pendingEventsList.add(pendingEvent);
	}
	
	private void processPendingRequests() {
		if (fPendingEvents.isEmpty()) return;  // Nothing to do
		
		final List<PendingEventInfo> modifyBPs = new ArrayList<PendingEventInfo>(1);
		final Map<IBreakpointsTargetDMContext, List<PendingEventInfo>> addBPs =
			new HashMap<IBreakpointsTargetDMContext, List<PendingEventInfo>>(1);
		final Map<IBreakpointsTargetDMContext, List<PendingEventInfo>> removeBPs =
			new HashMap<IBreakpointsTargetDMContext, List<PendingEventInfo>>(1);
		
		// Make a copy to avoid java.util.ConcurrentModificationException.
		Set<IBreakpoint> bpsInPendingEvents = new HashSet<IBreakpoint>(fPendingEvents.keySet()); 
//		for (IBreakpoint bp : fPendingEvents.keySet()) {
		for (IBreakpoint bp : bpsInPendingEvents) {
	    	// Process the next pending update for this breakpoint
	    	if (!fRunningEvents.contains(bp)) {
	    		LinkedList<PendingEventInfo> eventInfoList = fPendingEvents.get(bp);
	    		PendingEventInfo eventInfo = eventInfoList.removeFirst();
	    		if (eventInfoList.isEmpty()) {
	    			fPendingEvents.remove(bp);
	    		}
	    		BreakpointEventType type = eventInfo.fEventType;
	    		if (type.equals(BreakpointEventType.MODIFIED)) {
	    			modifyBPs.add(eventInfo);
	    		} else if (type.equals(BreakpointEventType.ADDED)){
	    			List<PendingEventInfo> addList = addBPs.get(eventInfo.fBPsTargetDmc);
	    			if (addList == null) {
	    				addList = new ArrayList<PendingEventInfo>(1);
	    				addBPs.put(eventInfo.fBPsTargetDmc, addList);
	    			}
	    			addList.add(eventInfo);
	    		} else if (type.equals(BreakpointEventType.REMOVED)){
	    			List<PendingEventInfo> removeList = removeBPs.get(eventInfo.fBPsTargetDmc);
	    			if (removeList == null) {
	    				removeList = new ArrayList<PendingEventInfo>(1);
	    				removeBPs.put(eventInfo.fBPsTargetDmc, removeList);
	    			}
	    			removeList.add(eventInfo);
	    		}
	    	}
		}

		if (modifyBPs.size() != 0 || removeBPs.size() != 0) {
			new Job("Deferred breakpoint changed job") { //$NON-NLS-1$
	            { setSystem(true); }
	            @Override
	            protected IStatus run(IProgressMonitor monitor) {
	            	if (modifyBPs.size() != 0) {
	            		breakpointsChanged(modifyBPs.toArray(new IBreakpoint[modifyBPs.size()]), null);	            		
	            	}
	            	if (addBPs.size() != 0) {
	            		for (Map.Entry<IBreakpointsTargetDMContext, List<PendingEventInfo>> addBPsEntry : addBPs.entrySet()) {
	            			IBreakpointsTargetDMContext bpsTargetDmc = addBPsEntry.getKey();
	            			final List<PendingEventInfo> addBpList = addBPsEntry.getValue();
	            			RequestMonitor rm = new RequestMonitor(ImmediateExecutor.getInstance(), null) {
	            				@Override
								protected void handleCompleted() {
	            					for (PendingEventInfo eventInfo : addBpList) {
	            						if (eventInfo.fRequestMonitor != null) {
	            							eventInfo.fRequestMonitor.done();
	            						}
	            					}
	            				};
	            			};
		            		doBreakpointsAdded(addBpList.toArray(new IBreakpoint[addBpList.size()]), bpsTargetDmc, rm);	            			            			
	            		}
	            	} 
	            	if (removeBPs.size() != 0) {
	            		for (Map.Entry<IBreakpointsTargetDMContext, List<PendingEventInfo>> removeBPsEntry : removeBPs.entrySet()) {
	            			IBreakpointsTargetDMContext bpsTargetDmc = removeBPsEntry.getKey();
	            			final List<PendingEventInfo> removeBpList = removeBPsEntry.getValue();
	            			RequestMonitor rm = new RequestMonitor(ImmediateExecutor.getInstance(), null) {
	            				@Override
								protected void handleCompleted() {
	            					for (PendingEventInfo eventInfo : removeBpList) {
	            						if (eventInfo.fRequestMonitor != null) {
	            							eventInfo.fRequestMonitor.done();
	            						}
	            					}
	            				};
	            			};
		            		doBreakpointsRemoved(removeBpList.toArray(new IBreakpoint[removeBpList.size()]), bpsTargetDmc, rm);	            			            			
	            		}
	            	} 
	                return Status.OK_STATUS;
	            };
			}.schedule();
		}
	}
	
	private void fireUpdateBreakpointsStatus(final Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> eventBPs, final BreakpointEventType eventType) {
        // Update breakpoint status
        new Job("Breakpoint status update") { //$NON-NLS-1$
            { setSystem(true); }
            @Override
            protected IStatus run(IProgressMonitor monitor) {
            	for (IBreakpoint bp : eventBPs.keySet()) {
            		fAttributeTranslator.updateBreakpointStatus(bp);
            	}
                
                if (fAttributeTranslator2 != null) {
                	fAttributeTranslator2.updateBreakpointsStatus(eventBPs, eventType);
                }

                return Status.OK_STATUS;
            };
        }.schedule();
		
	}

    /**
     * Determine the set of modified attributes.
     * 
     * @param oldAttributes old map of attributes.
     * @param newAttributes new map of attributes.
     * @return new and changed attribute in the new map. May be empty indicating the two maps are equal.
     */
    private Map<String, Object> getAttributesDelta(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {

        Map<String, Object> delta = new HashMap<String,Object>();

        Set<String> oldKeySet = oldAttributes.keySet();
        Set<String> newKeySet = newAttributes.keySet();

        Set<String> commonKeys  = new HashSet<String>(newKeySet); commonKeys.retainAll(oldKeySet);
        Set<String> addedKeys   = new HashSet<String>(newKeySet); addedKeys.removeAll(oldKeySet);
        Set<String> removedKeys = new HashSet<String>(oldKeySet); removedKeys.removeAll(newKeySet);

        // Add the modified attributes
        for (String key : commonKeys) {
            if (!(oldAttributes.get(key).equals(newAttributes.get(key))))
                delta.put(key, newAttributes.get(key));
        }

        // Add the new attributes
        for (String key : addedKeys) {
            delta.put(key, newAttributes.get(key));
        }

        // Remove the deleted attributes
        for (String key : removedKeys) {
            delta.put(key, null);
        }

        return delta;
    }
}
