/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Ericsson	AB		  - Modified for handling of multiple threads
 *******************************************************************************/

package org.eclipse.cdt.dsf.gdb.service;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.AbstractDMEvent;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.datamodel.IDMEvent;
import org.eclipse.cdt.dsf.debug.service.ICachingService;
import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IProcessDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlShutdownDMEvent;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.mi.service.IMIContainerDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.IMIRunControl;
import org.eclipse.cdt.dsf.mi.service.MIRunControl;
import org.eclipse.cdt.dsf.mi.service.MIStack;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIBreakDelete;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIBreakInsert;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecContinue;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecFinish;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecInterrupt;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecNext;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecNextInstruction;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecStep;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIExecStepInstruction;
import org.eclipse.cdt.dsf.mi.service.command.events.IMIDMEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIBreakpointHitEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIErrorEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIInferiorExitEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIRunningEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MISharedLibEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MISignalEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MISteppingRangeEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIStoppedEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIThreadCreatedEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIThreadExitEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIWatchpointTriggerEvent;
import org.eclipse.cdt.dsf.mi.service.command.output.MIBreakInsertInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * Implementation note: This class implements event handlers for the events that
 * are generated by this service itself. When the event is dispatched, these
 * handlers will be called first, before any of the clients. These handlers
 * update the service's internal state information to make them consistent with
 * the events being issued. Doing this in the handlers as opposed to when the
 * events are generated, guarantees that the state of the service will always be
 * consistent with the events. The purpose of this pattern is to allow clients
 * that listen to service events and track service state, to be perfectly in
 * sync with the service state.
 * @since 1.1
 */
public class GDBRunControl_7_0_NS extends AbstractDsfService implements IMIRunControl, ICachingService
{
	@Immutable
	private static class ExecutionData implements IExecutionDMData {
		private final StateChangeReason fReason;
		ExecutionData(StateChangeReason reason) {
			fReason = reason;
		}
		public StateChangeReason getStateChangeReason() { return fReason; }
	}

	/**
	 * Base class for events generated by the MI Run Control service.  Most events
	 * generated by the MI Run Control service are directly caused by some MI event.
	 * Other services may need access to the extended MI data carried in the event.
	 * 
	 * @param <V> DMC that this event refers to
	 * @param <T> MIInfo object that is the direct cause of this event
	 * @see MIRunControl
	 */
	@Immutable
	private static class RunControlEvent<V extends IDMContext, T extends MIEvent<? extends IDMContext>> extends AbstractDMEvent<V>
	implements IDMEvent<V>, IMIDMEvent
	{
		final private T fMIInfo;
		public RunControlEvent(V dmc, T miInfo) {
			super(dmc);
			fMIInfo = miInfo;
		}

		public T getMIEvent() { return fMIInfo; }
	}

	/**
	 * Indicates that the given thread has been suspended.
	 */
	@Immutable
	private static class SuspendedEvent extends RunControlEvent<IExecutionDMContext, MIStoppedEvent>
	implements ISuspendedDMEvent
	{
		SuspendedEvent(IExecutionDMContext ctx, MIStoppedEvent miInfo) {
			super(ctx, miInfo);
		}

		public StateChangeReason getReason() {
			if (getMIEvent() instanceof MIBreakpointHitEvent) {
				return StateChangeReason.BREAKPOINT;
			} else if (getMIEvent() instanceof MISteppingRangeEvent) {
				return StateChangeReason.STEP;
			} else if (getMIEvent() instanceof MISharedLibEvent) {
				return StateChangeReason.SHAREDLIB;
			}else if (getMIEvent() instanceof MISignalEvent) {
				return StateChangeReason.SIGNAL;
			}else if (getMIEvent() instanceof MIWatchpointTriggerEvent) {
				return StateChangeReason.WATCHPOINT;
			}else if (getMIEvent() instanceof MIErrorEvent) {
				return StateChangeReason.ERROR;
			}else {
				return StateChangeReason.USER_REQUEST;
			}
		}
	}

	@Immutable
	private static class ResumedEvent extends RunControlEvent<IExecutionDMContext, MIRunningEvent>
	implements IResumedDMEvent
	{
		ResumedEvent(IExecutionDMContext ctx, MIRunningEvent miInfo) {
			super(ctx, miInfo);
		}

		public StateChangeReason getReason() {
			switch(getMIEvent().getType()) {
			case MIRunningEvent.CONTINUE:
				return StateChangeReason.USER_REQUEST;
			case MIRunningEvent.NEXT:
			case MIRunningEvent.NEXTI:
				return StateChangeReason.STEP;
			case MIRunningEvent.STEP:
			case MIRunningEvent.STEPI:
				return StateChangeReason.STEP;
			case MIRunningEvent.FINISH:
				return StateChangeReason.STEP;
			case MIRunningEvent.UNTIL:
			case MIRunningEvent.RETURN:
				break;
			}
			return StateChangeReason.UNKNOWN;
		}
	}

	@Immutable
	private static class StartedDMEvent extends RunControlEvent<IExecutionDMContext,MIThreadCreatedEvent>
	implements IStartedDMEvent
	{
		StartedDMEvent(IMIExecutionDMContext executionDmc, MIThreadCreatedEvent miInfo) {
			super(executionDmc, miInfo);
		}
	}

	@Immutable
	private static class ExitedDMEvent extends RunControlEvent<IExecutionDMContext,MIThreadExitEvent>
	implements IExitedDMEvent
	{
		ExitedDMEvent(IMIExecutionDMContext executionDmc, MIThreadExitEvent miInfo) {
			super(executionDmc, miInfo);
		}
	}

	protected class MIThreadRunState {
		// State flags
		boolean fSuspended = false;
		boolean fResumePending = false;
		boolean fStepping = false;
		StateChangeReason fStateChangeReason;
	}

	private static class RunToLineActiveOperation {
		private IMIExecutionDMContext fThreadContext;
		private int fBpId;
		private String fFileLocation;
		private String fAddrLocation;
		private boolean fSkipBreakpoints;
		
		public RunToLineActiveOperation(IMIExecutionDMContext threadContext,
				int bpId, String fileLoc, String addr, boolean skipBreakpoints) {
			fThreadContext = threadContext;
			fBpId = bpId;
			fFileLocation = fileLoc;
			fAddrLocation = addr;
			fSkipBreakpoints = skipBreakpoints;
		}
		
		public IMIExecutionDMContext getThreadContext() { return fThreadContext; }
		public int getBreakointId() { return fBpId; }
		public String getFileLocation() { return fFileLocation; }
		public String getAddrLocation() { return fAddrLocation; }
		public boolean shouldSkipBreakpoints() { return fSkipBreakpoints; }
	}

	///////////////////////////////////////////////////////////////////////////
	// MIRunControlNS
	///////////////////////////////////////////////////////////////////////////

	private ICommandControlService fConnection;

	private boolean fTerminated = false;

	// ThreadStates indexed by the execution context
	protected Map<IMIExecutionDMContext, MIThreadRunState> fThreadRunStates = new HashMap<IMIExecutionDMContext, MIThreadRunState>();

	private RunToLineActiveOperation fRunToLineActiveOperation = null;

	///////////////////////////////////////////////////////////////////////////
	// Initialization and shutdown
	///////////////////////////////////////////////////////////////////////////

	public GDBRunControl_7_0_NS(DsfSession session) {
		super(session);
	}

	@Override
	public void initialize(final RequestMonitor rm) {
		super.initialize(new RequestMonitor(getExecutor(), rm) {
			@Override
			protected void handleSuccess() {
				doInitialize(rm);
			}
		});
	}

	private void doInitialize(final RequestMonitor rm) {
        register(new String[]{IRunControl.class.getName(), IMIRunControl.class.getName()}, new Hashtable<String,String>());
		fConnection = getServicesTracker().getService(ICommandControlService.class);
		getSession().addServiceEventListener(this, null);
		rm.done();
	}

	@Override
	public void shutdown(final RequestMonitor rm) {
        unregister();
		getSession().removeServiceEventListener(this);
		super.shutdown(rm);
	}

	///////////////////////////////////////////////////////////////////////////
	// AbstractDsfService
	///////////////////////////////////////////////////////////////////////////

	@Override
	protected BundleContext getBundleContext() {
		return GdbPlugin.getBundleContext();
	}

	///////////////////////////////////////////////////////////////////////////
	// IRunControl
	///////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// Suspend
	// ------------------------------------------------------------------------

	public boolean isSuspended(IExecutionDMContext context) {

		// Thread case
		if (context instanceof IMIExecutionDMContext) {
			MIThreadRunState threadState = fThreadRunStates.get(context);
			return (threadState == null) ? false : !fTerminated && threadState.fSuspended;
		}

		// Container case
		if (context instanceof IContainerDMContext) {
			boolean isSuspended = false;
			for (IMIExecutionDMContext threadContext : fThreadRunStates.keySet()) {
				if (DMContexts.isAncestorOf(threadContext, context)) {
					isSuspended |= isSuspended(threadContext);
				}
			}
			return isSuspended;
		}

		// Default case
		return false;
	}

	public void canSuspend(IExecutionDMContext context, DataRequestMonitor<Boolean> rm) {

		// Thread case
		if (context instanceof IMIExecutionDMContext) {
			rm.setData(doCanSuspend(context));
			rm.done();
			return;
		}

		// Container case
		if (context instanceof IContainerDMContext) {
			boolean canSuspend = false;
			for (IMIExecutionDMContext threadContext : fThreadRunStates.keySet()) {
				if (DMContexts.isAncestorOf(threadContext, context)) {
					canSuspend |= doCanSuspend(threadContext);
				}
			}
			rm.setData(canSuspend);
			rm.done();
			return;
		}

		// Default case
		rm.setData(false);
		rm.done();
	}

	private boolean doCanSuspend(IExecutionDMContext context) {
		MIThreadRunState threadState = fThreadRunStates.get(context);
		return (threadState == null) ? false : !fTerminated && !threadState.fSuspended;
	}

	public void suspend(IExecutionDMContext context, final RequestMonitor rm) {

		assert context != null;

		// Thread case
		IMIExecutionDMContext thread = DMContexts.getAncestorOfType(context, IMIExecutionDMContext.class);
		if (thread != null) {
			doSuspendThread(thread, rm);
			return;
		}

		// Container case
		IMIContainerDMContext container = DMContexts.getAncestorOfType(context, IMIContainerDMContext.class);
		if (container != null) {
			doSuspendContainer(container, rm);
			return;
		}

		// Default case
		rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED, "Invalid context type.", null)); //$NON-NLS-1$
		rm.done();
	}

	private void doSuspendThread(IMIExecutionDMContext context, final RequestMonitor rm) {

		if (!doCanSuspend(context)) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED,
				"Given context: " + context + ", is already suspended.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		MIExecInterrupt cmd = new MIExecInterrupt(context);
		fConnection.queueCommand(cmd, new DataRequestMonitor<MIInfo>(getExecutor(), rm));
	}

	private void doSuspendContainer(IMIContainerDMContext context, final RequestMonitor rm) {
		String groupId = context.getGroupId();
		MIExecInterrupt cmd = new MIExecInterrupt(context, groupId);
		fConnection.queueCommand(cmd, new DataRequestMonitor<MIInfo>(getExecutor(), rm));
	}

	// ------------------------------------------------------------------------
	// Resume
	// ------------------------------------------------------------------------

	public void canResume(IExecutionDMContext context, DataRequestMonitor<Boolean> rm) {

		// Thread case
		if (context instanceof IMIExecutionDMContext) {
			rm.setData(doCanResume(context));
			rm.done();
			return;
		}

		// Container case
		if (context instanceof IContainerDMContext) {
			boolean canSuspend = false;
			for (IMIExecutionDMContext threadContext : fThreadRunStates.keySet()) {
				if (DMContexts.isAncestorOf(threadContext, context)) {
					canSuspend |= doCanResume(threadContext);
				}
			}
			rm.setData(canSuspend);
			rm.done();
			return;
		}

		// Default case
		rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED, "Invalid context type.", null)); //$NON-NLS-1$
		rm.done();
	}

	private boolean doCanResume(IExecutionDMContext context) {
		MIThreadRunState threadState = fThreadRunStates.get(context);
		return (threadState == null) ? false : !fTerminated && threadState.fSuspended && !threadState.fResumePending;
	}

	public void resume(IExecutionDMContext context, final RequestMonitor rm) {

		assert context != null;

		// Thread case
		IMIExecutionDMContext thread = DMContexts.getAncestorOfType(context, IMIExecutionDMContext.class);
		if (thread != null) {
			doResumeThread(thread, rm);
			return;
		}

		// Container case
		IMIContainerDMContext container = DMContexts.getAncestorOfType(context, IMIContainerDMContext.class);
		if (container != null) {
			doResumeContainer(container, rm);
			return;
		}

		// Default case
		rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED, "Invalid context type.", null)); //$NON-NLS-1$
		rm.done();
	}

	private void doResumeThread(IMIExecutionDMContext context, final RequestMonitor rm) {

		if (!doCanResume(context)) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Given context: " + context + ", is already running.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		final MIThreadRunState threadState = fThreadRunStates.get(context);
		if (threadState == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Given context: " + context + " is not an MI execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}
		
		threadState.fResumePending = true;
		MIExecContinue cmd = new MIExecContinue(context);
		fConnection.queueCommand(cmd, new DataRequestMonitor<MIInfo>(getExecutor(), rm) {
			@Override
			protected void handleFailure() {
				threadState.fResumePending = false;
				super.handleFailure();
			}
		});
	}

	private void doResumeContainer(IMIContainerDMContext context, final RequestMonitor rm) {
		String groupId = context.getGroupId();
		MIExecContinue cmd = new MIExecContinue(context, groupId);
		fConnection.queueCommand(cmd, new DataRequestMonitor<MIInfo>(getExecutor(), rm));
	}

	// ------------------------------------------------------------------------
	// Step
	// ------------------------------------------------------------------------

	public boolean isStepping(IExecutionDMContext context) {

		// If it's a thread, just look it up
		if (context instanceof IMIExecutionDMContext) {
			MIThreadRunState threadState = fThreadRunStates.get(context);
			return (threadState == null) ? false : !fTerminated && threadState.fStepping;
		}

		// Default case
		return false;
	}

	public void canStep(final IExecutionDMContext context, StepType stepType, final DataRequestMonitor<Boolean> rm) {

		// If it's a thread, just look it up
		if (context instanceof IMIExecutionDMContext) {
	    	if (stepType == StepType.STEP_RETURN) {
	    		// A step return will always be done in the top stack frame.
	    		// If the top stack frame is the only stack frame, it does not make sense
	    		// to do a step return since GDB will reject it.
	            MIStack stackService = getServicesTracker().getService(MIStack.class);
	            if (stackService != null) {
	            	// Check that the stack is at least two deep.
	            	stackService.getStackDepth(context, 2, new DataRequestMonitor<Integer>(getExecutor(), rm) {
	            		@Override
	            		public void handleCompleted() {
	            			if (isSuccess() && getData() == 1) {
	            				rm.setData(false);
	            				rm.done();
	            			} else {
	            	    		canResume(context, rm);
	            			}
	            		}
	            	});
	            	return;
	            }
	    	}

			canResume(context, rm);
			return;
		}

		// If it's a container, then we don't want to step it
		rm.setData(false);
		rm.done();
	}

	public void step(IExecutionDMContext context, StepType stepType, final RequestMonitor rm) {

		assert context != null;

		IMIExecutionDMContext dmc = DMContexts.getAncestorOfType(context, IMIExecutionDMContext.class);
		if (dmc == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED,
				"Given context: " + context + " is not an MI execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		if (!doCanResume(context)) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Cannot resume context", null)); //$NON-NLS-1$
			rm.done();
			return;
		}

		final MIThreadRunState threadState = fThreadRunStates.get(context);
		if (threadState == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Given context: " + context + " can't be found.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		MICommand<MIInfo> cmd = null;
		switch (stepType) {
		case STEP_INTO:
			cmd = new MIExecStep(dmc);
			break;
		case STEP_OVER:
			cmd = new MIExecNext(dmc);
			break;
		case STEP_RETURN:
			// The -exec-finish command operates on the selected stack frame, but here we always
			// want it to operate on the stop stack frame. So we manually create a top-frame
			// context to use with the MI command.
			// We get a local instance of the stack service because the stack service can be shut
			// down before the run control service is shut down. So it is possible for the
			// getService() request below to return null.
			MIStack stackService = getServicesTracker().getService(MIStack.class);
			if (stackService != null) {
				IFrameDMContext topFrameDmc = stackService.createFrameDMContext(dmc, 0);
				cmd = new MIExecFinish(topFrameDmc);
			} else {
				rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED,
						"Cannot create context for command, stack service not available.", null)); //$NON-NLS-1$
				rm.done();
				return;
			}
			break;
		case INSTRUCTION_STEP_INTO:
			cmd = new MIExecStepInstruction(dmc);
			break;
		case INSTRUCTION_STEP_OVER:
			cmd = new MIExecNextInstruction(dmc);
			break;
		default:
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID,
					INTERNAL_ERROR, "Given step type not supported", null)); //$NON-NLS-1$
			rm.done();
			return;
		}
		
		threadState.fResumePending = true;
		threadState.fStepping = true;
		fConnection.queueCommand(cmd, new DataRequestMonitor<MIInfo>(getExecutor(), rm) {
			@Override
			public void handleFailure() {
				threadState.fResumePending = false;
				threadState.fStepping = false;

				super.handleFailure();
			}   
		});
	}

	// ------------------------------------------------------------------------
	// Run to line
	// ------------------------------------------------------------------------

	public void runToLine(IExecutionDMContext context, String fileName, String lineNo, final boolean skipBreakpoints, final DataRequestMonitor<MIInfo> rm) {

		assert context != null;

		final IMIExecutionDMContext dmc = DMContexts.getAncestorOfType(context, IMIExecutionDMContext.class);
		if (dmc == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, NOT_SUPPORTED,
				"Given context: " + context + " is not an MI execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		if (!doCanResume(context)) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Cannot resume context", null)); //$NON-NLS-1$
			rm.done();
			return;
		}

		MIThreadRunState threadState = fThreadRunStates.get(context);
		if (threadState == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_STATE,
				"Given context: " + context + " is not an MI execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		final String fileLocation = fileName + ":" + lineNo; //$NON-NLS-1$
    	IBreakpointsTargetDMContext bpDmc = DMContexts.getAncestorOfType(context, IBreakpointsTargetDMContext.class);
    	fConnection.queueCommand(
    			new MIBreakInsert(bpDmc, true, false, null, 0, 
    					          fileLocation, dmc.getThreadId()), 
    		    new DataRequestMonitor<MIBreakInsertInfo>(getExecutor(), rm) {
    				@Override
    				public void handleSuccess() {
    					// We must set are RunToLineActiveOperation *before* we do the resume
    					// or else we may get the stopped event, before we have set this variable.
       					int bpId = getData().getMIBreakpoints()[0].getNumber();
       					String addr = getData().getMIBreakpoints()[0].getAddress();
    		        	fRunToLineActiveOperation = new RunToLineActiveOperation(dmc, bpId, fileLocation, addr, skipBreakpoints);

    					resume(dmc, new RequestMonitor(getExecutor(), rm) {
            				@Override
            				public void handleFailure() {
            		    		IBreakpointsTargetDMContext bpDmc = DMContexts.getAncestorOfType(fRunToLineActiveOperation.getThreadContext(),
            		    				IBreakpointsTargetDMContext.class);
            		    		int bpId = fRunToLineActiveOperation.getBreakointId();

            		    		fConnection.queueCommand(new MIBreakDelete(bpDmc, new int[] {bpId}),
            		    				new DataRequestMonitor<MIInfo>(getExecutor(), null));
            		    		fRunToLineActiveOperation = null;

            		    		super.handleFailure();
            		    	}
    					});
    				}
    			});

	}

	// ------------------------------------------------------------------------
	// Support functions
	// ------------------------------------------------------------------------

	public void getExecutionContexts(final IContainerDMContext containerDmc, final DataRequestMonitor<IExecutionDMContext[]> rm) {
        IMIProcesses procService = getServicesTracker().getService(IMIProcesses.class);
		procService.getProcessesBeingDebugged(
				containerDmc,
				new DataRequestMonitor<IDMContext[]>(getExecutor(), rm) {
					@Override
					protected void handleSuccess() {
						if (getData() instanceof IExecutionDMContext[]) {
							rm.setData((IExecutionDMContext[])getData());
						} else {
							rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INTERNAL_ERROR, "Invalid contexts", null)); //$NON-NLS-1$
						}
						rm.done();
					}
				});
	}

	public void getExecutionData(IExecutionDMContext dmc, DataRequestMonitor<IExecutionDMData> rm) {
		MIThreadRunState threadState = fThreadRunStates.get(dmc);
		if (threadState == null) {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID,INVALID_HANDLE,
				"Given context: " + dmc + " is not a recognized execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
			rm.done();
			return;
		}

		if (dmc instanceof IMIExecutionDMContext) {
			rm.setData(new ExecutionData(threadState.fSuspended ? threadState.fStateChangeReason : null));
		} else {
			rm.setStatus(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, INVALID_HANDLE,
				"Given context: " + dmc + " is not a recognized execution context.", null)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		rm.done();
	}


	private IMIExecutionDMContext createMIExecutionContext(IContainerDMContext container, String threadId) {
        IMIProcesses procService = getServicesTracker().getService(IMIProcesses.class);

        IProcessDMContext procDmc = DMContexts.getAncestorOfType(container, IProcessDMContext.class);
        
        IThreadDMContext threadDmc = null;
        if (procDmc != null) {
        	// For now, reuse the threadId as the OSThreadId
        	threadDmc = procService.createThreadContext(procDmc, threadId);
        }

        return procService.createExecutionContext(container, threadDmc, threadId);
	}

	private void updateThreadState(IMIExecutionDMContext context, ResumedEvent event) {
		StateChangeReason reason = event.getReason();
		boolean isStepping = reason.equals(StateChangeReason.STEP);
		MIThreadRunState threadState = fThreadRunStates.get(context);
		if (threadState == null) {
			threadState = new MIThreadRunState();
			fThreadRunStates.put(context, threadState);
		}
		threadState.fSuspended = false;
		threadState.fResumePending = false;
		threadState.fStateChangeReason = reason;
		threadState.fStepping = isStepping;
	}

	private void updateThreadState(IMIExecutionDMContext context, SuspendedEvent event) {
		StateChangeReason reason = event.getReason();
		MIThreadRunState threadState = fThreadRunStates.get(context);
		if (threadState == null) {
			threadState = new MIThreadRunState();
			fThreadRunStates.put(context, threadState);
		}
		threadState.fSuspended = true;
		threadState.fResumePending = false;
		threadState.fStepping = false;
		threadState.fStateChangeReason = reason;
	}

	///////////////////////////////////////////////////////////////////////////
	// Event handlers
	///////////////////////////////////////////////////////////////////////////

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(final MIRunningEvent e) {
        getSession().dispatchEvent(new ResumedEvent(e.getDMContext(), e), getProperties());
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(final MIStoppedEvent e) {
    	if (fRunToLineActiveOperation != null) {
    		// First check if it is the right thread that stopped
    		IMIExecutionDMContext threadDmc = DMContexts.getAncestorOfType(e.getDMContext(), IMIExecutionDMContext.class);
    		if (fRunToLineActiveOperation.getThreadContext().equals(threadDmc)) {
        		int bpId = 0;
        		if (e instanceof MIBreakpointHitEvent) {
        			bpId = ((MIBreakpointHitEvent)e).getNumber();
        		}
    			String fileLocation = e.getFrame().getFile() + ":" + e.getFrame().getLine();  //$NON-NLS-1$
    			String addrLocation = e.getFrame().getAddress();
    			// Here we check three different things to see if we are stopped at the right place
    			// 1- The actual location in the file.  But this does not work for breakpoints that
    			//    were set on non-executable lines
    			// 2- The address where the breakpoint was set.  But this does not work for breakpoints
    			//    that have multiple addresses (GDB returns <MULTIPLE>.)  I think that is for multi-process
    			// 3- The breakpoint id that was hit.  But this does not work if another breakpoint
    			//    was also set on the same line because GDB may return that breakpoint as being hit.
    			//
    			// So this works for the large majority of cases.  The case that won't work is when the user
    			// does a runToLine to a line that is non-executable AND has another breakpoint AND
    			// has multiple addresses for the breakpoint.  I'm mean, come on!
    			if (fileLocation.equals(fRunToLineActiveOperation.getFileLocation()) ||
    				addrLocation.equals(fRunToLineActiveOperation.getAddrLocation()) ||
    				bpId == fRunToLineActiveOperation.getBreakointId()) {
        			// We stopped at the right place.  All is well.
    				fRunToLineActiveOperation = null;
    			} else {
    				// The right thread stopped but not at the right place yet
    				if (fRunToLineActiveOperation.shouldSkipBreakpoints() && e instanceof MIBreakpointHitEvent) {
    					fConnection.queueCommand(
    							new MIExecContinue(fRunToLineActiveOperation.getThreadContext()),
    							new DataRequestMonitor<MIInfo>(getExecutor(), null));

    					// Don't send the stop event since we are resuming again.
    					return;
    				} else {
    					// Stopped for any other reasons.  Just remove our temporary one
    					// since we don't want it to hit later
    					IBreakpointsTargetDMContext bpDmc = DMContexts.getAncestorOfType(fRunToLineActiveOperation.getThreadContext(),
    							IBreakpointsTargetDMContext.class);

    					fConnection.queueCommand(new MIBreakDelete(bpDmc, new int[] {fRunToLineActiveOperation.getBreakointId()}),
    							new DataRequestMonitor<MIInfo>(getExecutor(), null));
    					fRunToLineActiveOperation = null;
    				}
    			}
    		}
    	}
        getSession().dispatchEvent(new SuspendedEvent(e.getDMContext(), e), getProperties());
	}


    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(final MIThreadCreatedEvent e) {
		IContainerDMContext containerDmc = e.getDMContext();
		IMIExecutionDMContext executionCtx = null;
		if (e.getStrId() != null) {
			executionCtx = createMIExecutionContext(containerDmc, e.getStrId());
		}
		getSession().dispatchEvent(new StartedDMEvent(executionCtx, e),	getProperties());
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(final MIThreadExitEvent e) {
		IContainerDMContext containerDmc = e.getDMContext();
		IMIExecutionDMContext executionCtx = null;
		if (e.getStrId() != null) {
			executionCtx = createMIExecutionContext(containerDmc, e.getStrId());
		}
		getSession().dispatchEvent(new ExitedDMEvent(executionCtx, e), getProperties());
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(ResumedEvent e) {
		IExecutionDMContext ctx = e.getDMContext();
		if (ctx instanceof IMIExecutionDMContext) {			
			updateThreadState((IMIExecutionDMContext)ctx, e);
		}
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(SuspendedEvent e) {
		IExecutionDMContext ctx = e.getDMContext();
		if (ctx instanceof IMIExecutionDMContext) {			
			updateThreadState((IMIExecutionDMContext)ctx, e);
		}
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(StartedDMEvent e) {
		IExecutionDMContext executionCtx = e.getDMContext();
		if (executionCtx instanceof IMIExecutionDMContext) {			
			if (fThreadRunStates.get(executionCtx) == null) {
				fThreadRunStates.put((IMIExecutionDMContext)executionCtx, new MIThreadRunState());
			}
		}
	}

    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(ExitedDMEvent e) {
		fThreadRunStates.remove(e.getDMContext());
	}
	
    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     */
	@DsfServiceEventHandler
	public void eventDispatched(ICommandControlShutdownDMEvent e) {
		fTerminated = true;
	}


    /**
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     * @noreference This method is not intended to be referenced by clients.
     * 
     * @since 2.0
     */
    @DsfServiceEventHandler
    public void eventDispatched(MIInferiorExitEvent e) {
    	if (fRunToLineActiveOperation != null) {
    		IBreakpointsTargetDMContext bpDmc = DMContexts.getAncestorOfType(fRunToLineActiveOperation.getThreadContext(),
    				IBreakpointsTargetDMContext.class);
    		int bpId = fRunToLineActiveOperation.getBreakointId();

    		fConnection.queueCommand(new MIBreakDelete(bpDmc, new int[] {bpId}),
    				new DataRequestMonitor<MIInfo>(getExecutor(), null));
    		fRunToLineActiveOperation = null;
    	}
    }

	public void flushCache(IDMContext context) {
	}

}
