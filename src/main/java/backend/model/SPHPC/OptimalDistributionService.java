package backend.model.SPHPC;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;

import backend.model.result.SimpleResult;
import backend.model.service.GenericService;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@Entity
@Inheritance                                                                                                                                                 
@JsonTypeName("SPHPCOptimalDistributionService")
public class OptimalDistributionService extends GenericService<SimpleResult> {

    @OneToOne
	@org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.ALL)
	private PrototypeJob m_job;
	
	public OptimalDistributionService()
	{
		commonName("Optimal Distribution Service");
		m_job = new PrototypeJob();
		result(new SimpleResult());
	}
		
	@Override
	public void execute() {
		executeJob(m_job);
	}

	@Override
	@JsonProperty("result")
	public SimpleResult result() {
		return m_job.result();
	}
}
