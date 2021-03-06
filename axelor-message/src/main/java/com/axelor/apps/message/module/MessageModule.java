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
package com.axelor.apps.message.module;

import com.axelor.app.AxelorModule;
import com.axelor.app.AxelorModuleInfo;
import com.axelor.apps.message.service.MailAccountService;
import com.axelor.apps.message.service.MailAccountServiceImpl;
import com.axelor.apps.message.service.MessageService;
import com.axelor.apps.message.service.MessageServiceImpl;
import com.axelor.apps.message.service.TemplateMessageServiceImpl;

@AxelorModuleInfo(name = "axelor-message")
public class MessageModule extends AxelorModule {

    @Override
    protected void configure() {
    	bind(TemplateMessageServiceImpl.class);
        bind(MessageService.class).to(MessageServiceImpl.class);
        bind(MailAccountService.class).to(MailAccountServiceImpl.class);
    }
}