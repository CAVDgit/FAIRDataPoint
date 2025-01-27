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
package org.fairdatapoint.service.index.webhook;

import org.fairdatapoint.entity.index.event.IndexEvent;
import org.fairdatapoint.entity.index.http.Exchange;
import org.fairdatapoint.entity.index.http.ExchangeDirection;
import org.fairdatapoint.entity.index.http.ExchangeState;
import org.fairdatapoint.entity.index.webhook.IndexWebhook;
import org.fairdatapoint.entity.index.webhook.IndexWebhookEvent;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

public class WebhookUtils {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static boolean webhookMatches(
            IndexWebhook webhook, IndexWebhookEvent webhookEvent, IndexEvent triggerEvent
    ) {
        final boolean matchEvent = webhook.getAllEvents()
                || webhook.getEvents().contains(webhookEvent);
        final boolean matchEntry =
                webhook.getAllEntries()
                        || triggerEvent.getRelatedTo() == null
                        || webhook.getEntries().contains(
                                triggerEvent.getRelatedTo().getClientUrl());
        return matchEvent && matchEntry && webhook.getEnabled();
    }

    public static Stream<IndexWebhook> filterMatching(List<IndexWebhook> webhooks, IndexWebhookEvent webhookEvent,
                                                      IndexEvent triggerEvent) {
        return webhooks
                .parallelStream()
                .filter(webhook -> {
                    return WebhookUtils.webhookMatches(webhook, webhookEvent, triggerEvent);
                });
    }

    public static String computeHashSignature(String value) throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        return String.format("sha1=%040x", new BigInteger(1, digest.digest()));
    }

    public static void postWebhook(
            IndexEvent event, Duration timeout, String payload, String signature
    ) {
        final Exchange ex = new Exchange(ExchangeDirection.OUTGOING);
        event.getPayload().getWebhookTrigger().setExchange(ex);
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(event.getPayload().getWebhookTrigger().getWebhook().getPayloadUrl()))
                    .timeout(timeout)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .header("X-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            ex.getRequest().setFromHttpRequest(request);
            ex.setState(ExchangeState.Requested);
            final HttpResponse<String> response =
                    CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            ex.getResponse().setFromHttpResponse(response);
            ex.setState(ExchangeState.Retrieved);
        }
        catch (InterruptedException exception) {
            ex.setState(ExchangeState.Timeout);
            ex.setError("Timeout");
        }
        catch (IllegalArgumentException exception) {
            ex.setState(ExchangeState.Failed);
            ex.setError("Invalid URI: " + exception.getMessage());
        }
        catch (IOException exception) {
            ex.setState(ExchangeState.Failed);
            ex.setError("IO error: " + exception.getMessage());
        }
    }
}
