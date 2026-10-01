package lat.jbluesoft.erpadmin.asistencia.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Responsable únicamente de convertir HTML (ya renderizado) a PDF.
 * No conoce nada del dominio de negocio (Single Responsibility Principle).
 */
@Component
public class PdfGeneratorService {

    public byte[] convertirHtmlAPdf(String html) throws IOException {
        Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withW3cDocument(w3cDoc, "");
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }
}
