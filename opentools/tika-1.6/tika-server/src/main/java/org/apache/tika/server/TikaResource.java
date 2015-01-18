/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tika.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.EncryptedDocumentException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

@Path("/tika")
public class TikaResource {
  public static final String GREETING = "This is Tika Server. Please PUT\n";
  private final Log logger = LogFactory.getLog(TikaResource.class);
  
  private TikaConfig tikaConfig;
  public TikaResource(TikaConfig tikaConfig) {
      this.tikaConfig = tikaConfig;
  }

  static {
    ExtractorFactory.setAllThreadsPreferEventExtractors(true);
  }
  
  @GET
  @Produces("text/plain")
  public String getMessage() {
    return GREETING;
  }

  @SuppressWarnings("serial")
  public static AutoDetectParser createParser(TikaConfig tikaConfig) {
    final AutoDetectParser parser = new AutoDetectParser(tikaConfig);

    Map<MediaType,Parser> parsers = parser.getParsers();
    parsers.put(MediaType.APPLICATION_XML, new HtmlParser());
    parser.setParsers(parsers);

    parser.setFallback(new Parser() {
      public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
        return parser.getSupportedTypes(parseContext);
      }

      public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata, ParseContext parseContext) {
        throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
      }
    });

    return parser;
  }

  public static String detectFilename(MultivaluedMap<String, String> httpHeaders) {

    String disposition = httpHeaders.getFirst("Content-Disposition");
    if (disposition != null) {
      try {
        ContentDisposition c = new ContentDisposition(disposition);

        // only support "attachment" dispositions
        if ("attachment".equals(c.getDisposition())) {
          String fn = c.getParameter("filename");
          if (fn != null) {
            return fn;
          }
        }
      } catch (ParseException e) {
        // not a valid content-disposition field
      }
    }

    // this really should not be used, since it's not an official field
    return httpHeaders.getFirst("File-Name");
  }

  @SuppressWarnings("serial")
public static void fillMetadata(AutoDetectParser parser, Metadata metadata, MultivaluedMap<String, String> httpHeaders) {
    String fileName = detectFilename(httpHeaders);
    if (fileName != null) {
      metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, fileName);
    }

    String contentTypeHeader = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
    javax.ws.rs.core.MediaType mediaType = contentTypeHeader == null ? null 
        : javax.ws.rs.core.MediaType.valueOf(contentTypeHeader);
    if (mediaType!=null && "xml".equals(mediaType.getSubtype()) ) {
      mediaType = null;
    }

    if (mediaType !=null && mediaType.equals(javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE)) {
      mediaType = null;
    }

    if (mediaType !=null) {
      metadata.add(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE, mediaType.toString());

      final Detector detector = parser.getDetector();

      parser.setDetector(new Detector() {
        public MediaType detect(InputStream inputStream, Metadata metadata) throws IOException {
          String ct = metadata.get(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE);

          if (ct!=null) {
            return MediaType.parse(ct);
          } else {
            return detector.detect(inputStream, metadata);
          }
        }
      });
    }
  }

  @PUT
  @Consumes("multipart/form-data")
  @Produces("text/plain")
  @Path("form")
  public StreamingOutput getTextFromMultipart(Attachment att, @Context final UriInfo info) {
	  return produceText(att.getObject(InputStream.class), att.getHeaders(), info);
  }
  
  @PUT
  @Consumes("*/*")
  @Produces("text/plain")
  public StreamingOutput getText(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
	  return produceText(is, httpHeaders.getRequestHeaders(), info);
  }
  public StreamingOutput produceText(final InputStream is, MultivaluedMap<String, String> httpHeaders, final UriInfo info) {	  
    final AutoDetectParser parser = createParser(tikaConfig);
    final Metadata metadata = new Metadata();

    fillMetadata(parser, metadata, httpHeaders);

    logRequest(logger, info, metadata);

    return new StreamingOutput() {
      public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");

        BodyContentHandler body = new BodyContentHandler(new RichTextContentHandler(writer));

        TikaInputStream tis = TikaInputStream.get(is);

        try {
            parser.parse(tis, body, metadata);
        } catch (SAXException e) {
          throw new WebApplicationException(e);
        } catch (EncryptedDocumentException e) {
          logger.warn(String.format(
                  Locale.ROOT,
                  "%s: Encrypted document",
                  info.getPath()
          ), e);

          throw new WebApplicationException(e, Response.status(422).build());
        } catch (TikaException e) {
          logger.warn(String.format(
            Locale.ROOT,
            "%s: Text extraction failed",
            info.getPath()
          ), e);

          if (e.getCause()!=null && e.getCause() instanceof WebApplicationException) {
            throw (WebApplicationException) e.getCause();
          }

          if (e.getCause()!=null && e.getCause() instanceof IllegalStateException) {
            throw new WebApplicationException(Response.status(422).build());
          }

          if (e.getCause()!=null && e.getCause() instanceof OldWordFileFormatException) {
            throw new WebApplicationException(Response.status(422).build());
          }

          throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
          tis.close();
        }
      }
    };
  }

  @PUT
  @Consumes("multipart/form-data")
  @Produces("text/html")
  @Path("form")
  public StreamingOutput getHTMLFromMultipart(Attachment att, @Context final UriInfo info) {
	  return produceOutput(att.getObject(InputStream.class), att.getHeaders(), info, "html");
  }

  @PUT
  @Consumes("*/*")
  @Produces("text/html")
  public StreamingOutput getHTML(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
	  return produceOutput(is, httpHeaders.getRequestHeaders(), info, "html");
  }

  @PUT
  @Consumes("multipart/form-data")
  @Produces("text/xml")
  @Path("form")
  public StreamingOutput getXMLFromMultipart(Attachment att, @Context final UriInfo info) {
	  return produceOutput(att.getObject(InputStream.class), att.getHeaders(), info, "xml");
  }
  
  @PUT
  @Consumes("*/*")
  @Produces("text/xml")
  public StreamingOutput getXML(final InputStream is, @Context HttpHeaders httpHeaders, @Context final UriInfo info) {
    return produceOutput(is, httpHeaders.getRequestHeaders(), info, "xml");
  }
  
  private StreamingOutput produceOutput(final InputStream is, final MultivaluedMap<String, String> httpHeaders, 
        final UriInfo info, final String format) {
    final AutoDetectParser parser = createParser(tikaConfig);
    final Metadata metadata = new Metadata();

    fillMetadata(parser, metadata, httpHeaders);

    logRequest(logger, info, metadata);

    return new StreamingOutput() {
      public void write(OutputStream outputStream)
        throws IOException, WebApplicationException {
        Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
        ContentHandler content;

        try {
          SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance( );
          TransformerHandler handler = factory.newTransformerHandler( );
          handler.getTransformer().setOutputProperty(OutputKeys.METHOD, format);
          handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
          handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
          handler.setResult(new StreamResult(writer));
          content = new ExpandedTitleContentHandler( handler );
        }
        catch ( TransformerConfigurationException e ) {
          throw new WebApplicationException( e );
        }

        TikaInputStream tis = TikaInputStream.get(is);

        try {
          parser.parse(tis, content, metadata);
        }
        catch (SAXException e) {
          throw new WebApplicationException(e);
        }
        catch (EncryptedDocumentException e) {
          logger.warn(String.format(
            Locale.ROOT,
            "%s: Encrypted document",
            info.getPath()
          ), e);
          throw new WebApplicationException(e, Response.status(422).build());
        }
        catch (TikaException e) {
          logger.warn(String.format(
            Locale.ROOT,
            "%s: Text extraction failed",
            info.getPath()
          ), e);

          if (e.getCause()!=null && e.getCause() instanceof WebApplicationException)
            throw (WebApplicationException) e.getCause();

          if (e.getCause()!=null && e.getCause() instanceof IllegalStateException)
            throw new WebApplicationException(Response.status(422).build());

          if (e.getCause()!=null && e.getCause() instanceof OldWordFileFormatException)
            throw new WebApplicationException(Response.status(422).build());

          throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        finally {
          tis.close();
        }
      }
    };
  }

  public static void logRequest(Log logger, UriInfo info, Metadata metadata) {
    if (metadata.get(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE)==null) {
      logger.info(String.format(
              Locale.ROOT,
              "%s (autodetecting type)",
              info.getPath()
      ));
    } else {
      logger.info(String.format(
              Locale.ROOT,
              "%s (%s)",
              info.getPath(),
              metadata.get(org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE)
      ));
    }
  }
}
