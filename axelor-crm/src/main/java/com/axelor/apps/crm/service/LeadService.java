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
package com.axelor.apps.crm.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.axelor.apps.base.db.Company;
import com.axelor.apps.base.db.IAdministration;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.service.PartnerService;
import com.axelor.apps.base.service.administration.SequenceService;
import com.axelor.apps.base.service.user.UserService;
import com.axelor.apps.crm.db.Event;
import com.axelor.apps.crm.db.ILead;
import com.axelor.apps.crm.db.Lead;
import com.axelor.apps.crm.db.Opportunity;
import com.axelor.apps.crm.db.repo.LeadRepository;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class LeadService extends LeadRepository {

	@Inject
	private SequenceService sequenceService;
	
	@Inject
	private UserService userService;
	
	@Inject
	private PartnerService partnerService;
	
	@Inject
	private OpportunityService opportunityService;
	
	@Inject
	private EventService  eventService;
	
	
	/**
	 * Convert lead into a partner
	 * @param lead
	 * @return
	 * @throws AxelorException
	 */
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public Lead convertLead(Lead lead, Partner partner, Partner contactPartner, Opportunity opportunity, Event callEvent, Event meetingEvent, Event taskEvent) throws AxelorException  {
		
//		lead.setEvent(meeting);
//		lead.setCall(call);
//		lead.setOpportunity(opportunity);
//		lead.setContactPartner(contact);
		
		if(partner != null && contactPartner != null)  {
			if(partner.getContactPartnerSet()==null)  {
				partner.setContactPartnerSet(new HashSet<Partner>());
			}
			partner.getContactPartnerSet().add(contactPartner);
		}
		
		if(opportunity != null && partner != null)  {
			opportunity.setPartner(partner);
		}
		
		if(partner != null)  {
			lead.setPartner(partner);
			partnerService.save(partner);
		}
		if(contactPartner!=null)  {
			partnerService.save(contactPartner);
		}
		if(opportunity!=null)  {
			opportunityService.save(opportunity);
		}
		if(callEvent!=null)  {
			eventService.save(callEvent);
		}
		if(meetingEvent!=null)  {
			eventService.save(meetingEvent);
		}
		if(taskEvent!=null)  {
			eventService.save(taskEvent);
		}
		
		lead.setPartner(partner);
		lead.setStatusSelect(ILead.STATUS_CONVERTED);
		save(lead);
		
		return lead;
		
	}
	
	
	/**
	 * Get sequence for partner
	 * @return
	 * @throws AxelorException
	 */
	public String getSequence() throws AxelorException  {
		
		String seq = sequenceService.getSequenceNumber(IAdministration.PARTNER);
		if (seq == null)  {
			throw new AxelorException("Aucune séquence configurée pour les tiers",
							IException.CONFIGURATION_ERROR);
		}
		return seq;
	}
	
	
	/**
	 * Assign user company to partner
	 * @param partner
	 * @return
	 */
	public Partner setPartnerCompany(Partner partner)  {
		
		
		if(userService.getUserActiveCompany() != null)  {
			partner.setCompanySet(new HashSet<Company>());
			partner.getCompanySet().add(userService.getUserActiveCompany());
		}
		
		return partner;
	}
	
	public Map<String,String> getSocialNetworkUrl(String name,String firstName, String companyName){
		
		Map<String,String> urlMap = new HashMap<String,String>();
		String searchName = firstName != null && name != null ? firstName+"+"+name : name == null ? firstName : name;  
		searchName = searchName == null ? "" : searchName;
		urlMap.put("facebook","<a class='fa fa-facebook' href='https://www.facebook.com/search/more/?q="+searchName+"&init=public"+"' target='_blank'/>");
		urlMap.put("twitter", "<a class='fa fa-twitter' href='https://twitter.com/search?q="+searchName+"' target='_blank' />");
		urlMap.put("linkedin","<a class='fa fa-linkedin' href='http://www.linkedin.com/pub/dir/"+searchName.replace("+","/")+"' target='_blank' />");
		if(companyName != null){
			urlMap.put("youtube","<a class='fa fa-youtube' href='https://www.youtube.com/results?search_query="+companyName+"' target='_blank' />");
			urlMap.put("google","<a class='fa fa-google-plus' href='https://www.google.com/?gws_rd=cr#q="+companyName+"+"+searchName+"' target='_blank' />");
		}
		else {
			urlMap.put("youtube","<a class='fa fa-youtube' href='https://www.youtube.com/results?search_query="+searchName+"' target='_blank' />");
			urlMap.put("google","<a class='fa fa-google-plus' href='https://www.google.com/?gws_rd=cr#q="+searchName+"' target='_blank' />");
		}
		return urlMap;
	}

	@Transactional
	public void saveLead(Lead lead){
		save(lead);
	}
	
}
