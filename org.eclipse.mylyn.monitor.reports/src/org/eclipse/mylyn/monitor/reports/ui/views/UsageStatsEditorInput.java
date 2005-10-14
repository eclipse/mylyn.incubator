/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.monitor.reports.ui.views;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.monitor.reports.ReportGenerator;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * @author Mik Kersten
 *
 */
public class UsageStatsEditorInput implements IEditorInput {
	
	private ReportGenerator reportGenerator;
	private List<File> usageFiles;
	
	/**
	 * Supports either the single workspace file or multiple zip files.
	 */
	public UsageStatsEditorInput(List<File> files, ReportGenerator reportGenerator) {
		try {
			this.reportGenerator = reportGenerator;
			usageFiles = new ArrayList<File>();
			if (files.size() == 1 && files.get(0).getName().endsWith(".xml")) {
				usageFiles.add(files.get(0));
			} else {
				for (File file : files) {
		    		ZipFile zip = new ZipFile(file);
		    		if(zip.entries().hasMoreElements()){
		    			ZipEntry entry = zip.entries().nextElement();
		    			File tempFile = File.createTempFile(file.getName(), "xml");
		    			InputStream in = zip.getInputStream(entry);
		    			OutputStream out = new FileOutputStream(tempFile);
		    			transferData(in, out);
		    			usageFiles.add(tempFile);
		    		}
				}
			}
		} catch (Exception e) {
			MylarPlugin.log(e, "Could not unzip usage files");
		}
		
//		parser = new ReportGenerator(MylarMonitorPlugin.getDefault().getInteractionLogger(), collectors);
		reportGenerator.getStatisticsFromInteractionHistories(usageFiles);
	}
	
	public boolean exists() {
		return true;
	}

	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	public String getName() {
		return "Mylar Usage Statistics";
	}

	public IPersistableElement getPersistable() {
		return null;
	}

	public String getToolTipText() {
		return "Mylar Usage Statistics";
	}

	public Object getAdapter(Class adapter) {
		return null;
	}

	public List<File> getInputFiles() {
		return usageFiles;
	}

	public ReportGenerator getReportGenerator() {
		return reportGenerator;
	}
	
	private byte[] buffer = new byte[8192];
	
	public void transferData(InputStream sourceStream, OutputStream destination) throws IOException {
		int bytesRead = 0;
		while(bytesRead != -1){
			bytesRead = sourceStream.read(buffer, 0, buffer.length);
			if(bytesRead != -1){
				destination.write(buffer, 0, bytesRead);
			}
		}
	}
}
