package com.redhat.service.bridge.manager.dao;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

import static java.util.Collections.emptyList;

@ApplicationScoped
@Transactional
public class ProcessorDAO implements PanacheRepositoryBase<Processor, String> {

    /*
     * NOTE: the Processor queries that use a left join on the filters **MUST** be wrapped by the method `removeDuplicates`!
     * see https://developer.jboss.org/docs/DOC-15782#
     * jive_content_id_Hibernate_does_not_return_distinct_results_for_a_query_with_outer_join_fetching_enabled_for_a_collection_even_if_I_use_the_distinct_keyword
     */

    private static final String IDS_PARAM = "ids";

    public Processor findByBridgeIdAndName(String bridgeId, String name) {
        Parameters p = Parameters.with(Processor.NAME_PARAM, name).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return singleResultFromList(find("#PROCESSOR.findByBridgeIdAndName", p));
    }

    /*
     * For queries where we need to fetch join associations, this works around the fact that Hibernate has to
     * apply pagination in-memory _if_ we rely on Panaches .firstResult() or firstResultOptional() methods. This
     * manifests as "HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!" in the log
     * 
     * This performs the query as if we expect a list result, but then converts the list into a single result
     * response: either the entity if the list has a single result, or null if not.
     *
     * More than 1 entity in the list throws an IllegalStateException as it's not something that we expect to happen
     * 
     */
    private Processor singleResultFromList(PanacheQuery<Processor> find) {
        List<Processor> processors = removeDuplicates(find.list());
        if (processors.size() > 1) {
            throw new IllegalStateException("Multiple Entities returned from a Query that should only return a single Entity");
        }
        return processors.size() == 1 ? processors.get(0) : null;
    }

    public Processor findByIdBridgeIdAndCustomerId(String id, String bridgeId, String customerId) {

        Parameters p = Parameters.with(Processor.ID_PARAM, id)
                .and(Processor.BRIDGE_ID_PARAM, bridgeId)
                .and(Bridge.CUSTOMER_ID_PARAM, customerId);

        return singleResultFromList(find("#PROCESSOR.findByIdBridgeIdAndCustomerId", p));
    }

    public List<Processor> findByStatuses(List<BridgeStatus> statuses) {
        Parameters p = Parameters.with("statuses", statuses);
        return removeDuplicates(find("#PROCESSOR.findByStatus", p).list());
    }

    private Long countProcessorsOnBridge(Parameters params) {
        TypedQuery<Long> namedQuery = getEntityManager().createNamedQuery("PROCESSOR.countByBridgeIdAndCustomerId", Long.class);
        addParamsToNamedQuery(params, namedQuery);
        return namedQuery.getSingleResult();
    }

    private void addParamsToNamedQuery(Parameters params, TypedQuery<?> namedQuery) {
        params.map().forEach((key, value) -> namedQuery.setParameter(key, value.toString()));
    }

    public ListResult<Processor> findByBridgeIdAndCustomerId(String bridgeId, String customerId, int page, int size) {

        /*
         * Unfortunately we can't rely on Panaches in-built Paging due the fetched join in our query
         * for Processor e.g. join fetch p.bridge. Instead, we simply build a list of ids to fetch and then
         * execute the join fetch as normal. So the workflow here is:
         *
         * - Count the number of Processors on a bridge. If > 0
         * - Select the ids of the Processors that need to be retrieved based on the page/size requirements
         * - Select the Processors in the list of ids, performing the fetch join of the Bridge
         */

        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        Long processorCount = countProcessorsOnBridge(p);
        if (processorCount == 0L) {
            return new ListResult<>(emptyList(), page, processorCount);
        }

        int firstResult = getFirstResult(page, size);
        TypedQuery<String> idsQuery = getEntityManager().createNamedQuery("PROCESSOR.idsByBridgeIdAndCustomerId", String.class);
        addParamsToNamedQuery(p, idsQuery);
        List<String> ids = idsQuery.setMaxResults(size).setFirstResult(firstResult).getResultList();

        /*
         * We have to include the Action in the select list, so this returns both the Processor and Action as a pair. We only
         * want the Processor.
         */
        List<Object[]> results = getEntityManager().createNamedQuery("PROCESSOR.findByIds").setParameter(IDS_PARAM, ids).getResultList();
        List<Processor> processors = removeDuplicates(results.stream().map((o) -> (Processor) o[0]).collect(Collectors.toList()));
        return new ListResult<>(processors, page, processorCount);
    }

    public Long countByBridgeIdAndCustomerId(String bridgeId, String customerId) {
        Parameters p = Parameters.with(Bridge.CUSTOMER_ID_PARAM, customerId).and(Processor.BRIDGE_ID_PARAM, bridgeId);
        return countProcessorsOnBridge(p);
    }

    private int getFirstResult(int requestedPage, int requestedPageSize) {
        if (requestedPage <= 0) {
            return 0;
        }

        return requestedPage * requestedPageSize;
    }

    private List<Processor> removeDuplicates(List<Processor> processors) {
        return new ArrayList<>(new LinkedHashSet<>(processors));
    }
}
