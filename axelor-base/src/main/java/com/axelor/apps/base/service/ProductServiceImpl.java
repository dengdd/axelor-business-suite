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

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.IProductVariant;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.base.db.ProductVariant;
import com.axelor.apps.base.db.ProductVariantAttr;
import com.axelor.apps.base.db.ProductVariantConfig;
import com.axelor.apps.base.db.ProductVariantValue;
import com.axelor.apps.base.db.repo.ProductRepository;
import com.axelor.exception.AxelorException;
import com.beust.jcommander.internal.Lists;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ProductServiceImpl extends ProductRepository implements ProductService  {

	private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);
		
	@Inject
	private ProductVariantService productVariantService;
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void updateProductPrice(Product product)  {
		
		this.updateSalePrice(product);
		
		save(product);
		
	}
	
	
	/**
	 * Retourne le prix d'un produit à une date t.
	 * 
	 * @param product
	 * @param date
	 * @return
	 */
	public BigDecimal getPrice(Product product, boolean isPurchase){
		
		if(isPurchase)  {  return product.getPurchasePrice();  }
		else  {  return product.getSalePrice();  }

	}
	
	
	public void updateSalePrice(Product product)  {
		
		BigDecimal managePriceCoef = product.getManagPriceCoef();
		
		if(product.getCostPrice() != null)  {
			
			if(product.getProductVariant() != null)  {
				
				product.setCostPrice(product.getCostPrice().add(this.getProductExtraPrice(product.getProductVariant(), IProductVariant.APPLICATION_COST_PRICE)));
				
			}
			
		}
		
		if(product.getCostPrice() != null && managePriceCoef != null)  {
			
			product.setSalePrice((product.getCostPrice().multiply(managePriceCoef)).setScale(5, BigDecimal.ROUND_HALF_UP));
			
			if(product.getProductVariant() != null)  {
				
				product.setSalePrice(product.getSalePrice().add(this.getProductExtraPrice(product.getProductVariant(), IProductVariant.APPLICATION_SALE_PRICE)));
				
			}
		}
		
		if(product.getProductVariantConfig() != null)  {
			
			this.updateSalePriceOfVariant(product);
			
		}
	}
	
	
	private void updateSalePriceOfVariant(Product product)  {
		
		List<? extends Product> productVariantList = all().filter("self.parentProduct = ?1", product).fetch();
		
		for(Product productVariant : productVariantList)  {
			
			productVariant.setCostPrice(product.getCostPrice());
			productVariant.setSalePrice(product.getSalePrice());
			productVariant.setManagPriceCoef(product.getManagPriceCoef());
			
			this.updateSalePrice(productVariant);
			
		}
		
		
	}
	
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void generateProductVariants(Product productModel)  {
		
		List<ProductVariant> productVariantList = this.getProductVariantList(productModel.getProductVariantConfig());
		
		for(ProductVariant productVariant : productVariantList)  {
			
			productVariantService.save(productVariant);
			
			save(this.createProduct(productModel, productVariant));
			
		}
		
	}
	
	
	public Product createProduct(Product productModel, ProductVariant productVariant)  {
		
		Product product = new Product(
				productModel.getName()+" ("+productVariant.getName()+")",
				productModel.getCode()+"-"+productVariant.getId(),
				productModel.getDescription()+"<br>"+productVariant.getName(), 
				productModel.getInternalDescription()+"<br>"+productVariant.getName(),
				productModel.getPicture(), 
				productModel.getProductCategory(), 
				productModel.getProductFamily(), 
				productModel.getUnit(), 
				productModel.getApplicationTypeSelect(), 
				productModel.getSaleSupplySelect(), 
				productModel.getProductTypeSelect(), 
				productModel.getProcurementMethodSelect(), 
				productModel.getIsRawMaterial(), 
				productModel.getSaleCurrency(), 
				productModel.getPurchaseCurrency(), 
				productModel.getStartDate(), 
				productModel.getEndDate());
		
		productModel.setIsModel(true);
		
		product.setIsModel(false);
		product.setParentProduct(productModel);
		product.setProductVariant(productVariant);
		
		this.updateSalePrice(product);
		
		return product;
	}
	
	
	/**
	 * 
	 * @param productVariant
	 * @param applicationPriceSelect
	 * 		- 1 : Sale price
	 * 		- 2 : Cost price
	 * @return
	 */
	public BigDecimal getProductExtraPrice(ProductVariant productVariant, int applicationPriceSelect)  {
		
		BigDecimal extraPrice = BigDecimal.ZERO;
		
		ProductVariantValue productVariantValue1 = productVariant.getProductVariantValue1();
		ProductVariantValue productVariantValue2 = productVariant.getProductVariantValue2();
		ProductVariantValue productVariantValue3 = productVariant.getProductVariantValue3();
		ProductVariantValue productVariantValue4 = productVariant.getProductVariantValue4();
		
		
		if(productVariantValue1 != null && productVariantValue1.getApplicationPriceSelect() == applicationPriceSelect)  {
			
			extraPrice = extraPrice.add(productVariantValue1.getPriceExtra());
			
		}
		
		if(productVariantValue2 != null)  {
			
			extraPrice = extraPrice.add(productVariantValue2.getPriceExtra());
			
		}

		if(productVariantValue3 != null)  {
			
			extraPrice = extraPrice.add(productVariantValue3.getPriceExtra());
			
		}
		
		if(productVariantValue4 != null)  {
			
			extraPrice = extraPrice.add(productVariantValue4.getPriceExtra());
			
		}
		
		return extraPrice;
		
	}
	
	
	private List<ProductVariant> getProductVariantList(ProductVariantConfig productVariantConfig)  {
		
		List<ProductVariant> productVariantList = Lists.newArrayList();
		
		if(productVariantConfig.getProductVariantAttr1() != null && productVariantConfig.getProductVariantValue1Set() != null)  {
			
			for(ProductVariantValue productVariantValue1 : productVariantConfig.getProductVariantValue1Set())  {
				
				productVariantList.addAll(this.getProductVariantList(productVariantConfig, productVariantValue1));
				
			}
		}
		
		return productVariantList;
	}
	
	
	private List<ProductVariant> getProductVariantList(ProductVariantConfig productVariantConfig, ProductVariantValue productVariantValue1)  {
		
		List<ProductVariant> productVariantList = Lists.newArrayList();
		
		if(productVariantConfig.getProductVariantAttr2() != null && productVariantConfig.getProductVariantValue2Set() != null)  {
			
			for(ProductVariantValue productVariantValue2 : productVariantConfig.getProductVariantValue2Set())  {

				productVariantList.addAll(this.getProductVariantList(productVariantConfig, productVariantValue1, productVariantValue2));
				
			}
		}
		
		else  {
			
			productVariantList.add( 
					this.createProductVariant(productVariantConfig, productVariantValue1, null, null, 	null));
		}
		
		return productVariantList;
		
	}
	
	
	private List<ProductVariant> getProductVariantList(ProductVariantConfig productVariantConfig, ProductVariantValue productVariantValue1, ProductVariantValue productVariantValue2)  {
		
		List<ProductVariant> productVariantList = Lists.newArrayList();
		
		if(productVariantConfig.getProductVariantAttr3() != null && productVariantConfig.getProductVariantValue3Set() != null)  {
			
			for(ProductVariantValue productVariantValue3 : productVariantConfig.getProductVariantValue3Set())  {
				
				productVariantList.addAll(this.getProductVariantList(productVariantConfig, productVariantValue1, productVariantValue2, productVariantValue3));
			}
		}
		
		else  {
			
			productVariantList.add(
					this.createProductVariant(productVariantConfig, productVariantValue1, productVariantValue2, null, 	null));
		}
		
		return productVariantList;
		
	}
	
	
	private List<ProductVariant> getProductVariantList(ProductVariantConfig productVariantConfig, ProductVariantValue productVariantValue1, ProductVariantValue productVariantValue2,
			ProductVariantValue productVariantValue3)  {
		
		List<ProductVariant> productVariantList = Lists.newArrayList();
		
		if(productVariantConfig.getProductVariantAttr4() != null && productVariantConfig.getProductVariantValue4Set() != null)  {
			
			for(ProductVariantValue productVariantValue4 : productVariantConfig.getProductVariantValue4Set())  {
				
				productVariantList.add(
						this.createProductVariant(productVariantConfig, productVariantValue1, productVariantValue2, productVariantValue3, 	productVariantValue4));
			}
		}
		
		else  {
			
			productVariantList.add(
					this.createProductVariant(productVariantConfig, productVariantValue1, productVariantValue2, productVariantValue3, 	null));
		}
		
		return productVariantList;
		
	}
	
	
	public ProductVariant createProductVariant(ProductVariantConfig productVariantConfig, ProductVariantValue productVariantValue1, ProductVariantValue productVariantValue2,
			ProductVariantValue productVariantValue3, ProductVariantValue productVariantValue4)  {
		
		ProductVariantAttr productVariantAttr1 = null, productVariantAttr2 = null, productVariantAttr3 = null, productVariantAttr4 = null;
		if(productVariantValue1 != null)  {
			productVariantAttr1 = productVariantConfig.getProductVariantAttr1();
		}
		if(productVariantValue2 != null)  {
			productVariantAttr2 = productVariantConfig.getProductVariantAttr2();
		}
		if(productVariantValue3 != null)  {
			productVariantAttr3 = productVariantConfig.getProductVariantAttr3();
		}
		if(productVariantValue4 != null)  {
			productVariantAttr4 = productVariantConfig.getProductVariantAttr4();
		}
		
		return productVariantService.createProductVariant(
				productVariantAttr1, 
				productVariantAttr2, 
				productVariantAttr3, 
				productVariantAttr4, 
				productVariantValue1, 
				productVariantValue2, 
				productVariantValue3, 
				productVariantValue4, 
				false);
		
	}
 	
	
	
	
}
