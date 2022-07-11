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
package nl.dtls.fairdatapoint.service.settings;

import nl.dtls.fairdatapoint.api.dto.search.SearchFilterDTO;
import nl.dtls.fairdatapoint.api.dto.search.SearchFilterItemDTO;
import nl.dtls.fairdatapoint.api.dto.settings.*;
import nl.dtls.fairdatapoint.config.properties.InstanceProperties;
import nl.dtls.fairdatapoint.config.properties.PingProperties;
import nl.dtls.fairdatapoint.config.properties.RepositoryProperties;
import nl.dtls.fairdatapoint.database.mongo.repository.SettingsRepository;
import nl.dtls.fairdatapoint.entity.settings.Settings;
import nl.dtls.fairdatapoint.entity.settings.SettingsPing;
import nl.dtls.fairdatapoint.entity.settings.SettingsSearchFilter;
import nl.dtls.fairdatapoint.entity.settings.SettingsSearchFilterItem;
import nl.dtls.fairdatapoint.service.search.SearchMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SettingsMapper {

    @Autowired
    private InstanceProperties instanceProperties;

    @Autowired
    private PingProperties pingProperties;

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Autowired
    private SearchMapper searchMapper;

    public SettingsDTO toDTO(Settings settings) {
        return SettingsDTO
                .builder()
                .clientUrl(instanceProperties.getClientUrl())
                .persistentUrl(instanceProperties.getPersistentUrl())
                .metadataMetrics(settings.getMetadataMetrics())
                .ping(toDTO(settings.getPing()))
                .repository(getRepositoryDTO())
                .search(
                        SettingsSearchDTO
                                .builder()
                                .filters(settings.getSearchFilters().stream().map(this::toDTO).toList())
                                .build()
                )
                .build();
    }

    public SearchFilterDTO toDTO(SettingsSearchFilter filter) {
        return SearchFilterDTO
                .builder()
                .type(filter.getType())
                .label(filter.getLabel())
                .predicate(filter.getPredicate())
                .values(filter.getPresetValues().stream().map(this::toDTO).toList())
                .queryFromRecords(filter.isQueryFromRecords())
                .build();
    }

    public SearchFilterItemDTO toDTO(SettingsSearchFilterItem filterItem) {
        return SearchFilterItemDTO
                .builder()
                .label(filterItem.getLabel())
                .value(filterItem.getValue())
                .preset(true)
                .build();
    }

    public SettingsPingDTO toDTO(SettingsPing settingsPing) {
        return SettingsPingDTO
                .builder()
                .enabled(settingsPing.isEnabled())
                .endpoints(settingsPing.getEndpoints())
                .interval(pingProperties.getInterval().toString())
                .build();
    }

    public SettingsRepositoryDTO getRepositoryDTO() {
        return SettingsRepositoryDTO
                .builder()
                .type(repositoryProperties.getStringType())
                .dir(repositoryProperties.getDir())
                .url(repositoryProperties.getUrl())
                .repository(repositoryProperties.getRepository())
                .username(repositoryProperties.getUsername())
                .password(repositoryProperties.getPassword() != null ? "<SECRET>" : null)
                .build();
    }

    public Settings fromUpdateDTO(SettingsUpdateDTO dto, Settings settings) {
        return settings
                .toBuilder()
                .metadataMetrics(dto.getMetadataMetrics())
                .ping(fromUpdateDTO(dto.getPing(), settings.getPing()))
                .searchFilters(dto.getSearch().getFilters().stream().map(this::fromUpdateDTO).toList())
                .build();
    }

    public SettingsSearchFilter fromUpdateDTO(SearchFilterDTO dto) {
        return SettingsSearchFilter
                .builder()
                .type(dto.getType())
                .label(dto.getLabel())
                .predicate(dto.getPredicate())
                .queryFromRecords(dto.isQueryFromRecords())
                .presetValues(dto.getValues().stream().map(this::fromUpdateDTO).toList())
                .build();
    }

    public SettingsSearchFilterItem fromUpdateDTO(SearchFilterItemDTO dto) {
        return SettingsSearchFilterItem
                .builder()
                .label(dto.getLabel())
                .value(dto.getValue())
                .build();
    }

    public SettingsPing fromUpdateDTO(SettingsPingUpdateDTO dto, SettingsPing settingsPing) {
        return settingsPing
                .toBuilder()
                .enabled(dto.isEnabled())
                .endpoints(dto.getEndpoints())
                .build();
    }

    public SettingsUpdateDTO toUpdateDTO(Settings settings) {
        return SettingsUpdateDTO
                .builder()
                .metadataMetrics(settings.getMetadataMetrics())
                .ping(toUpdateDTO(settings.getPing()))
                .search(
                        SettingsSearchDTO
                                .builder()
                                .filters(
                                        settings
                                                .getSearchFilters()
                                                .stream()
                                                .map(searchMapper::toFilterDTO)
                                                .toList()
                                )
                                .build()
                )
                .build();
    }

    public SettingsPingUpdateDTO toUpdateDTO(SettingsPing settingsPing) {
        return SettingsPingUpdateDTO
                .builder()
                .enabled(settingsPing.isEnabled())
                .endpoints(settingsPing.getEndpoints())
                .build();
    }
}
