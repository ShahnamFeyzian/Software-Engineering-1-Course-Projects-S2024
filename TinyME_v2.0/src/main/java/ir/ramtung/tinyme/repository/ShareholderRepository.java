package ir.ramtung.tinyme.repository;

import ir.ramtung.tinyme.domain.entity.Shareholder;
import ir.ramtung.tinyme.repository.exception.NotFoundException;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class ShareholderRepository {

	private final HashMap<Long, Shareholder> shareholderById = new HashMap<>();

	public Shareholder findShareholderById(long shareholderId) {
		Shareholder shareholder = shareholderById.get(shareholderId);
		if (shareholder == null) {
            throw new NotFoundException(); 
        }
        
        return shareholder;
	}

	public boolean isThereShareholderWithId(long id) {
		try {
			this.findShareholderById(id);
			return true;
		} catch (NotFoundException exp) {
			return false;
		}
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
