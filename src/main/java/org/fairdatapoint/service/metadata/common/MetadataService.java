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
package org.fairdatapoint.service.metadata.common;

import org.fairdatapoint.database.rdf.repository.RepositoryMode;
import org.fairdatapoint.entity.exception.ResourceNotFoundException;
import org.fairdatapoint.entity.resource.ResourceDefinition;
import org.fairdatapoint.service.metadata.exception.MetadataServiceException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import java.util.List;

public interface MetadataService {

    Model retrieve(IRI uri) throws MetadataServiceException, ResourceNotFoundException;

    List<Model> retrieve(List<IRI> uri) throws MetadataServiceException, ResourceNotFoundException;

    Model retrieve(IRI uri, RepositoryMode mode) throws MetadataServiceException, ResourceNotFoundException;

    List<Model> retrieve(List<IRI> uri, RepositoryMode mode) throws MetadataServiceException, ResourceNotFoundException;

    Model store(
            Model metadata, IRI uri, ResourceDefinition resourceDefinition
    ) throws MetadataServiceException;

    Model update(
            Model model, IRI uri, ResourceDefinition resourceDefinition, boolean validate
    ) throws MetadataServiceException;

    void delete(IRI uri, ResourceDefinition resourceDefinition) throws MetadataServiceException;

}
