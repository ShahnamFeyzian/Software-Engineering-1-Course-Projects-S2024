package ir.ramtung.tinyme.repository;

import ir.ramtung.tinyme.domain.entity.Security;
import ir.ramtung.tinyme.repository.exception.NotFoundException;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class SecurityRepository {

	private final HashMap<String, Security> securityByIsin = new HashMap<>();

	public Security findSecurityByIsin(String isin) {
		Security security = securityByIsin.get(isin);
		if (security == null) {
			throw new NotFoundException();
		}

		return security;
	}

	public boolean isThereSecurityWithIsin(String isin) {
		try {
			this.findSecurityByIsin(isin);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
	}

	public void addSecurity(Security security) {
		securityByIsin.put(security.getIsin(), security);
	}

	public void clear() {
		securityByIsin.clear();
	}

	Iterable<? extends Security> allSecurities() {
		return securityByIsin.values();
	}
}
