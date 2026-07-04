package org.example.bdoc.io;

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.example.bdoc.model.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Открытый .bdoc-документ: манифест и stories загружены в память,
 * страницы подгружаются по требованию через loadPage(index).
 */
public final class DocumentHandle implements Closeable {

    private final BdocContainer container;
    private final Manifest manifest;
    private final StylesCatalog styles;
    private final TemplatesCatalog templates;
    private final Map<String, StoryModel> storiesById;
    private final Map<Integer, ManifestPageEntry> pageEntriesByIndex;
    private final Map<Integer, PageModel> pageCache = new HashMap<>();
    private final CBORMapper cborMapper;

    DocumentHandle(BdocContainer container, Manifest manifest, StylesCatalog styles,
                   List<StoryModel> stories, TemplatesCatalog templates, CBORMapper cborMapper) {
        this.container = container;
        this.manifest = manifest;
        this.styles = styles;
        this.templates = templates != null ? templates : TemplatesCatalog.empty();
        this.cborMapper = cborMapper;
        this.storiesById = stories.stream()
                .collect(Collectors.toMap(StoryModel::getId, s -> s));
        this.pageEntriesByIndex = manifest.getPages().stream()
                .collect(Collectors.toMap(ManifestPageEntry::getIndex, e -> e));
    }

    public String getId() { return manifest.getId(); }
    public String getTitle() { return manifest.getTitle(); }
    public String getDocumentType() { return manifest.getDocumentType(); }
    public int getPageCount() { return manifest.getPages().size(); }
    public StylesCatalog getStyles() { return styles; }
    public TemplatesCatalog getTemplates() { return templates; }

    public StoryModel getStory(String storyId) {
        return storiesById.get(storyId);
    }

    /**
     * Возвращает шаблон страницы по её templateRef, либо null, если
     * страница не привязана к мастеру или мастер не найден.
     */
    public MasterPage getMasterPage(String templateRef) {
        return templates.findMasterPage(templateRef);
    }

    /**
     * Загружает страницу по логическому номеру (1-based), используя CBOR-парсер.
     * Результат кэшируется, повторный вызов не обращается к архиву.
     */
    public PageModel loadPage(int index) throws IOException {
        PageModel cached = pageCache.get(index);
        if (cached != null) {
            return cached;
        }
        ManifestPageEntry entry = pageEntriesByIndex.get(index);
        if (entry == null) {
            throw new IllegalArgumentException("No page with index " + index + " in manifest");
        }
        byte[] bytes = container.readBytes(entry.getFile());
        PageModel page = cborMapper.readValue(bytes, PageModel.class);
        pageCache.put(index, page);
        return page;
    }

    public byte[] loadResourceBytes(String assetRef) throws IOException {
        return container.readBytes(assetRef);
    }

    @Override
    public void close() throws IOException {
        container.close();
    }

    public java.util.Set<Integer> getPageIndices() {
        return pageEntriesByIndex.keySet();
    }

    public boolean hasResource(String assetRef) {
        return container.exists(assetRef);
    }

    public java.util.Set<String> getStoryIds() {
        return storiesById.keySet();
    }
}