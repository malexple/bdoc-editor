package org.example.bdoc.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import org.example.bdoc.model.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BdocContainerSerializer {

    private final ObjectMapper jsonMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final CBORMapper cborMapper = (CBORMapper) new CBORMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * Открывает .bdoc-файл: манифест и stories читаются немедленно,
     * страницы — лениво через возвращённый DocumentHandle.
     */
    public DocumentHandle open(File bdocFile) throws IOException {
        BdocContainer container = BdocContainer.openForRead(bdocFile.toPath());
        try {
            Manifest manifest = jsonMapper.readValue(
                    container.readBytes("manifest.json"), Manifest.class);

            StylesCatalog styles = container.exists("styles.json")
                    ? jsonMapper.readValue(container.readBytes("styles.json"), StylesCatalog.class)
                    : StylesCatalog.empty();

            List<StoryModel> stories = new ArrayList<>();
            for (ManifestStoryEntry entry : manifest.getStories()) {
                StoryModel story = jsonMapper.readValue(
                        container.readBytes(entry.getFile()), StoryModel.class);
                stories.add(story);
            }

            return new DocumentHandle(container, manifest, styles, stories, cborMapper);
        } catch (IOException e) {
            container.close();
            throw e;
        }
    }

    /**
     * Начинает потоковую запись нового .bdoc-файла.
     * Страницы пишутся по одной, без накопления всех в памяти —
     * это позволяет собирать книги на тысячи страниц с постоянным расходом RAM.
     */
    public Writer beginWrite(File bdocFile) throws IOException {
        BdocContainer container = BdocContainer.createForWrite(bdocFile.toPath());
        return new Writer(container, jsonMapper, cborMapper);
    }

    public static final class Writer implements AutoCloseable {

        private final BdocContainer container;
        private final ObjectMapper jsonMapper;
        private final CBORMapper cborMapper;
        private final List<ManifestPageEntry> pageEntries = new ArrayList<>();
        private final List<ManifestStoryEntry> storyEntries = new ArrayList<>();
        private boolean finished = false;

        private Writer(BdocContainer container, ObjectMapper jsonMapper, CBORMapper cborMapper) {
            this.container = container;
            this.jsonMapper = jsonMapper;
            this.cborMapper = cborMapper;
        }

        public Writer writeStory(StoryModel story) throws IOException {
            String file = "stories/" + story.getId() + ".json";
            container.writeBytes(file, jsonMapper.writeValueAsBytes(story));
            storyEntries.add(new ManifestStoryEntry(story.getId(), file));
            return this;
        }

        public Writer writePage(PageModel page) throws IOException {
            String file = "pages/page-" + page.getIndex() + ".cbor";
            container.writeBytes(file, cborMapper.writeValueAsBytes(page));
            pageEntries.add(new ManifestPageEntry(page.getIndex(), page.getId(), file));
            return this;
        }

        public Writer writeResource(String assetRef, byte[] data) throws IOException {
            container.writeBytes(assetRef, data);
            return this;
        }

        public void finish(String id, String title, String documentType,
                           String version, String language, StylesCatalog styles) throws IOException {
            if (finished) {
                throw new IllegalStateException("Writer already finished");
            }
            Manifest manifest = new Manifest(
                    id, title, documentType, version, language, pageEntries, storyEntries);
            container.writeBytes("manifest.json", jsonMapper.writeValueAsBytes(manifest));
            container.writeBytes("styles.json",
                    jsonMapper.writeValueAsBytes(styles != null ? styles : StylesCatalog.empty()));
            finished = true;
            container.close();
        }

        @Override
        public void close() throws IOException {
            if (!finished) {
                container.close();
            }
        }
    }
}