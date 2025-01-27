/**
 * The MIT License
 * Copyright © 2016-2024 FAIR Data Team
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
package org.fairdatapoint.acceptance.metadata.dataset.meta;

import org.fairdatapoint.WebIntegrationTest;
import org.fairdatapoint.api.dto.metadata.MetaStateChangeDTO;
import org.fairdatapoint.database.rdf.repository.exception.MetadataRepositoryException;
import org.fairdatapoint.database.rdf.repository.generic.GenericMetadataRepository;
import org.fairdatapoint.entity.metadata.MetadataState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static java.lang.String.format;
import static org.fairdatapoint.acceptance.common.ForbiddenTest.createNoUserForbiddenTestPut;
import static org.fairdatapoint.acceptance.metadata.Common.createMetadataStateAlreadyPublished;
import static org.fairdatapoint.acceptance.metadata.Common.createMetadataStateChangeToDraft;
import static org.fairdatapoint.util.ValueFactoryHelper.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@DisplayName("PUT /dataset/:datasetId/meta/state")
public class List_State_PUT extends WebIntegrationTest {

    @Autowired
    @Qualifier("persistentUrl")
    private String persistentUrl;

    @Autowired
    private GenericMetadataRepository repository;

    private URI url(String id) {
        return URI.create(format("/dataset/%s/meta/state", id));
    }

    private MetaStateChangeDTO reqDto() {
        return new MetaStateChangeDTO(MetadataState.PUBLISHED);
    }

    @Test
    @DisplayName("HTTP 200")
    public void res200() throws MetadataRepositoryException {
        // GIVEN:
        RequestEntity<MetaStateChangeDTO> request = RequestEntity
                .put(url("dataset-1"))
                .header(HttpHeaders.AUTHORIZATION, ALBERT_TOKEN)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(reqDto());
        ParameterizedTypeReference<MetaStateChangeDTO> responseType = new ParameterizedTypeReference<>() {
        };

        // AND: Prepare database (make it a draft)
        repository.moveToDrafts(i(persistentUrl + "/dataset/dataset-1"));

        // WHEN:
        ResponseEntity<MetaStateChangeDTO> result = client.exchange(request, responseType);

        // THEN:
        assertThat(result.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat(result.getBody(), is(equalTo(reqDto())));
    }

    @Test
    @DisplayName("HTTP 400: Metadata is already published")
    public void res400_already_published() throws MetadataRepositoryException {
        repository.moveToMain(i(persistentUrl + "/dataset/dataset-1"));
        createMetadataStateAlreadyPublished(client, url("dataset-1"));
    }

    @Test
    @DisplayName("HTTP 400: You can not change state to DRAFT")
    public void res400_change_to_draft() throws MetadataRepositoryException {
        repository.moveToDrafts(i(persistentUrl + "/dataset/dataset-1"));
        createMetadataStateChangeToDraft(client, url("dataset-1"));
    }

    @Test
    @DisplayName("HTTP 403: User is not authenticated")
    public void res403_notAuthenticated() {
        createNoUserForbiddenTestPut(client, url("dataset-1"), reqDto());
    }

}
