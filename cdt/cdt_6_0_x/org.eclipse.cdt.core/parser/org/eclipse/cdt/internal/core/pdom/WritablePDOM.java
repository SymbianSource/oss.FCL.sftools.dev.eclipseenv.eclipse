/*******************************************************************************
 * Copyright (c) 2006, 2009 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *    Andrew Ferguson (Symbian)
 *******************************************************************************/ 
package org.eclipse.cdt.internal.core.pdom;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.index.IIndexFileLocation;
import org.eclipse.cdt.core.index.IIndexLocationConverter;
import org.eclipse.cdt.internal.core.index.IIndexFragment;
import org.eclipse.cdt.internal.core.index.IIndexFragmentFile;
import org.eclipse.cdt.internal.core.index.IWritableIndexFragment;
import org.eclipse.cdt.internal.core.index.IWritableIndex.IncludeInformation;
import org.eclipse.cdt.internal.core.pdom.db.ChunkCache;
import org.eclipse.cdt.internal.core.pdom.db.DBProperties;
import org.eclipse.cdt.internal.core.pdom.db.IBTreeVisitor;
import org.eclipse.cdt.internal.core.pdom.dom.IPDOMLinkageFactory;
import org.eclipse.cdt.internal.core.pdom.dom.PDOMFile;
import org.eclipse.core.runtime.CoreException;

public class WritablePDOM extends PDOM implements IWritableIndexFragment {	
	private boolean fClearedBecauseOfVersionMismatch= false;
	private boolean fCreatedFromScratch= false;
	private ASTFilePathResolver fPathResolver;

	public WritablePDOM(File dbPath, IIndexLocationConverter locationConverter, Map<String, IPDOMLinkageFactory> linkageFactoryMappings) throws CoreException {
		this(dbPath, locationConverter, ChunkCache.getSharedInstance(), linkageFactoryMappings);
	}
	
	public WritablePDOM(File dbPath, IIndexLocationConverter locationConverter, ChunkCache cache, Map<String, IPDOMLinkageFactory> linkageFactoryMappings) throws CoreException {
		super(dbPath, locationConverter, cache, linkageFactoryMappings);
	}
	
	public void setASTFilePathResolver(ASTFilePathResolver resolver) {
		fPathResolver= resolver;
	}

	@Override
	public IIndexFragmentFile addFile(int linkageID, IIndexFileLocation location) throws CoreException {
		return super.addFile(linkageID, location);
	}

	public void addFileContent(IIndexFragmentFile sourceFile, IncludeInformation[] includes, 
			IASTPreprocessorStatement[] macros, IASTName[][] names, ASTFilePathResolver pathResolver) throws CoreException {
		assert sourceFile.getIndexFragment() == this;
		
		PDOMFile pdomFile = (PDOMFile) sourceFile;
		pdomFile.addIncludesTo(includes);
		pdomFile.addMacros(macros);
		final ASTFilePathResolver origResolver= fPathResolver;
		fPathResolver= pathResolver;
		try {
			pdomFile.addNames(names);
		}
		finally {
			fPathResolver= origResolver;
		}
		
		final IIndexFileLocation location = pdomFile.getLocation();
		fEvent.fClearedFiles.remove(location);
		fEvent.fFilesWritten.add(location);
	}

	public void clearFile(IIndexFragmentFile file, Collection<IIndexFileLocation> contextsRemoved) throws CoreException {
		assert file.getIndexFragment() == this;
		((PDOMFile) file).clear(contextsRemoved);	
		
		fEvent.fClearedFiles.add(file.getLocation());
	}
	
	@Override
	public void clear() throws CoreException {
		super.clear();
	}
	
	@Override
	public void flush() throws CoreException {
		super.flush();
	}
		
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.internal.core.index.IWritableIndexFragment#setProperty(java.lang.String, java.lang.String)
	 */
	public void setProperty(String propertyName, String value) throws CoreException {
		if(IIndexFragment.PROPERTY_FRAGMENT_FORMAT_ID.equals(propertyName) 
		|| IIndexFragment.PROPERTY_FRAGMENT_FORMAT_VERSION.equals(propertyName)) {
			throw new IllegalArgumentException("Property "+value+" may not be written to"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		new DBProperties(db, PROPERTIES).setProperty(propertyName, value);
	}
	
	/**
	 * Use the specified location converter to update each internal representation of a file location.
	 * The file index is rebuilt with the new representations. Individual PDOMFile records are unmoved so
	 * as to maintain referential integrity with other PDOM records.
	 * 
	 * <b>A write-lock must be obtained before calling this method</b>
	 * 
	 * @param newConverter the converter to use to update internal file representations
	 * @throws CoreException
	 */
	public void rewriteLocations(final IIndexLocationConverter newConverter) throws CoreException {
		final List<PDOMFile> pdomfiles = new ArrayList<PDOMFile>();
		getFileIndex().accept(new IBTreeVisitor(){
			public int compare(long record) throws CoreException {
				return 0;
			}
			public boolean visit(long record) throws CoreException {
				PDOMFile file = PDOMFile.recreateFile(WritablePDOM.this, record);
				pdomfiles.add(file);
				return true;
			}
		});

		clearFileIndex();
		final List<PDOMFile> notConverted = new ArrayList<PDOMFile>();
		for (PDOMFile file : pdomfiles) {
			String internalFormat = newConverter.toInternalFormat(file.getLocation());
			if(internalFormat!=null) {
				file.setInternalLocation(internalFormat);
				getFileIndex().insert(file.getRecord());
			} else {
				notConverted.add(file);
			}
		}


		// remove content where converter returns null
		for (PDOMFile file : notConverted) {
			file.convertIncludersToUnresolved();
			file.clear(null);
		}
	}

	boolean isClearedBecauseOfVersionMismatch() {
		return fClearedBecauseOfVersionMismatch;
	}

	void setClearedBecauseOfVersionMismatch(boolean clearedBecauseOfVersionMismatch) {
		fClearedBecauseOfVersionMismatch = clearedBecauseOfVersionMismatch;
	}

	boolean isCreatedFromScratch() {
		return fCreatedFromScratch;
	}

	void setCreatedFromScratch(boolean createdFromScratch) {
		fCreatedFromScratch = createdFromScratch;
	}
	
	@Override
	protected final boolean isPermanentlyReadOnly() {
		return false;
	}

	public PDOMFile getFileForASTPath(int linkageID, String astPath) throws CoreException {
		if (fPathResolver != null) {
			if (astPath != null) {
				return getFile(linkageID, fPathResolver.resolveASTPath(astPath));
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.internal.core.index.IWritableIndexFragment#getDatabaseSizeBytes()
	 */
	public long getDatabaseSizeBytes() {
		return getDB().getSizeBytes();
	}
}
