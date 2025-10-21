package com.glt.idm.rest.handlers;/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
@Order
public class GltBinderControllerAdvice {

    /**
     * Should prevent Spring4Shell vulnerability (but we believe midPoint does not allow it anyway).
     * See https://spring.io/blog/2022/03/31/spring-framework-rce-early-announcement[this] for more info.
     */
    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("class.*", "Class.*", "*.class.*", "*.Class.*");
    }
}
