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
package com.axelor.apps.base.service.imports.importer;

import javax.inject.Inject;

import com.axelor.apps.base.db.ImportHistory;
import com.axelor.apps.base.service.imports.listener.ImporterListener;
import com.axelor.data.xml.XMLImporter;
import com.google.inject.Injector;

class ImporterXML extends Importer {

	@Inject
	public ImporterXML( Injector injector ) { super( injector ); }

	@Override
	protected ImportHistory process( String bind, String data ) {

		XMLImporter importer = new XMLImporter(injector, bind, data);
		
		ImporterListener listener = new ImporterListener( getConfiguration().getName() ); 		
		importer.addListener( listener );
		importer.run(null);
		
		return addHistory( listener );
		
	}


}
