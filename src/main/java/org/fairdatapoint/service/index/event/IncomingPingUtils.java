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
package org.fairdatapoint.service.index.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.fairdatapoint.api.dto.index.ping.PingDTO;
import org.fairdatapoint.entity.index.event.IndexEvent;
import org.fairdatapoint.entity.index.event.payload.IncomingPing;
import org.fairdatapoint.entity.index.http.Exchange;
import org.fairdatapoint.entity.index.http.ExchangeDirection;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IncomingPingUtils {

    private static final Integer VERSION = 1;

    private final ObjectMapper objectMapper;

    public IndexEvent prepareEvent(PingDTO reqDto, HttpServletRequest request, String remoteAddr) {
        final IncomingPing incomingPing = new IncomingPing();
        final Exchange ex = new Exchange(ExchangeDirection.INCOMING, remoteAddr);
        incomingPing.setExchange(ex);

        ex.getRequest().setHeaders(getHeaders(request));
        ex.getRequest().setFromHttpServletRequest(request);
        try {
            ex.getRequest().setBody(objectMapper.writeValueAsString(reqDto));
        }
        catch (JsonProcessingException exception) {
            ex.getRequest().setBody(null);
        }

        return new IndexEvent(VERSION, incomingPing);
    }

    private Map<String, List<String>> getHeaders(HttpServletRequest request) {
        final Map<String, List<String>> map = new HashMap<>();
        final Iterator<String> requestI = request.getHeaderNames().asIterator();
        while (requestI.hasNext()) {
            final String headerName = requestI.next();
            final List<String> headerValues = new ArrayList<>();
            final Iterator<String> headerI = request.getHeaders(headerName).asIterator();
            while (headerI.hasNext()) {
                headerValues.add(headerI.next());
            }
            map.put(headerName, headerValues);
        }
        return map;
    }

}
