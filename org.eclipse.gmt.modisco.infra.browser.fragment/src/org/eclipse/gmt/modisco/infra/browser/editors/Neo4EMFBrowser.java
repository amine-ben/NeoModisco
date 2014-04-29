package org.eclipse.gmt.modisco.infra.browser.editors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.common.util.URI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.gmt.modisco.infra.browser.util.Neo4EMFEditorInput;
import org.eclipse.gmt.modisco.java.neo4emf.meta.JavaFactory;
import org.eclipse.gmt.modisco.java.neo4emf.reltypes.ReltypesMappings;

import fr.inria.atlanmod.neo4emf.INeo4emfResource;
import fr.inria.atlanmod.neo4emf.INeo4emfResourceFactory;
import fr.inria.atlanmod.neo4emf.impl.Neo4emfResource;
public class Neo4EMFBrowser extends EcoreBrowser {
	
	public static final String EDITOR_ID = "org.eclipse.gmt.modisco.infra.browser.NeoEditor";

	public Neo4EMFBrowser() {
	}
	
	@Override
	protected Resource createModel() {
			Assert.isTrue(getEditorInput() instanceof Neo4EMFEditorInput, getEditingDomain()+ " is not type of Neo4EMFEditorInput");
		IFolder dbFolder = ((Neo4EMFEditorInput)getEditorInput()).getDbFolder();	
		URI neoURI = URI.createURI("neo4emf:/"+ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()+dbFolder.getFullPath().toString());
		JavaFactory factory = JavaFactory.eINSTANCE;
		ResourceSet rscSet = createResourceSet();
		rscSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("neo4emf", 
				INeo4emfResourceFactory.eINSTANCE.setRelationshipsMap(ReltypesMappings.getInstance().getMap()));
		// Create the resource
		INeo4emfResource resource = (INeo4emfResource) rscSet.createResource(neoURI);
		// TODO force a FULL_LOAD
		Map<String, String> loadOptions = new HashMap<String, String>();
		loadOptions.put(INeo4emfResource.LOADING_STRATEGY, INeo4emfResource.FULL_LOADING);
		try {
			resource.getAllInstances(JavaFactory.eINSTANCE.getjavaPackage().getAbstractMethodDeclaration());
			resource.load(null);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return resource;
	}

	@Override
	public void dispose() {
		for (Resource resource : getEditingDomain().getResourceSet().getResources()) {
			if (resource instanceof Neo4emfResource) {
				((Neo4emfResource) resource).shutdown();
			}
		}
		super.dispose();
	}
	
}
