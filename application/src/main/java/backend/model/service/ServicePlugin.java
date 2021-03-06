package backend.model.service;

import javax.persistence.Entity;
import javax.persistence.Inheritance;

import ro.fortsoft.pf4j.ExtensionPoint;
import backend.model.result.Result;

@Entity
@Inheritance
public abstract class ServicePlugin<T extends Result> extends ServiceEntity<T> implements ExtensionPoint{
	
	public ServicePlugin()
	{
		super();
    	m_classLoader = "Plugin";
    	descriptor().pluginIdentifier(true);
	}
}
