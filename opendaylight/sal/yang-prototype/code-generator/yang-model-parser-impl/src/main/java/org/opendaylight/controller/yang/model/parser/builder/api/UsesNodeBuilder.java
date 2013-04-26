/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import java.util.List;

import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.UsesNode;
import org.opendaylight.controller.yang.model.parser.util.RefineHolder;

/**
 * Interface for builders of 'uses' statement.
 */
public interface UsesNodeBuilder extends Builder {

    SchemaPath getGroupingPath();
    void addAugment(AugmentationSchemaBuilder builder);
    void setAugmenting(boolean augmenting);
    List<RefineHolder> getRefines();
    void addRefine(RefineHolder refine);
    void addRefineNode(SchemaNodeBuilder refineNode);
    UsesNode build();

}
