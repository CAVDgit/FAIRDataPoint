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
package org.fairdatapoint.acceptance.index.admin;

import org.fairdatapoint.WebIntegrationTest;
import org.fairdatapoint.database.db.repository.IndexEntryRepository;
import org.fairdatapoint.database.db.repository.IndexEventRepository;
import org.fairdatapoint.entity.index.event.IndexEvent;
import org.fairdatapoint.entity.index.event.IndexEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@DisplayName("POST /index/admin/trigger-all")
public class List_TriggerAll_POST extends WebIntegrationTest {

    @Autowired
    private IndexEventRepository eventRepository;

    @Autowired
    private IndexEntryRepository indexEntryRepository;

    private final ParameterizedTypeReference<Void> responseType = new ParameterizedTypeReference<>() {
    };

    private URI url() {
        return URI.create("/index/admin/trigger-all");
    }

    @Test
    @DisplayName("HTTP 403: no token")
    public void res403_noToken() {
        // GIVEN
        RequestEntity<Void> request = RequestEntity
                .post(url())
                .build();

        // WHEN
        ResponseEntity<Void> result = client.exchange(request, responseType);

        // THEN: 
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
    }

    @Test
    @DisplayName("HTTP 403: incorrect token")
    public void res403_incorrectToken() {
        // GIVEN
        RequestEntity<Void> request = RequestEntity
                .post(url())
                .header(HttpHeaders.AUTHORIZATION, "mySecretToken")
                .build();

        // WHEN
        ResponseEntity<Void> result = client.exchange(request, responseType);

        // THEN: 
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
    }

    @Test
    @DisplayName("HTTP 403: non-admin token")
    public void res403_nonAdminToken() {
        // GIVEN
        RequestEntity<Void> request = RequestEntity
                .post(url())
                .header(HttpHeaders.AUTHORIZATION, ALBERT_TOKEN)
                .build();

        // WHEN
        ResponseEntity<Void> result = client.exchange(request, responseType);

        // THEN: 
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
    }

    @Test
    @DisplayName("HTTP 204: trigger all")
    public void res204_triggerAll() {
        // GIVEN: prepare request
        RequestEntity<Void> request = RequestEntity
                .post(url())
                .header(HttpHeaders.AUTHORIZATION, ADMIN_TOKEN)
                .build();

        // WHEN
        ResponseEntity<Void> result = client.exchange(request, responseType);
        List<IndexEvent> events = eventRepository.getAllByType(IndexEventType.ADMIN_TRIGGER);

        // THEN: 
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.NO_CONTENT)));
        assertThat("One AdminTrigger event is created", events.size(), is(equalTo(1)));
        assertThat("Records correct client URL as null", events.get(0).getPayload().getAdminTrigger().getClientUrl(), is(equalTo(null)));
    }
}
