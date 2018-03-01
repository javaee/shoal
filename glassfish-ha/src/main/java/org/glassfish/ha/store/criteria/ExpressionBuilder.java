/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.ha.store.criteria;

import org.glassfish.ha.store.criteria.spi.*;
import org.glassfish.ha.store.spi.AttributeMetadata;

/**
 * A Class to construct portable Criteria objects
 *
 * @author Mahesh.Kannan@Sun.Com
 *
 */
public class ExpressionBuilder<V> {

    Class<V> entryClazz;

    public ExpressionBuilder(Class<V> entryClazz) {
        this.entryClazz = entryClazz;
    }

    public Criteria<V> setCriteria(Expression<Boolean> expr) {
        Criteria<V> c = new Criteria<V>(entryClazz);
        c.setExpression(expr);

        return c;
    }

    public <T> AttributeAccessNode<V, T> attr(AttributeMetadata<V, T> meta) {
        return new AttributeAccessNode<V, T>(meta);
    }

    public <T> LiteralNode<T> literal(Class<T> type, T value) {
        return new LiteralNode<T>(type, value);
    }

    public <T> LogicalExpressionNode eq(T value, AttributeMetadata<V, T> meta) {
        return new LogicalExpressionNode(Opcode.EQ,
                new LiteralNode<T>(meta.getAttributeType(), value),
                new AttributeAccessNode<V, T>(meta));
    }

    public <T> LogicalExpressionNode eq(AttributeMetadata<V, T> meta, T value) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta),
                new LiteralNode<T>(meta.getAttributeType(), value));
    }

    public <T> LogicalExpressionNode eq(AttributeMetadata<V, T> meta1,
                                           AttributeMetadata<V, T> meta2) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta1),
                new AttributeAccessNode<V, T>(meta2));
    }

    public <T> LogicalExpressionNode eq(ExpressionNode<T> expr1, ExpressionNode<T> expr2) {
        return new LogicalExpressionNode(Opcode.EQ, expr1, expr2);
    }

    public <T extends Number> LogicalExpressionNode eq(LiteralNode<T> value, AttributeMetadata<V, T> meta) {
        return new LogicalExpressionNode(Opcode.EQ,
                value, new AttributeAccessNode<V, T>(meta));
    }

    public <T extends Number> LogicalExpressionNode eq(AttributeMetadata<V, T> meta, LiteralNode<T> value) {
        return new LogicalExpressionNode(Opcode.EQ,
                new AttributeAccessNode<V, T>(meta), value);
    }

}
