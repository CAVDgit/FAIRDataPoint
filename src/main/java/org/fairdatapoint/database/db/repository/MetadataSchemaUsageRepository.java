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
package org.fairdatapoint.database.db.repository;

import org.fairdatapoint.entity.resource.MetadataSchemaUsage;
import org.fairdatapoint.entity.resource.ResourceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MetadataSchemaUsageRepository extends JpaRepository<MetadataSchemaUsage, UUID> {

    List<MetadataSchemaUsage> findAllByResourceDefinition(ResourceDefinition resourceDefinition);

    @Query(
        """
            SELECT e.uuid as uuid, rd.uuid as resourceDefinitionUuid,
                   e.usedMetadataSchema.uuid as schemaUuid,
                   rd.name as resourceDefinitionName
            FROM MetadataSchemaUsage e JOIN ResourceDefinition rd ON e.resourceDefinition.uuid = rd.uuid
        """
    )
    Iterable<MetadataSchemaUsageBasic> getBasicUsages();

    interface MetadataSchemaUsageBasic {
        UUID getUuid();

        UUID getResourceDefinitionUuid();

        UUID getSchemaUuid();

        UUID getResourceDefinitionName();
    }

}
