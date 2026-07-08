# BDoc Editor 📚✨

`BDoc Editor` is an open-source, extensible desktop publishing (DTP) environment and visual layout editor specially designed for book restoration, layout reconstruction, and advanced digital typesetting.

The project introduces the **BDoc (Book Document)** format specification—a clean, modern alternative to legacy binary polymorphic formats (like IDML, INDD, or Scribus SLA). It is architected from the ground up to support high-fidelity print preparation, multi-layered canvas renderings, and AI-assisted layout recovery.

![BDoc Editor Interface Overview](assets/screenshots/img-full.png)

---

## 🚀 Key Features & Architecture

* **Strict Model/UI Separation:** Pure POJO data model (`bdoc-model`) decoupled from JavaFX/Canvas for headless processing and scripting.
* **Extensible SPI & ModuleLayer Plugin Architecture:** Dynamic module loading from `plugins/` via `ServiceLoader`, similar to the IntelliJ Platform.
* **Hybrid Stream Processing (JSON + CBOR):** Fast, low-memory, lazy-loading of heavy pages via `CBORMapper`, with JSON metadata.
* **DTP Background Threading Framework (`TaskQueue`):** Isolates heavy operations (OCR, layout) from the JavaFX thread to prevent freezing.


### 📸 Application Previews

<p align="center">
  <img src="assets/screenshots/img-1.png" width="30%" alt="Preview 1" />
  <img src="assets/screenshots/img-3.png" width="30%" alt="Preview 2" />
  <img src="assets/screenshots/img-4.png" width="30%" alt="Preview 3" />
</p>

---

## 🛠 Project Structure

The repository is organized into highly decoupled modules:

* `bdoc-core` — Core application logic and execution engine.
* `bdoc-model` — Pure, framework-agnostic document POJOs (JSON/CBOR serializable).
* `bdoc-ui` — Desktop visual editor application built with JavaFX.
* `bdoc-spi` — Service Provider Interfaces and extension points for plugin developers.
* `bdoc-io` — Input/Output drivers for format serialization and document importing.
* `bdoc-plugin-demo-toolbar` — Reference implementation of a decoupled UI toolbar plugin.

---

## 📂 The BDoc Format Specification (v0.1)

The open `.bdoc` format specification is designed as a canonical, framework-agnostic document representation that decouples structural semantics from rendering geometry. It synthesizes core DTP standards (InDesign IDML, Scribus SLA, ODF) with advanced accessibility frameworks (ALTO/PAGE OCR layouts).

### Core Architectural Principles
* **Layer Separation:** Independent boundaries for Document Semantics (`Stories`), Page Layout (`Pages`), and Rendering Logic (`Graphics`).
* **Deterministic Behavior:** Fully programmatic and predictable layout schemas; execution of custom inline scripts or active external code is strictly restricted.
* **Deterministic Accessibility:** Built-in semantic reading order for accessibility, screen readers, and programmatic text reflow.

### Document Block Architecture
A minimal valid BDoc v0.1 file container separates metadata, logical streams, and geometries into top-level logical blocks:

| Block | Target Domain & Purpose |
| :--- | :--- |
| **`Document`** | Root entry point containing structural meta-properties, global versioning, and base localization. |
| **`Templates`** | Master pages layouts, margin configurations, column guides, and running header/footer rules. |
| **`Pages`** | Physical print spreads and page matrices mapping boundary metrics to active objects. |
| **`Layers`** | Composite composition planes (background overlays, vector artwork, annotations, typography). |
| **`Stories`** | Contiguous, layout-independent textual flows mapped through paragraphs and text spans. |
| **`Styles`** | Reusable stylistic property registries targeting paragraphs, inline characters, frames, and tables. |
| **`Graphics`** | Primitive 2D paths, vector transforms, boolean compound shapes, clipping rules, and opacity masks. |
| **`Assets`** | Centralized binary artifact registries hosting font subsets, embedded rasters, and ICC profiles. |
| **`ReadingOrder`** | Explicit logical sequencing indices guiding downstream multi-column converters and TTS models. |

### Data Model Blueprints
* **The Text Engine (`Stories`):** Composed of hierarchical `Story` ➔ `Paragraph` ➔ `Span` structures. Paragraph nodes enforce explicit typographic semantic roles (`title`, `heading`, `body`, `caption`, `footnote`).
* **The Geometry Engine (`Graphics`):** Implements absolute path constructors including precise bezier curves, transformations matrices, complex clipping paths, and custom compound shapes.

---

## 🗺 Roadmap & Current Status

The project is moving through a structured development cycle:

1. **Stage 1: Document Structure & Core Geometry**
    * [ ] Mastering Pages, Margin Models, and Layout Guides.
    * [ ] ReadingOrder Segments and text wrap boundaries.
    * [ ] Extended vector geometry (BezierPaths, CompoundPaths) and strict multi-unit conversion.
    * [ ] Text threading (story flow across frames) and style hierarchies.
2. **Stage 2: Professional DTP & Preflight Functionality**
    * [ ] Visual Master Page overriding.
    * [ ] Integrated Preflight validation (Effective DPI check, CMYK profile matching, bleed/trim safety zones).
    * [ ] Advanced DTP tools (Gap tool, Scissors/Pen nodes editing, Eyedropper style copying).
    * [ ] Advanced Typography (Justify with Knuth-Plass algorithm, OpenType ligatures).
3. **Stage 3: Rendering Optimization & SDK Ecosystem**
    * [ ] Render Cache (Bitmap buffering for fluid 1000+ page scrolling on lower-end hardware).
    * [ ] Declarative plugin distribution marketplace.

---

## 🏗 Getting Started

### Prerequisites
* **JDK 17** or higher
* **Gradle** (Wrapper included)

### Build and Run
Clone the repository and build:
```bash
git clone https://github.com
cd bdoc-editor
./gradlew build
./gradlew :bdoc-ui:run
```

---

## 🤝 Contributing & Community

We are building a toolchain for preservationists, typographers, and software engineers alike. Contributions to the specifications, text layout engines, or importing filters are highly welcome!

Feel free to open an Issue, submit a Pull Request, or check out our specifications draft inside the `preliminary-open-document-spec-v0.1.md` file.

## 📄 License
This project is licensed under the MIT License.
