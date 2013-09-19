package ch.opendata.law.fccextractor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.InvalidContentException;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.randomUUID;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component(immediate = true, metatype = true, inherit = true)
@Service
@Properties(value = {
    @Property(name = EnhancementEngine.PROPERTY_NAME, value = "fccextractor")
})
public class FccEnhancer extends AbstractEnhancementEngine
        implements EnhancementEngine, ServiceProperties {

    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(FccEnhancer.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    @Reference
    private ContentItemFactory ciFactory;
    
    @Reference
    private Parser parser;

    /**
     * ServiceProperties are currently only used for automatic ordering of the
     * execution of EnhancementEngines (e.g. by the WeightedChain
     * implementation). Default ordering means that the engine is called after
     * all engines that use a value < {@link
     * ServiceProperties#ORDERING_CONTENT_EXTRACTION} and >=
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}.
     */
    public Map getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
                ENHANCEMENT_ENGINE_ORDERING, ORDERING_CONTENT_EXTRACTION));
    }

    /**
     * @return if and how (asynchronously) we can enhance a ContentItem
     */
    public int canEnhance(ContentItem ci) throws EngineException {
        // check if content is present
        try {
            if ((ci.getBlob() == null)
                    || (ci.getBlob().getStream().read() == -1)) {
                return CANNOT_ENHANCE;
            }
        } catch (IOException e) {
            log.error("Failed to get the text for "
                    + "enhancement of content: " + ci.getUri(), e);
            throw new InvalidContentException(this, ci, e);
        }
        // no reason why we should require to be executed synchronously
        return ENHANCE_ASYNC;
    }

    public void computeEnhancements(ContentItem ci) throws EngineException {
        try {
            Map.Entry<UriRef, Blob> entry =
                    ContentItemHelper.getBlob(ci,
                    Collections.singleton("application/xml"));
            if (entry == null) {
                System.out.println("ignoring entry with no xml");
                return;
            }
            Blob xmlBlob = entry.getValue();
            InputStream in = xmlBlob.getStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            dbf.setIgnoringComments(false);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setFeature("http://xml.org/sax/features/namespaces", false);
            dbf.setFeature("http://xml.org/sax/features/validation", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(in));
            //db.setEntityResolver(new NullResolver());
            System.out.println("xml: " + doc);
            extractPlainText(doc, ci);
            extractMetadata(doc, ci);
        } catch (ParserConfigurationException ex) {
            java.util.logging.Logger.getLogger(FccEnhancer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            java.util.logging.Logger.getLogger(FccEnhancer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(FccEnhancer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            java.util.logging.Logger.getLogger(FccEnhancer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            java.util.logging.Logger.getLogger(FccEnhancer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void extractPlainText(Document doc, ContentItem ci) throws TransformerException, IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source xsltSource = new StreamSource(getClass().getResourceAsStream("caselaw.xsl"));
        ContentSink plainTextSink = ciFactory.createContentSink("text/plain" + "; charset=" + UTF8.name());
        StreamResult result = new StreamResult(plainTextSink.getOutputStream());
        Transformer t = tf.newTransformer(xsltSource);
        t.transform(new DOMSource(doc), result);
        String random = randomUUID().toString();
        UriRef textBlobUri = new UriRef("urn:fcc:text:" + random);
        ci.addPart(textBlobUri, plainTextSink.getBlob());
    }
    
    
    private void extractMetadata(Document doc, ContentItem ci) throws TransformerException, IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source xsltSource = new StreamSource(getClass().getResourceAsStream("caselaw-rdf.xsl"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(baos);
        Transformer t = tf.newTransformer(xsltSource);
        t.transform(new DOMSource(doc), result);
        MGraph parsedData = new SimpleMGraph();
        parser.parse(parsedData, new ByteArrayInputStream(baos.toByteArray()), SupportedFormat.RDF_XML);
        GraphNode docNode = new GraphNode(new UriRef("urn:x-current-document:/"), parsedData);
        docNode.replaceWith(ci.getUri());
        ci.getMetadata().addAll(parsedData);
    }
}
