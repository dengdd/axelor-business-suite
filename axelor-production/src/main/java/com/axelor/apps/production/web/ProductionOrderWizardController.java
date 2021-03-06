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
package com.axelor.apps.production.web;

import java.math.BigDecimal;
import java.util.Map;

import javax.inject.Inject;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.repo.ProductRepository;
//import com.axelor.apps.organisation.db.Project;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ProductionOrder;
import com.axelor.apps.production.service.BillOfMaterialService;
import com.axelor.apps.production.service.ProductionOrderService;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.rpc.Context;

public class ProductionOrderWizardController {

	@Inject
	private ProductionOrderService productionOrderService;
	
	@Inject
	private BillOfMaterialService billOfMaterialService;
	
	@Inject
	private ProductRepository productRepo;
	
	@SuppressWarnings("unchecked")
	public void validate (ActionRequest request, ActionResponse response) throws AxelorException {

		Context context = request.getContext();
		
		if(context.get("qty") == null || new BigDecimal((String)context.get("qty")).compareTo(BigDecimal.ZERO) <= 0)  {
			response.setFlash("Veuillez entrer une quantité positive !");
		}
		else if(context.get("billOfMaterial") == null)  {
			response.setFlash("Veuillez sélectionner une nomenclature !");
		}
		else  {
			Map<String, Object> bomContext = (Map<String, Object>) context.get("billOfMaterial");
			BillOfMaterial billOfMaterial = billOfMaterialService.find(((Integer) bomContext.get("id")).longValue());
			
			BigDecimal qty = new BigDecimal((String)context.get("qty"));
			
			Product product = null;
			
			if(context.get("product") != null)  {
				Map<String, Object> productContext = (Map<String, Object>) context.get("product");
				product = productRepo.find(((Integer) productContext.get("id")).longValue());
			}
			else  {
				product = billOfMaterial.getProduct();
			}
			
//			Project businessProject = null;
//			if(context.get("business_id") != null)  {
//				businessProject = Project.find(((Integer) context.get("business_id")).longValue());
//			}
			
//			ProductionOrder productionOrder = productionOrderService.generateProductionOrder(product, billOfMaterial, qty, businessProject);
			ProductionOrder productionOrder = productionOrderService.generateProductionOrder(product, billOfMaterial, qty);
			
			if(productionOrder != null)  {
				response.setFlash("Ordre de production créé ("+productionOrder.getProductionOrderSeq()+")");
			}
			else  {
				response.setFlash("Erreur lors de la création de l'ordre de production");
			}
		}
		
	}
	
}
