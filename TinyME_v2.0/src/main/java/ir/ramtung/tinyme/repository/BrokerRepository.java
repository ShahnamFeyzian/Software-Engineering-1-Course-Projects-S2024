package ir.ramtung.tinyme.repository;

import ir.ramtung.tinyme.domain.entity.Broker;
import ir.ramtung.tinyme.repository.exception.NotFoundException;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class BrokerRepository {

	private final HashMap<Long, Broker> brokerById = new HashMap<>();

	public Broker findBrokerById(long brokerId) {
		Broker broker = brokerById.get(brokerId);
		if (broker == null) {
            throw new NotFoundException();
        } 
        
        return broker;
	}

	public boolean isThereBrokerWithId(long id) {
		try {
			this.findBrokerById(id);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
	}

	public void addBroker(Broker broker) {
		brokerById.put(broker.getBrokerId(), broker);
	}

	public void clear() {
		brokerById.clear();
	}

	Iterable<? extends Broker> allBrokers() {
		return brokerById.values();
	}
}
