/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.repository.query;

import com.microsoft.azure.spring.data.cosmosdb.core.CosmosOperations;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

public abstract class AbstractCosmosQuery implements RepositoryQuery {

    private final CosmosQueryMethod method;
    private final CosmosOperations operations;

    public AbstractCosmosQuery(CosmosQueryMethod method, CosmosOperations operations) {
        this.method = method;
        this.operations = operations;
    }

    public Object execute(Object[] parameters) {
        final CosmosParameterAccessor accessor = new CosmosParameterParameterAccessor(method, parameters);
        final DocumentQuery query = createQuery(accessor);

        final ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
        final String container = ((CosmosEntityMetadata) method.getEntityInformation()).getContainerName();

        final CosmosQueryExecution execution = getExecution(accessor, processor.getReturnedType());
        return execution.execute(query, processor.getReturnedType().getDomainType(), container);
    }


    private CosmosQueryExecution getExecution(CosmosParameterAccessor accessor, ReturnedType returnedType) {
        if (isDeleteQuery()) {
            return new CosmosQueryExecution.DeleteExecution(operations);
        } else if (method.isPageQuery()) {
            return new CosmosQueryExecution.PagedExecution(operations, accessor.getPageable());
        } else if (isExistsQuery()) {
            return new CosmosQueryExecution.ExistsExecution(operations);
        } else if (method.isCollectionQuery()) {
            return new CosmosQueryExecution.MultiEntityExecution(operations);
        } else {
            return new CosmosQueryExecution.SingleEntityExecution(operations, returnedType);
        }
    }

    public CosmosQueryMethod getQueryMethod() {
        return method;
    }

    protected abstract DocumentQuery createQuery(CosmosParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

    protected abstract boolean isExistsQuery();

}
