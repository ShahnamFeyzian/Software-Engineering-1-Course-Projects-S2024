package ir.ramtung.tinyme.repository;

import ir.ramtung.tinyme.domain.entity.Shareholder;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class ShareholderRepository {

	private final HashMap<Long, Shareholder> shareholderById = new HashMap<>();

	public Shareholder findShareholderById(long shareholderId) {
		return shareholderById.get(shareholderId);
	}

	public void addShareholder(Shareholder shareholder) {
		shareholderById.put(shareholder.getShareholderId(), shareholder);
	}

	public void clear() {
		shareholderById.clear();
	}

	Iterable<? extends Shareholder> allShareholders() {
		return shareholderById.values();
	}
}
