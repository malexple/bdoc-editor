module org.example.bdoc.plugin.demo {
    requires org.example.bdoc.spi;
    requires javafx.controls;

    provides org.example.bdoc.spi.ToolbarActionExtension
            with org.example.bdoc.plugin.demo.DemoToolbarButtonPlugin;
}