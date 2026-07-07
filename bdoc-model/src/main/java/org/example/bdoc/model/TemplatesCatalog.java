package org.example.bdoc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class TemplatesCatalog {

    private final List<MasterPage> masterPages;

    @JsonCreator
    public TemplatesCatalog(@JsonProperty("masterPages") List<MasterPage> masterPages) {
        this.masterPages = masterPages != null ? masterPages : List.of();
    }

    public static TemplatesCatalog empty() {
        return new TemplatesCatalog(List.of());
    }

    public List<MasterPage> getMasterPages() { return masterPages; }

    public MasterPage findMasterPage(String id) {
        if (id == null) {
            return null;
        }
        return masterPages.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}