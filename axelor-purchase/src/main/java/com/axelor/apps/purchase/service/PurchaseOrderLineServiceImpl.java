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
package com.axelor.apps.purchase.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.account.db.TaxLine;
import com.axelor.apps.base.db.Partner;
import com.axelor.apps.base.db.PriceList;
import com.axelor.apps.base.db.PriceListLine;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.ProductVariant;
import com.axelor.apps.base.db.SupplierCatalog;
import com.axelor.apps.base.db.Unit;
import com.axelor.apps.base.service.CurrencyService;
import com.axelor.apps.base.service.PriceListService;
import com.axelor.apps.base.service.tax.AccountManagementService;
import com.axelor.apps.purchase.db.PurchaseOrder;
import com.axelor.apps.purchase.db.PurchaseOrderLine;
import com.axelor.db.Query;
import com.axelor.db.mapper.Property;
import com.axelor.exception.AxelorException;
import com.google.inject.Inject;

public class PurchaseOrderLineServiceImpl implements PurchaseOrderLineService  {
	private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderLineServiceImpl.class); 
	
	@Inject
	private CurrencyService currencyService;
	
	@Inject
	private AccountManagementService accountManagementService;
	
	@Inject
	private PriceListService priceListService;
	
	private int sequence = 0;
	
	/**
	 * Calculer le montant HT d'une ligne de commande.
	 * 
	 * @param quantity
	 *          Quantité.
	 * @param price
	 *          Le prix.
	 * 
	 * @return 
	 * 			Le montant HT de la ligne.
	 */
	public static BigDecimal computeAmount(BigDecimal quantity, BigDecimal price) {

		BigDecimal amount = quantity.multiply(price).setScale(2, RoundingMode.HALF_EVEN);

		LOG.debug("Calcul du montant HT avec une quantité de {} pour {} : {}", new Object[] { quantity, price, amount });

		return amount;
	}
	
	
	public BigDecimal getUnitPrice(PurchaseOrder purchaseOrder, PurchaseOrderLine purchaseOrderLine) throws AxelorException  {
		
		Product product = purchaseOrderLine.getProduct();
		
		return currencyService.getAmountCurrencyConverted(
			product.getPurchaseCurrency(), purchaseOrder.getCurrency(), product.getPurchasePrice(), purchaseOrder.getOrderDate());  
		
	}
	
	public BigDecimal getMinSalePrice(PurchaseOrder purchaseOrder, PurchaseOrderLine purchaseOrderLine) throws AxelorException  {
		
		Product product = purchaseOrderLine.getProduct();
		
		return currencyService.getAmountCurrencyConverted(
			product.getSaleCurrency(), purchaseOrder.getCurrency(), product.getSalePrice(), purchaseOrder.getOrderDate());  
		
	}
	
	public BigDecimal getSalePrice(PurchaseOrder purchaseOrder, BigDecimal price) throws AxelorException  {
		
		return price;  
		
	}
	
	
	public TaxLine getTaxLine(PurchaseOrder purchaseOrder, PurchaseOrderLine purchaseOrderLine) throws AxelorException  {
		
		return accountManagementService.getTaxLine(
				purchaseOrder.getOrderDate(), purchaseOrderLine.getProduct(), purchaseOrder.getCompany(), purchaseOrder.getSupplierPartner().getFiscalPosition(), true);
		
	}
	
	
	public BigDecimal computePurchaseOrderLine(PurchaseOrderLine purchaseOrderLine)  {
		
		return purchaseOrderLine.getExTaxTotal();
	}

	
	public BigDecimal getCompanyExTaxTotal(BigDecimal exTaxTotal, PurchaseOrder purchaseOrder) throws AxelorException  {
		
		return currencyService.getAmountCurrencyConverted(
				purchaseOrder.getCurrency(), purchaseOrder.getCompany().getCurrency(), exTaxTotal, purchaseOrder.getOrderDate());  
	}
	
	
	public PriceListLine getPriceListLine(PurchaseOrderLine purchaseOrderLine, PriceList priceList)  {
		
		return priceListService.getPriceListLine(purchaseOrderLine.getProduct(), purchaseOrderLine.getQty(), priceList);
	
	}
	
	
	public BigDecimal computeDiscount(PurchaseOrderLine purchaseOrderLine)  {
		
		return priceListService.computeDiscount(purchaseOrderLine.getPrice(), purchaseOrderLine.getDiscountTypeSelect(), purchaseOrderLine.getDiscountAmount());
		
	}
	
	
	public PurchaseOrderLine createPurchaseOrderLine(PurchaseOrder purchaseOrder, Product product, String description, ProductVariant productVariant, BigDecimal qty, Unit unit) throws AxelorException  {
		
		PurchaseOrderLine purchaseOrderLine = new PurchaseOrderLine();
		purchaseOrderLine.setPurchaseOrder(purchaseOrder);
		
		purchaseOrderLine.setEstimatedDelivDate(purchaseOrder.getDeliveryDate());
		purchaseOrderLine.setDescription(description);
		
		purchaseOrderLine.setIsOrdered(false);
		
		purchaseOrderLine.setProduct(product);
		purchaseOrderLine.setProductName(product.getName());
		purchaseOrderLine.setProductVariant(productVariant);
		
		purchaseOrderLine.setQty(qty);
		purchaseOrderLine.setSequence(sequence);
		sequence++;
		
		purchaseOrderLine.setUnit(unit);
		purchaseOrderLine.setTaxLine(this.getTaxLine(purchaseOrder, purchaseOrderLine));
		
		BigDecimal price = this.getUnitPrice(purchaseOrder, purchaseOrderLine);
		
		PriceList priceList = purchaseOrder.getPriceList();
		if(priceList != null)  {
			PriceListLine priceListLine = this.getPriceListLine(purchaseOrderLine, priceList);
			
			Map<String, Object> discounts = priceListService.getDiscounts(priceList, priceListLine, price);
			
			purchaseOrderLine.setDiscountAmount((BigDecimal) discounts.get("discountAmount"));
			purchaseOrderLine.setDiscountTypeSelect((Integer) discounts.get("discountTypeSelect"));
			
			if(discounts.get("price") != null)  {
				price = (BigDecimal) discounts.get("price");
			}
		}
		purchaseOrderLine.setPrice(price);
		
		BigDecimal exTaxTotal = PurchaseOrderLineServiceImpl.computeAmount(purchaseOrderLine.getQty(), this.computeDiscount(purchaseOrderLine));
			
		BigDecimal companyExTaxTotal = this.getCompanyExTaxTotal(exTaxTotal, purchaseOrder);
			
		purchaseOrderLine.setExTaxTotal(exTaxTotal);
		purchaseOrderLine.setCompanyExTaxTotal(companyExTaxTotal);
			
		return purchaseOrderLine;
	}
	
	
	public BigDecimal getQty(PurchaseOrder purchaseOrder, PurchaseOrderLine purchaseOrderLine)  {
		
		SupplierCatalog supplierCatalog = this.getSupplierCatalog(purchaseOrder,purchaseOrderLine);
		
		if(supplierCatalog != null)  {
			
			return supplierCatalog.getMinQty();
			
		}
		
		return BigDecimal.ONE;
		
	}
	
	public SupplierCatalog getSupplierCatalog(PurchaseOrder purchaseOrder, PurchaseOrderLine purchaseOrderLine)  {
		
		Product product = purchaseOrderLine.getProduct();
		
		SupplierCatalog supplierCatalog = this.getSupplierCatalog(product, purchaseOrder.getSupplierPartner());
		
		if(supplierCatalog == null)  {
			
			supplierCatalog = this.getSupplierCatalog(product, product.getDefaultSupplierPartner());
		}
		
		return supplierCatalog;
		
	}
	
	
	public SupplierCatalog getSupplierCatalog(Product product, Partner supplierPartner)  {
		
		if(product.getSupplierCatalogList() != null)  {
			
			for(SupplierCatalog supplierCatalog : product.getSupplierCatalogList())  {
				
				if(supplierCatalog.getSupplierPartner().equals(supplierPartner))  {
					return supplierCatalog;
				}
				
			}
			
		}
		return null;
		
	}


	@Override
	public Query<PurchaseOrderLine> all() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PurchaseOrderLine copy(PurchaseOrderLine arg0, boolean arg1) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PurchaseOrderLine create(Map<String, Object> arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<Property> fields() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public PurchaseOrderLine find(Long arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void flush() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void refresh(PurchaseOrderLine arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void remove(PurchaseOrderLine arg0) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PurchaseOrderLine save(PurchaseOrderLine arg0) {
		// TODO Auto-generated method stub
		return null;
	}


}
