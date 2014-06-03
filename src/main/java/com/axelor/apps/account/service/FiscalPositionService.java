/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.service;

import com.axelor.apps.account.db.Account;
import com.axelor.apps.account.db.AccountEquiv;
import com.axelor.apps.account.db.FiscalPosition;
import com.axelor.apps.account.db.Tax;
import com.axelor.apps.account.db.TaxEquiv;

public class FiscalPositionService {
	

	public Account getAccount(FiscalPosition fiscalPosition, Account account)  {
		
		if(fiscalPosition != null && fiscalPosition.getAccountEquivList() != null)  {
			for(AccountEquiv accountEquiv : fiscalPosition.getAccountEquivList())  {
				
				if(accountEquiv.getFromAccount().equals(account) && accountEquiv.getToAccount() != null)  {
					return accountEquiv.getToAccount();
				}
			}
		}
		
		return account;
	}
	
	
	public Tax getTax(FiscalPosition fiscalPosition, Tax tax)  {
		
		if(fiscalPosition != null && fiscalPosition.getTaxEquivList() != null)  {
			for(TaxEquiv taxEquiv : fiscalPosition.getTaxEquivList())  {
				
				if(taxEquiv.getFromTax().equals(tax) && taxEquiv.getToTax() != null)  {
					return taxEquiv.getToTax();
				}
			}
		}
		
		return tax;
	}
}
