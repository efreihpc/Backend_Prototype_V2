package backend.model.job;

import backend.model.result.Result;
import backend.model.result.DictionaryResult;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonTypeName;


@Entity
@Inheritance 

public class PersistJob extends JobEntity<DictionaryResult> {

	@Transient
	JobRepository m_jobRepository;
	
	@OneToOne(fetch = FetchType.EAGER)
	@org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.ALL)
	JobEntity m_jobToPersist;
	
	public PersistJob()
	{
		commonName("PersistJob");
	}
	
	public void jobRepository(JobRepository repository)
	{
		m_jobRepository = repository;
	}
	
	@Override
	protected void execute() {
		m_jobRepository.save(this.m_jobToPersist);	
	}
	
	public void jobToPersist(JobEntity job)
	{
		m_jobToPersist = job;
	}
}
