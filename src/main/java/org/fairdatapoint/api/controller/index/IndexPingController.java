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
package org.fairdatapoint.api.controller.index;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fairdatapoint.api.dto.index.ping.PingDTO;
import org.fairdatapoint.database.rdf.repository.exception.MetadataRepositoryException;
import org.fairdatapoint.entity.index.event.IndexEvent;
import org.fairdatapoint.service.UtilityService;
import org.fairdatapoint.service.index.entry.IndexEntryService;
import org.fairdatapoint.service.index.event.EventService;
import org.fairdatapoint.service.index.webhook.WebhookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Index")
@Slf4j
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexPingController {

    private final EventService eventService;

    private final WebhookService webhookService;

    private final IndexEntryService indexEntryService;

    private final UtilityService utilityService;

    @Operation(
            description = "Inform about running FAIR Data Point. It is expected to "
                    + "send pings regularly (at least weekly). There is a rate limit set "
                    + "both per single IP within a period of time and per URL in message.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Ping payload with FAIR Data Point info",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                @ExampleObject(value = "{\"clientUrl\": \"https://example.com\"}")
                            },
                            schema = @Schema(
                                    type = "object",
                                    title = "Ping",
                                    implementation = PingDTO.class
                            )
                    )
            ),
            responses = {
                @ApiResponse(responseCode = "204", description = "Ping accepted (no content)"),
                @ApiResponse(responseCode = "400", description = "Invalid ping format"),
                @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
            }
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> receivePing(
            @RequestBody @Valid PingDTO reqDto,
            HttpServletRequest request
    ) throws MetadataRepositoryException {
        log.info("Received ping from {}", utilityService.getRemoteAddr(request));
        final IndexEvent event = eventService.acceptIncomingPing(reqDto, request);
        log.info("Triggering metadata retrieval for {}", event.getRelatedTo().getClientUrl());
        eventService.triggerMetadataRetrieval(event);
        indexEntryService.harvest(reqDto.getClientUrl());
        webhookService.triggerWebhooks(event);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
