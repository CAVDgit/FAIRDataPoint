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
package org.fairdatapoint.acceptance.index.settings;

import org.fairdatapoint.WebIntegrationTest;
import org.fairdatapoint.api.dto.index.settings.IndexSettingsDTO;
import org.fairdatapoint.database.db.repository.IndexSettingsRepository;
import org.fairdatapoint.entity.index.settings.IndexSettings;
import org.fairdatapoint.entity.index.settings.SettingsIndexPing;
import org.fairdatapoint.entity.index.settings.SettingsIndexRetrieval;
import org.fairdatapoint.service.index.settings.IndexSettingsDefaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

@DisplayName("DELETE /index/settings")
public class List_DELETE extends WebIntegrationTest {

    @Autowired
    private IndexSettingsRepository indexSettingsRepository;

    @Autowired
    private IndexSettingsDefaults indexSettingsDefaults;

    private final ParameterizedTypeReference<IndexSettingsDTO> responseType =
            new ParameterizedTypeReference<>() {
            };

    private URI url() {
        return URI.create("/index/settings");
    }

    private IndexSettings customSettings(IndexSettings indexSettings) {
        indexSettings.setPing(
                SettingsIndexPing.builder()
                        .denyList(Collections.singletonList("http://localhost.*$"))
                        .rateLimitDuration(Duration.ofMinutes(17))
                        .validDuration(Duration.ofDays(5))
                        .rateLimitHits(666)
                        .build()
        );
        indexSettings.setRetrieval(
                SettingsIndexRetrieval.builder()
                        .rateLimitWait(Duration.ofHours(16))
                        .timeout(Duration.ofSeconds(55))
                        .build()
        );
        indexSettings.setAutoPermit(false);
        return indexSettings;
    }

    @Test
    @DisplayName("HTTP 200: default settings")
    public void res200_defaultSettings() {
        // GIVEN: prepare data
        IndexSettings settings = indexSettingsDefaults.getDefaults();
        indexSettingsRepository.deleteAll();

        // AND: prepare request
        RequestEntity<?> request = RequestEntity
                .delete(url())
                .header(HttpHeaders.AUTHORIZATION, ADMIN_TOKEN)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        // WHEN
        ResponseEntity<IndexSettingsDTO> result = client.exchange(request, responseType);

        // THEN
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat("Response body is not null", result.getBody(), is(notNullValue()));
        assertThat("Response contains default valid duration", Objects.requireNonNull(result.getBody()).getPing().getValidDuration(), is(equalTo(settings.getPing().getValidDuration().toString())));
        assertThat("Response contains default rate limit duration", Objects.requireNonNull(result.getBody()).getPing().getRateLimitDuration(), is(equalTo(settings.getPing().getRateLimitDuration().toString())));
        assertThat("Response contains default rate limit hits", Objects.requireNonNull(result.getBody()).getPing().getRateLimitHits(), is(equalTo(settings.getPing().getRateLimitHits())));
        assertThat("Response contains default deny list", Objects.requireNonNull(result.getBody()).getPing().getDenyList(), is(equalTo(settings.getPing().getDenyList())));
        assertThat("Response contains default timeout", Objects.requireNonNull(result.getBody()).getRetrieval().getTimeout(), is(equalTo(settings.getRetrieval().getTimeout().toString())));
        assertThat("Response contains default rate limit wait", Objects.requireNonNull(result.getBody()).getRetrieval().getRateLimitWait(), is(equalTo(settings.getRetrieval().getRateLimitWait().toString())));
        assertThat("Response indicated default settings", Objects.requireNonNull(result.getBody()).getIsDefault(), is(Boolean.TRUE));
    }

    @Test
    @DisplayName("HTTP 200: custom settings")
    public void res200_customSettings() {
        // GIVEN: prepare data
        IndexSettings settings = indexSettingsDefaults.getDefaults();
        IndexSettings newSettings = customSettings(indexSettingsDefaults.getDefaults());
        indexSettingsRepository.saveAndFlush(newSettings);

        // AND: prepare request
        RequestEntity<?> request = RequestEntity
                .delete(url())
                .header(HttpHeaders.AUTHORIZATION, ADMIN_TOKEN)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        // WHEN
        ResponseEntity<IndexSettingsDTO> result = client.exchange(request, responseType);

        // THEN
        assertThat("Correct response code is received", result.getStatusCode(), is(equalTo(HttpStatus.OK)));
        assertThat("Response body is not null", result.getBody(), is(notNullValue()));
        assertThat("Response contains default valid duration", Objects.requireNonNull(result.getBody()).getPing().getValidDuration(), is(equalTo(settings.getPing().getValidDuration().toString())));
        assertThat("Response contains default rate limit duration", Objects.requireNonNull(result.getBody()).getPing().getRateLimitDuration(), is(equalTo(settings.getPing().getRateLimitDuration().toString())));
        assertThat("Response contains default rate limit hits", Objects.requireNonNull(result.getBody()).getPing().getRateLimitHits(), is(equalTo(settings.getPing().getRateLimitHits())));
        assertThat("Response contains default deny list", Objects.requireNonNull(result.getBody()).getPing().getDenyList(), is(equalTo(settings.getPing().getDenyList())));
        assertThat("Response contains default timeout", Objects.requireNonNull(result.getBody()).getRetrieval().getTimeout(), is(equalTo(settings.getRetrieval().getTimeout().toString())));
        assertThat("Response contains default rate limit wait", Objects.requireNonNull(result.getBody()).getRetrieval().getRateLimitWait(), is(equalTo(settings.getRetrieval().getRateLimitWait().toString())));
        assertThat("Response indicated default settings", Objects.requireNonNull(result.getBody()).getIsDefault(), is(Boolean.TRUE));
    }

    @Test
    @DisplayName("HTTP 403: no token")
    public void res403_noToken() {
        // GIVEN
        RequestEntity<?> request = RequestEntity
                .delete(url())
                .accept(MediaType.APPLICATION_JSON)
                .build();

        // WHEN
        ResponseEntity<IndexSettingsDTO> result = client.exchange(request, responseType);

        // THEN
        assertThat("It is forbidden without auth", result.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
    }

    @Test
    @DisplayName("HTTP 403: not admin")
    public void res403_notAdmin() {
        // GIVEN
        RequestEntity<?> request = RequestEntity
                .delete(url())
                .header(HttpHeaders.AUTHORIZATION, NIKOLA_TOKEN)
                .accept(MediaType.APPLICATION_JSON)
                .build();

        // WHEN
        ResponseEntity<IndexSettingsDTO> result = client.exchange(request, responseType);

        // THEN
        assertThat("It is forbidden for non-admin users", result.getStatusCode(), is(equalTo(HttpStatus.FORBIDDEN)));
    }
}
