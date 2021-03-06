/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2014 Axelor (<http://axelor.com>).
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
package com.axelor.apps.base.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import com.axelor.apps.base.db.Address;
import com.axelor.apps.base.db.Country;
import com.axelor.apps.base.db.repo.AddressRepository;
import com.axelor.apps.base.db.repo.PartnerRepository;
import com.google.inject.Inject;


public class AddressServiceImpl extends AddressRepository implements AddressService  {
	
	
	@Inject
	private com.axelor.apps.tool.address.AddressTool ads;
	
	@Inject
	private PartnerRepository partnerRepo;
	
	private static final Logger LOG = LoggerFactory.getLogger(AddressServiceImpl.class);
	
	public boolean check(String wsdlUrl) {
		return ads.doCanSearch(wsdlUrl);
	}
	
	public Map<String,Object> validate(String wsdlUrl, String search) {
		return (Map<String, Object>) ads.doSearch(wsdlUrl, search);
	}
	
	public com.qas.web_2005_02.Address select(String wsdlUrl, String moniker) {
		return ads.doGetAddress(wsdlUrl, moniker);
	}
	
	public int export(String path) throws IOException {
		List<Address> addresses = (List<Address>) all().filter("self.certifiedOk IS FALSE").fetch();
		
		CSVWriter csv = new CSVWriter(new java.io.FileWriter(path), "|".charAt(0), CSVWriter.NO_QUOTE_CHARACTER);
		List<String> header = new ArrayList<String>();
		header.add("Id");
		header.add("AddressL1");
		header.add("AddressL2");
		header.add("AddressL3");
		header.add("AddressL4");
		header.add("AddressL5");
		header.add("AddressL6");
		header.add("CodeINSEE");
		
		csv.writeNext(header.toArray(new String[header.size()]));
		List<String> items = new ArrayList<String>();
		for (Address a : addresses) {
			
			items.add(a.getId() != null ? a.getId().toString(): "");
			items.add(a.getAddressL2() != null ? a.getAddressL2(): "");
			items.add(a.getAddressL3() != null ? a.getAddressL3(): "");
			items.add(a.getAddressL4() != null ? a.getAddressL4(): "");
			items.add(a.getAddressL5() != null ? a.getAddressL5(): "");
			items.add(a.getAddressL6() != null ? a.getAddressL6(): "");
			items.add(a.getInseeCode() != null ? a.getInseeCode(): "");
			
			csv.writeNext(items.toArray(new String[items.size()]));
			items.clear();
		}
		csv.close();
		LOG.info("{} exported", path);
		
		return addresses.size();
	}
	
	
	public Address createAddress(String addressL2, String addressL3, String addressL4, String addressL5, String addressL6, Country addressL7Country)  {
		
		Address address = new Address();
		address.setAddressL2(addressL2);
		address.setAddressL3(addressL3);
		address.setAddressL4(addressL4);
		address.setAddressL5(addressL5);
		address.setAddressL6(addressL6);
		address.setAddressL7Country(addressL7Country);
		
		return address;
	}
	
	
	public Address getAddress(String addressL2, String addressL3, String addressL4, String addressL5, String addressL6, Country addressL7Country)  {
		
		return all().filter("self.addressL2 = ?1 AND self.addressL3 = ?2 AND self.addressL4 = ?3 " +
				"AND self.addressL5 = ?4 AND self.addressL6 = ?5 AND self.addressL7Country = ?6",
				addressL2,
				addressL3,
				addressL4,
				addressL5,
				addressL6,
				addressL7Country).fetchOne();
	}
	
	
	public boolean checkAddressUsed(Long addressId){
		LOG.debug("Address Id to be checked = {}",addressId);
		if(addressId != null){
			if(partnerRepo.all().filter("self.mainInvoicingAddress.id = ?1 OR self.deliveryAddress.id = ?1",addressId).fetchOne() != null)
				return true;
		}
		return false;
	}
	
	
}
