package com.axelor.apps.supplychain.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.axelor.apps.base.db.IProduct;
import com.axelor.apps.base.db.Product;
import com.axelor.apps.supplychain.db.IStockMove;
import com.axelor.apps.supplychain.db.Inventory;
import com.axelor.apps.supplychain.db.InventoryLine;
import com.axelor.apps.supplychain.db.Location;
import com.axelor.apps.supplychain.db.StockMove;
import com.axelor.apps.supplychain.db.StockMoveLine;
import com.axelor.apps.tool.file.CsvTool;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class InventoryService {
	
	@Inject
	StockMoveService stockMoveService;
	
	@Transactional
	public void importFile(String filePath, char separator, Inventory inventory) throws IOException, AxelorException {
		
		List<InventoryLine> inventoryLineList = inventory.getInventoryLineList();
		
		List<String[]> data = CsvTool.cSVFileReader(filePath, separator);
		
		if (data == null || data.isEmpty())
			throw new AxelorException("L'importation du fichier "+filePath+" a échouée", IException.CONFIGURATION_ERROR);
		
		HashMap<String,InventoryLine> inventoryLineMap = new HashMap<String,InventoryLine>();
		
		for (InventoryLine line : inventoryLineList) {
			if (line.getProduct() != null)
				inventoryLineMap.put(line.getProduct().getCode(), line);
		}
		
		for (String[] line : data) {
			if (line.length < 6)
				throw new AxelorException("Données importées invalides", IException.CONFIGURATION_ERROR);
			String code = line[1].replace("\"", "");
			Integer realQty = Integer.valueOf(line[4].replace("\"", ""));
			String description = line[5].replace("\"", "");
			
			if (inventoryLineMap.containsKey(code)) {
				inventoryLineMap.get(code).setRealQty(new BigDecimal(realQty));
				inventoryLineMap.get(code).setDescription(description);
			}
			else {
				Integer currentQty = Integer.valueOf(line[2].replace("\"", ""));
				InventoryLine inventoryLine = new InventoryLine();
				Product product = Product.findByCode(code);
				if (product == null || product.getApplicationTypeSelect() != IProduct.PRODUCT_TYPE || !product.getProductTypeSelect().equals(IProduct.STOCKABLE))
					throw new AxelorException("Produit "+code+" invalide", IException.CONFIGURATION_ERROR);
				inventoryLine.setProduct(product);
				inventoryLine.setInventory(inventory);
				inventoryLine.setCurrentQty(new BigDecimal(currentQty));
				inventoryLine.setRealQty(new BigDecimal(realQty));
				inventoryLine.setDescription(description);
				inventoryLineList.add(inventoryLine);
			}
		}
		inventory.setInventoryLineList(inventoryLineList);
		
		inventory.save();
	}
	
	public void generateStockMove(Inventory inventory) throws AxelorException {
		
		List<InventoryLine> inventoryLineList = inventory.getInventoryLineList();
		StockMove stockMove = null;
		List<StockMoveLine> stockMoveLineList = null;
		
		for (InventoryLine inventoryLine : inventoryLineList) {
			BigDecimal currentQty = inventoryLine.getCurrentQty();
			BigDecimal realQty = inventoryLine.getRealQty();
			if (currentQty.compareTo(realQty) != 0) {
				BigDecimal diff = realQty.subtract(currentQty);
				
				if (inventory.getLocation() == null || inventory.getLocation().getCompany() == null || inventoryLine.getProduct() == null ) {
					throw new AxelorException("Informations manquantes dans l'inventaire "+inventory.getName(), IException.CONFIGURATION_ERROR);
				}
				
				if (stockMove == null) {
					Location fromLocation = inventory.getLocation().getCompany().getInventoryVirtualLocation();
					Location toLocation = inventory.getLocation();
					stockMove = stockMoveService.createStocksMoves(null, toLocation.getCompany(), null, fromLocation, toLocation);
					stockMove.setTypeSelect(IStockMove.INTERNAL);
					stockMove.setName(inventory.getName());
					stockMove.setStockMoveSeq(inventory.getName());
				}
				
				StockMoveLine stockMoveLine = stockMoveService.createStocksMovesLines(inventoryLine.getProduct(), diff, 
						inventoryLine.getProduct().getUnit(), null, stockMove);
				
				if (stockMoveLine == null)
					throw new AxelorException("Produit incorrect dans la ligne de l'inventaire "+inventory.getName(), IException.CONFIGURATION_ERROR);
				
				if (stockMoveLineList == null)
					stockMoveLineList = new ArrayList<StockMoveLine>();
				stockMoveLineList.add(stockMoveLine);
			}
			
		}
		if (stockMoveLineList != null && stockMove != null) {
			stockMove.setStockMoveLineList(stockMoveLineList);
			stockMoveService.validate(stockMove);
		}
	}
}
