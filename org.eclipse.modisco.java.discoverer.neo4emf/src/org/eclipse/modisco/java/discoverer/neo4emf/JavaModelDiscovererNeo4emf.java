package org.eclipse.modisco.java.discoverer.neo4emf;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.print.attribute.standard.Severity;

import org.apache.lucene.analysis.CharArrayMap.EntrySet;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.modisco.infra.discovery.ui.internal.celleditors.composite.TargetURIComposite;
import org.eclipse.modisco.java.discoverer.DiscoverJavaModelFromJavaProject;

import fr.inria.atlanmod.neo4emf.INeo4emfResource;
import fr.inria.atlanmod.neo4emf.INeo4emfResourceFactory;
import fr.inria.atlanmod.neo4emf.change.IChangeLog;
import fr.inria.atlanmod.neo4emf.change.impl.ChangeLog;
import fr.inria.atlanmod.neo4emf.change.impl.Entry;
import fr.inria.atlanmod.neo4emf.change.impl.NewObject;
import fr.inria.atlanmod.neo4emf.logger.Logger;

import org.eclipse.gmt.modisco.java.emf.JavaFactory;
import org.eclipse.gmt.modisco.java.neo4emf.reltypes.ReltypesMappings;

public class JavaModelDiscovererNeo4emf extends DiscoverJavaModelFromJavaProject {

	@Override
	protected Resource createTargetModel() {	

		String pathToResource = toFileString(getDefaultTargetURI());
		File folder = new File (pathToResource);
		if (folder.exists()) {	
			try {
				deleteDirectory(folder);
			} catch (IOException e) {
				e.printStackTrace();
			}
					}	
		URI uri = URI.createURI("neo4emf:/"+pathToResource);
		getResourceSet().getResourceFactoryRegistry().getProtocolToFactoryMap().
				put("neo4emf", INeo4emfResourceFactory.eINSTANCE.setRelationshipsMap(ReltypesMappings.getInstance().getMap()));
		// Create the resource 
		INeo4emfResource resource = (INeo4emfResource) getResourceSet().createResource(uri);
		setTargetModel(resource);
		return resource;		
	}
	
	private void deleteDirectory(File file) throws IOException{	 
	    	if(file.isDirectory()){
	    		if(file.list().length==0){	 
	    		   file.delete();
	    		} else{
	        	   String files[] = file.list();
	        	   for (String temp : files) {
	        	      File fileDelete = new File(file, temp);
	        	     deleteDirectory(fileDelete);
	        	   }
	        	   if(file.list().length==0){
	           	     file.delete();
	        	   }
	    		}	 
	    	}else{
	    		file.delete();
	    	}
	    }
	
	private String toFileString(URI defaultTargetURI) {
		StringBuffer str =new StringBuffer();
		str.append(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString());
		String relativePath = "/"+defaultTargetURI.segment(1)+"/"+defaultTargetURI.segment(1)+"_java_neo";
		str.append(relativePath);
		return str.toString();

	}

	@Override 
	protected void analyzeJavaProject(final IJavaProject source, final IProgressMonitor monitor) {
		try {
			super.analyzeJavaProject(source, monitor);
		}catch (Exception e){
			e.printStackTrace();
//			if (getTargetModel() != null){
//				if (getTargetModel() instanceof INeo4emfResource){
//					((INeo4emfResource)getTargetModel()).shutdown();
//					e.printStackTrace();
//				}
//			}
		}
	}
	
	@Override
	public JavaFactory getEFactory() {
		org.eclipse.gmt.modisco.java.neo4emf.impl.JavaPackageImpl.init();
		return org.eclipse.gmt.modisco.java.neo4emf.meta.JavaFactory.eINSTANCE;
	}

	@Override 
	protected void saveTargetModel() throws IOException {
		
		Map<String, Object> options = new HashMap<String, Object>();
		if (getTargetModel() == null) {
			createTargetModel();
		}
		try{
			long start = System.currentTimeMillis();
			Logger.log(Logger.SEVERITY_INFO, "Starting to save the model in Neo4EMF Resource : " + getTargetModel().getURI().lastSegment() );
			inspectChanges();
			((INeo4emfResource)getTargetModel()).save(options);			
			long end = System.currentTimeMillis();
			long time = end - start;
			long ms = (time%1000);
			long sec = (time-ms)/1000;
			long min = (sec - (sec % 60))/60;
			sec = sec % 60;
			Logger.log(Logger.SEVERITY_INFO, "Finished saving the model, time taken is : "+min +"mins, "+sec+ " secs, "+ms+ " millis" );
		}finally{
			((INeo4emfResource)getTargetModel()).shutdown();
		}
	}

	private void inspectChanges() {
		
		Logger.log(Logger.SEVERITY_INFO, "Starting inspection of ChangeLog");
		Iterator<Entry> iterator = ChangeLog.getInstance().iterator();
		int numberOfObjects = 0;
		Entry witness = null;
		EClass eClass =  null;
		int value = 0;
		Map<EClass, Integer> class2sizeMap = new HashMap<EClass, Integer>();
		while (iterator.hasNext()) {
			witness = iterator.next();
			value = 1;
			if (witness instanceof NewObject) {
				numberOfObjects++;
				eClass = witness.geteObject().eClass();
				if (class2sizeMap.containsKey(eClass)) {
					value = class2sizeMap.get(eClass);
					class2sizeMap.remove(eClass);
					
				}
					class2sizeMap.put(eClass, value+1);		
			}
			
		}
		
		
		Logger.log(Logger.SEVERITY_INFO, "Printing results to the log");
		
		for (java.util.Map.Entry<EClass, Integer> entry : class2sizeMap.entrySet()) {
			
			Logger.log(Logger.SEVERITY_INFO, "The size of "+entry.getKey()+" is : "+entry.getValue());
			
		}
		
	}
	

}
