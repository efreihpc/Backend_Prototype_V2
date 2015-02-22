package backend.system;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.orm.jpa.JpaTransactionManager;

public class PluginTransactionManager extends JpaTransactionManager {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PluginTransactionManager(EntityManagerFactory emf)
	{
		super(emf);
	}
	
	public EntityManager entityManager()
	{
		return createEntityManagerForTransaction();
	}
	
}
