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
package com.axelor.apps.account.web;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.db.MoveTemplate;
import com.axelor.apps.account.db.MoveTemplateLine;
import com.axelor.apps.account.service.MoveTemplateService;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.google.common.base.Joiner;
import com.google.inject.Inject;

public class MoveTemplateController {
	private static final Logger LOG = LoggerFactory.getLogger(MoveTemplateController.class);
	
	@Inject
	MoveTemplateService mts;
	
	public void checkValidity(ActionRequest request, ActionResponse response){
		MoveTemplate moveTemplate = request.getContext().asType(MoveTemplate.class);
		moveTemplate = mts.find(moveTemplate.getId());
		BigDecimal creditPercent = BigDecimal.ZERO;
		BigDecimal debitPercent = BigDecimal.ZERO;
		Boolean partnerDebit = false;
		Boolean partnerCredit = false;
		for(MoveTemplateLine line : moveTemplate.getMoveTemplateLineList()){
			LOG.debug("Adding percent: {}",line.getPercentage());
			if(line.getDebitCreditSelect().equals("0"))
				debitPercent = debitPercent.add(line.getPercentage());
			else	
				creditPercent = creditPercent.add(line.getPercentage());
			
			if(line.getHasPartnerToDebit())
				partnerDebit = true;
			else if(line.getHasPartnerToCredit())
				partnerCredit = true;
			
		}
		
		LOG.debug("Credit percent: {}, Debit percent: {}",new Object[]{creditPercent,debitPercent});
		if(creditPercent.compareTo(BigDecimal.ZERO) != 0 && debitPercent.compareTo(BigDecimal.ZERO) != 0 && creditPercent.compareTo(debitPercent) == 0){
			if(partnerCredit && partnerDebit)
				moveTemplate.setPartnerInputSelect(3);
			else if(partnerCredit)
				moveTemplate.setPartnerInputSelect(2);
			else if(partnerDebit)
				moveTemplate.setPartnerInputSelect(1);
			mts.validateMoveTemplateLine(moveTemplate);
			response.setReload(true);
		}
		else
			response.setFlash("Template move is not balanced");
			
	}
	
	public void generateMove(ActionRequest request, ActionResponse response){
		HashMap<String,Object> moveTemplateMap = (HashMap<String, Object>) request.getContext().get("moveTemplate");
		MoveTemplate moveTemplate = mts.find(Long.parseLong(moveTemplateMap.get("id").toString()));
		List<HashMap<String,Object>> dataList = (List<HashMap<String, Object>>) request.getContext().get("dataInputList");
		LOG.debug("MoveTemplate : {}",moveTemplate);
		LOG.debug("Data inputlist : {}",dataList);
		if(dataList != null && !dataList.isEmpty()){
			List<Long> moveList = mts.generateMove(moveTemplate, dataList);
			if(moveList == null)
				response.setFlash("Error in move generation");
			else
				response.setView(ActionView
			            .define("Generated moves")
			            .model(Move.class.getName())
			            .add("grid", "move-grid")
			            .add("form", "move-form")
			            .domain("self.id in ("+Joiner.on(",").join(moveList)+")")
			            .map());
		}
		else 
			response.setFlash("Please fill input lines");
		
	}
	
	public void setIsValid(ActionRequest request, ActionResponse response){
		MoveTemplate moveTemplate = request.getContext().asType(MoveTemplate.class);
		if(moveTemplate.getIsValid())
			return;
//		List<MoveTemplateLine> moveTemplateLines = new ArrayList<MoveTemplateLine>(); 
		for(MoveTemplateLine line : moveTemplate.getMoveTemplateLineList()){
			line.setIsValid(moveTemplate.getIsValid());
		}
		response.setValue("moveTemplateLineList", moveTemplate.getMoveTemplateLineList());
	}
}
