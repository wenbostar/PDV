package PDVGUI.utils;

import com.compomics.util.enumeration.ImageType;

import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

/**
 * Export to figure formats for Components.
 * Created by Ken on 12/1/2017.
 */
public class Export {

    /**
     * Constructor
     * @param component Component to export
     * @param bounds Dimensions of the viewport
     * @param exportFile Output file
     * @param imageType Image type
     */
    public static void exportPic(Component component, Rectangle bounds, File exportFile, ImageType imageType)
            throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(svgDocument);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        component.paint(svgGraphics2D);

        exportExceptedFormatPic(exportFile, imageType, svgGraphics2D);
    }

    /**
     * Exports the selected file to the selected format.
     * @param exportFile Output file
     * @param imageType Image type
     * @param svgGraphics2D SVGGraphics2D
     */
    private static void exportExceptedFormatPic(File exportFile, ImageType imageType, SVGGraphics2D svgGraphics2D)
            throws IOException, TranscoderException {

        DecimalFormat df = new DecimalFormat("#.000000");

        File svgFile = new File(exportFile.getAbsolutePath() + ".temp");

        OutputStream outputStream = new FileOutputStream(svgFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        bos.close();

        String svgURI = svgFile.toURI().toString();
        TranscoderInput svgInputFile = new TranscoderInput(svgURI);

        OutputStream outstream = new FileOutputStream(exportFile);
        bos = new BufferedOutputStream(outstream);
        TranscoderOutput output = new TranscoderOutput(bos);

        if (imageType == ImageType.PDF) {

            Transcoder pdfTranscoder = new PDFTranscoder();
            pdfTranscoder.addTranscodingHint(PDFTranscoder.KEY_DEVICE_RESOLUTION, new Float(Toolkit.getDefaultToolkit().getScreenResolution()));
            pdfTranscoder.transcode(svgInputFile, output);

        }  else if (imageType == ImageType.TIFF) {

            Transcoder tiffTranscoder = new TIFFTranscoder();
            tiffTranscoder.addTranscodingHint(TIFFTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(df.format(25.4 / Toolkit.getDefaultToolkit().getScreenResolution())));
            tiffTranscoder.addTranscodingHint(TIFFTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true);
            tiffTranscoder.transcode(svgInputFile, output);

        } else if (imageType == ImageType.PNG) {

            Transcoder pngTranscoder = new PNGTranscoder();
            pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER, new Float(df.format(25.4 / Toolkit.getDefaultToolkit().getScreenResolution())));
            pngTranscoder.transcode(svgInputFile, output);

        }

        outstream.flush();
        outstream.close();
        bos.close();

        if (svgFile.exists()) {
            svgFile.delete();
        }
    }
}
