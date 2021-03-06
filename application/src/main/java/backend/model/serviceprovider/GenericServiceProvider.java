package backend.model.serviceprovider;

import java.util.HashMap;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

import ro.fortsoft.pf4j.ExtensionPoint;
import backend.model.dependency.ServiceDependency;
import backend.model.descriptor.Descriptor;
import backend.model.descriptor.ServiceDescriptor;
import backend.model.result.Result;
import backend.model.service.ServiceEntity;
import backend.model.service.ServicePersistenceUnit;
import backend.model.task.ConfigurationFailedException;
import backend.model.task.TaskQueue;
import backend.model.task.TaskRepository;
import backend.system.GlobalPersistenceUnit;
import backend.system.execution.ThreadPoolExecutor;

import com.fasterxml.jackson.annotation.JsonProperty;

//TODO: move execution to a new Class
public abstract class GenericServiceProvider implements ExtensionPoint, ServiceProvider{
	
	public static class ServiceProviderDescriptor extends Descriptor<GenericServiceProvider>
	{
		public ServiceProviderDescriptor(Class<GenericServiceProvider> providerClass)
		{
			super(providerClass);
		}
	}
	
	@JsonProperty("descriptor")
    private ServiceProviderDescriptor m_descriptor;
    
	@JsonProperty("services")
    private HashMap<String, ServiceDescriptor> m_registeredServices;
    
    private GlobalPersistenceUnit m_globalPersistenceUnit;
    private ServicePersistenceUnit m_servicePersistenceUnit;
    private ServiceProviderRepository m_serviceProviderRepository;
    private TaskRepository m_taskRepository;
    
    ThreadPoolExecutor m_jobExecutor;
    ThreadPoolExecutor m_serviceExecutor;
    
    public GenericServiceProvider()
    {
	    	m_descriptor = new ServiceProviderDescriptor((Class<GenericServiceProvider>)this.getClass());
	    	m_descriptor.commonName(this.getClass().getName());
	    	m_registeredServices = new HashMap<String, ServiceDescriptor>();
	    	m_jobExecutor = new ThreadPoolExecutor("jobExecutor");
	    	m_serviceExecutor = new ThreadPoolExecutor("serviceExecutor");
	    	registerServices();
    }
    
    @JsonProperty("descriptor")
    public ServiceProviderDescriptor descriptor()
    {
    	return m_descriptor;
    }
    
    public String commonName()
    {
    	return m_descriptor.commonName();
    }
    
    protected void commonName(String name)
    {
    	m_descriptor.commonName(name);
    }
    
    @Override
    public ServiceDescriptor serviceDescriptor(String serviceIdentifier)
    {
    	return m_registeredServices.get(serviceIdentifier);
    }
    
    @JsonProperty("services")
    @Override
    public HashMap<String, ServiceDescriptor> services()
    {
    	return m_registeredServices;
    }
    
    @Override
    public <E extends Result> ServiceEntity<E> service(String serviceIdentifier, Result configuration) throws InstantiationException, IllegalAccessException
    {
    	Class<ServiceEntity> serviceClass = m_registeredServices.get(serviceIdentifier).classDescriptor();
    	ServiceEntity<E> newService = (ServiceEntity<E>) serviceClass.newInstance();
    	
    	System.out.println("GenericServiceProvider> creating Service: " + newService.commonName());
    	
    	newService.jobExecutor(m_jobExecutor);
    	
    	if(m_globalPersistenceUnit != null)
    		newService.persistenceUnit(m_globalPersistenceUnit);
    	
		try 
		{
	    	newService.configuration(configuration);
		} 
		catch (ConfigurationFailedException e) 
		{
			System.out.println("ServiceEntity> Configuration Error: ");
			System.out.println("\t" + e.getMessage());
			e.printStackTrace();
		}
    	
    	System.out.println("GenericServiceProvider> Result of new Service:" + newService.result() );
    	
    	if(m_servicePersistenceUnit != null)
    		m_servicePersistenceUnit.save(newService);
    	
    	newService.providerIdentifier(m_descriptor.identifier());    	
    	return newService;
    }
    
    @Override
    public <T extends Result> void executeService(ServiceEntity<T> serviceToExecute)
    {
    	executeServiceQueue(serviceExecutionQueue(serviceToExecute));
    }
    
    public <T extends Result> void executeServiceQueue(TaskQueue queueToExecute)
    {
    	m_serviceExecutor.execute(queueToExecute);
    }    
    
    public <T extends Result> TaskQueue serviceExecutionQueue(ServiceEntity<T> serviceToExecute)
    {
    	TaskQueue queue = new TaskQueue();
    	m_taskRepository.save(queue);
    	
    	for(ServiceDependency dependency: serviceToExecute.dependencies())
    	{
    		System.out.println("GenericServiceProvider> Dependency found: " + dependency.descriptor().identifier());
			GenericServiceProvider provider;
			try 
			{
				provider = m_serviceProviderRepository.serviceProvider(dependency.descriptor().providerIdentifier());
				ServiceEntity service = provider.service(dependency.descriptor().identifier(), null);
				
				try 
				{
					service.configuration(dependency.configuration());
				} 
				catch (ConfigurationFailedException e) 
				{
					System.out.println("ServiceEntity> Configuration Error: ");
					System.out.println("\t" + e.getMessage());
					e.printStackTrace();
				}
				
				System.out.println("GenericServiceProvider> Dependency instantiated: " + service.descriptor().commonName());
				dependency.task(service);
				queue.enqueue(provider.serviceExecutionQueue(service));
			} 
			catch (InstantiationException | IllegalAccessException e) 
			{
				e.printStackTrace();
			}
    	}
    	
    	queue.enqueue(serviceToExecute);
    	return queue;
    }
    
    public boolean hasService(String identifier)
    {
    	return m_registeredServices.containsKey(identifier);	
    }
    
    private void registerServices()
    {    	
    	// create scanner and disable default filters (that is the 'false' argument)
    	final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    	ClassLoader classLoader = this.getClass().getClassLoader();
    	// set current classes classloader in case it had been loaded as a plugin
    	provider.setResourceLoader(new PathMatchingResourcePatternResolver(classLoader));
    	// add include filters which matches all the classes (or use your own)
    	provider.addIncludeFilter((TypeFilter) new AssignableTypeFilter(ServiceEntity.class));
    	// get matching classes defined in the package
    	final Set<BeanDefinition> classes = provider.findCandidateComponents(this.getClass().getPackage().getName());
    	
    	for (BeanDefinition definition : classes) {
			try 
			{
				Class<ServiceEntity> registeredClass;
				registeredClass = (Class<ServiceEntity>) classLoader.loadClass(definition.getBeanClassName());

				ServiceEntity instance = registeredClass.newInstance();
				String commonName = instance.commonName();
				
				instance.providerIdentifier(m_descriptor.identifier());
				
				m_registeredServices.put(instance.descriptor().identifier(), instance.descriptor());
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			} 
			catch (IllegalAccessException e) 
			{
				e.printStackTrace();
			} 
			catch (IllegalArgumentException e) 
			{
				e.printStackTrace();
			} 
			catch (InstantiationException e) 
			{
				e.printStackTrace();
			}
    	}
    }
    
	@Override
	public void persistenceUnit(GlobalPersistenceUnit persistenceUnit) {
		m_globalPersistenceUnit = persistenceUnit;
		m_servicePersistenceUnit = persistenceUnit.servicePersistence();
		m_serviceProviderRepository = persistenceUnit.serviceProviderRepository();
		m_taskRepository = persistenceUnit.taskRepository();
	}
	
	@Override
	public GlobalPersistenceUnit persistenceUnit()
	{
		return m_globalPersistenceUnit;
	}
    
}
