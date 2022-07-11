/**
 * The MIT License
 * Copyright © 2017 DTL
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package nl.dtls.fairdatapoint.service.search;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import nl.dtls.fairdatapoint.api.dto.search.*;
import nl.dtls.fairdatapoint.database.rdf.repository.exception.MetadataRepositoryException;
import nl.dtls.fairdatapoint.database.rdf.repository.generic.GenericMetadataRepository;
import nl.dtls.fairdatapoint.entity.exception.ResourceNotFoundException;
import nl.dtls.fairdatapoint.entity.metadata.MetadataState;
import nl.dtls.fairdatapoint.entity.search.SearchFilterCacheContainer;
import nl.dtls.fairdatapoint.entity.search.SearchFilterType;
import nl.dtls.fairdatapoint.entity.search.SearchFilterValue;
import nl.dtls.fairdatapoint.entity.search.SearchResult;
import nl.dtls.fairdatapoint.entity.settings.SettingsSearchFilter;
import nl.dtls.fairdatapoint.service.metadata.state.MetadataStateService;
import nl.dtls.fairdatapoint.service.settings.SettingsService;
import org.apache.commons.lang.text.StrSubstitutor;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static nl.dtls.fairdatapoint.util.ValueFactoryHelper.i;
import static nl.dtls.fairdatapoint.util.ValueFactoryHelper.l;

@Service
public class SearchService {

    private static final String QUERY_TEMPLATE_NAME = "queryTemplate.sparql";

    private static final String QUERY_TEMPLATE = loadSparqlQueryTemplate();

    @Autowired
    private GenericMetadataRepository metadataRepository;

    @Autowired
    private MetadataStateService metadataStateService;

    @Autowired
    private SearchMapper searchMapper;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private SearchFilterCache searchFilterCache;

    @Autowired
    @Qualifier("persistentUrl")
    private String persistentUrl;

    public List<SearchResultDTO> search(SearchQueryDTO reqDto) throws MetadataRepositoryException {
        List<SearchResult> results = metadataRepository.findByLiteral(l(reqDto.getQ()));
        return processSearchResults(results);
    }

    public SearchQueryTemplateDTO getSearchQueryTemplate() {
        return searchMapper.toQueryTemplateDTO(QUERY_TEMPLATE);
    }

    public List<SearchResultDTO> search(SearchQueryVariablesDTO reqDto) throws MetadataRepositoryException, MalformedQueryException {
        // Compose query
        String query = composeQuery(reqDto);
        // Verify query
        SPARQLParser parser = new SPARQLParser();
        parser.parseQuery(query, persistentUrl);
        // Get and process results for query
        List<SearchResult> results = metadataRepository.findBySparqlQuery(query);
        return processSearchResults(results);
    }

    public List<SearchFilterDTO> getSearchFilters() {
        return settingsService
                .getOrDefaults()
                .getSearchFilters()
                .parallelStream()
                .map(this::enrichItems)
                .toList();
    }

    public List<SearchFilterDTO> resetSearchFilters() {
        searchFilterCache.clearCache();
        return getSearchFilters();
    }

    private SearchFilterDTO enrichItems(SettingsSearchFilter filter) {
        SearchFilterDTO result = searchMapper.toFilterDTO(filter);
        Set<String> values = result.getValues().stream().map(SearchFilterItemDTO::getValue).collect(Collectors.toSet());
        if (filter.isQueryFromRecords()) {
            List<SearchFilterItemDTO> xvalues = new ArrayList<>();
            xvalues.addAll(result.getValues());
            xvalues.addAll(
                queryFilterItems(filter.getPredicate())
                    .stream()
                    .filter(v -> !values.contains(v.getValue()))
                    .map(searchMapper::toFilterItemDTO)
                    .toList()
            );
            if (filter.getType().equals(SearchFilterType.IRI)) {
                searchFilterCache.updateLabels(filter.getPredicate());
            }
            result.setValues(xvalues);
        }
        return result;
    }

    private List<SearchFilterValue> queryFilterItems(String predicate) {
        // TODO: filter related to DRAFT records
        SearchFilterCacheContainer cacheContainer = searchFilterCache.getFilter(predicate);
        if (cacheContainer != null) {
            return cacheContainer.getValues();
        }
        try {
            List<SearchFilterValue> result = metadataRepository.findByFilterPredicate(i(predicate));
            searchFilterCache.setFilter(predicate, new SearchFilterCacheContainer(result));
            return result;
        } catch (MetadataRepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private List<SearchResultDTO> processSearchResults(List<SearchResult> results) {
        Map<String, SearchResultDTO> resultDtos = results
                .stream()
                .collect(Collectors.groupingBy(SearchResult::getUri, Collectors.mapping(i -> i, toList())))
                .entrySet()
                .parallelStream()
                .filter(entry -> {
                    try {
                        return !metadataStateService.get(i(entry.getKey())).getState().equals(MetadataState.DRAFT);
                    } catch (ResourceNotFoundException e) {
                        return true;
                    }
                })
                .map(entry -> searchMapper.toResultDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(SearchResultDTO::getUri, Function.identity()));
        return results
                .stream()
                .map(SearchResult::getUri)
                .distinct()
                .map(resultDtos::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private String composeQuery(SearchQueryVariablesDTO reqDto) {
        StrSubstitutor substitutor = new StrSubstitutor(searchMapper.toSubstitutions(reqDto), "{{", "}}");
        return substitutor.replace(QUERY_TEMPLATE);
    }

    private static String loadSparqlQueryTemplate() {
        try {
            URL fileURL = SearchService.class.getResource(QUERY_TEMPLATE_NAME);
            return Resources.toString(fileURL, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(format("Cannot load SPARQL template for search: %s", e.getMessage()));
        }
    }
}
