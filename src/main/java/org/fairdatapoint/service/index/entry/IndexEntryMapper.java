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
package org.fairdatapoint.service.index.entry;

import org.fairdatapoint.api.dto.index.entry.IndexEntryDTO;
import org.fairdatapoint.api.dto.index.entry.IndexEntryDetailDTO;
import org.fairdatapoint.api.dto.index.entry.IndexEntryStateDTO;
import org.fairdatapoint.entity.index.entry.IndexEntry;
import org.fairdatapoint.entity.index.entry.IndexEntryState;
import org.fairdatapoint.entity.index.entry.RepositoryMetadata;
import org.fairdatapoint.entity.index.event.IndexEvent;
import org.fairdatapoint.service.index.event.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.StreamSupport;

@Service
public class IndexEntryMapper {

    @Autowired
    private EventMapper eventMapper;

    public IndexEntryDTO toDTO(IndexEntry indexEntry, Instant validThreshold) {
        return new IndexEntryDTO(
                indexEntry.getUuid(),
                indexEntry.getClientUrl(),
                toStateDTO(indexEntry.getState(),
                        indexEntry.getLastRetrievalAt(),
                        validThreshold),
                indexEntry.getPermit(),
                indexEntry.getCreatedAt().toString(),
                indexEntry.getUpdatedAt().toString()
        );
    }

    public IndexEntryDetailDTO toDetailDTO(
            IndexEntry indexEntry, Iterable<IndexEvent> events, Instant validThreshold
    ) {
        return new IndexEntryDetailDTO(
                indexEntry.getUuid(),
                indexEntry.getClientUrl(),
                toStateDTO(indexEntry.getState(),
                        indexEntry.getLastRetrievalAt(),
                        validThreshold),
                indexEntry.getPermit(),
                toRepositoryMetadata(indexEntry),
                StreamSupport.stream(events.spliterator(), false)
                        .map(eventMapper::toDTO)
                        .toList(),
                indexEntry.getCreatedAt().toString(),
                indexEntry.getUpdatedAt().toString(),
                indexEntry.getLastRetrievalAt().toString()
        );
    }

    public IndexEntryStateDTO toStateDTO(
            IndexEntryState state, Instant lastRetrievalTime, Instant validThreshold
    ) {
        return switch (state) {
            case UNKNOWN -> IndexEntryStateDTO.UNKNOWN;
            case VALID -> lastRetrievalTime.isAfter(validThreshold)
                    ? IndexEntryStateDTO.ACTIVE
                    : IndexEntryStateDTO.INACTIVE;
            case INVALID -> IndexEntryStateDTO.INVALID;
            case UNREACHABLE -> IndexEntryStateDTO.UNREACHABLE;
        };
    }

    private RepositoryMetadata toRepositoryMetadata(IndexEntry entry) {
        return RepositoryMetadata.builder()
                .repositoryUri(entry.getRepositoryUri())
                .metadataVersion(entry.getMetadataVersion())
                .metadata(entry.getMetadata())
                .build();
    }

}
