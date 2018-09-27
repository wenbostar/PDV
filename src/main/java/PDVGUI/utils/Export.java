package PDVGUI.utils;

import com.compomics.util.enumeration.ImageType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.metadata.tiff.Rational;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.metadata.tiff.TIFFEntry;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.apache.fop.svg.PDFTranscoder;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.svg.SVGDocument;

import javax.imageio.*;
import javax.imageio.metadata.IIOInvalidTreeException;

import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

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
    public static void exportPic(Component component, Rectangle bounds, File exportFile, ImageType imageType) throws IOException, TranscoderException {

        if (imageType == ImageType.PDF){
            exportPDF(component, bounds, exportFile);
        } else if (imageType == ImageType.PNG){
            exportPNG(component, bounds, exportFile);
        } else if (imageType == ImageType.TIFF){
            exportTIFF(component, bounds, exportFile);
        }
    }

    /**
     * Constructor
     * @param chart JFreeChart to export
     * @param bounds Dimensions of the viewport
     * @param exportFile Output file
     * @param imageType Image type
     */
    public static void exportPic(JFreeChart chart, Rectangle bounds, File exportFile, ImageType imageType)
            throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(svgDocument);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        chart.draw(svgGraphics2D, bounds);

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

        if (new File(exportFile.getAbsolutePath() + ".temp").exists()) {
            new File(exportFile.getAbsolutePath() + ".temp").delete();
        }

        File svgFile = new File(exportFile.getAbsolutePath() + ".temp");

        OutputStream outputStream = new FileOutputStream(svgFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        out.close();
        bos.close();

        String svgURI = svgFile.toURI().toString();
        TranscoderInput svgInputFile = new TranscoderInput(svgURI);

        OutputStream outstream = new FileOutputStream(exportFile);
        bos = new BufferedOutputStream(outstream);
        TranscoderOutput output = new TranscoderOutput(bos);

        if (imageType == ImageType.PDF) {

            Transcoder pdfTranscoder = new PDFTranscoder();
            pdfTranscoder.addTranscodingHint(PDFTranscoder.KEY_DEVICE_RESOLUTION, (float) Toolkit.getDefaultToolkit().getScreenResolution());
            pdfTranscoder.transcode(svgInputFile, output);

        }  else if (imageType == ImageType.JPEG) {

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

    /**
     * Export component to pdf format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     * @throws TranscoderException
     */
    private static void exportPDF(Component component, Rectangle bounds, File exportFile) throws IOException, TranscoderException {

        DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        SVGDocument svgDocument = (SVGDocument) domImplementation.createDocument(svgNS, "svg", null);

        SVGGraphics2D svgGraphics2D = new SVGGraphics2D(svgDocument);
        svgGraphics2D.setSVGCanvasSize(bounds.getSize());

        component.paintAll(svgGraphics2D);

        if (new File(exportFile.getAbsolutePath() + ".temp").exists()) {
            new File(exportFile.getAbsolutePath() + ".temp").delete();
        }

        File svgFile = new File(exportFile.getAbsolutePath() + ".temp");

        OutputStream outputStream = new FileOutputStream(svgFile);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        Writer out = new OutputStreamWriter(bos, "UTF-8");

        svgGraphics2D.stream(out, true);
        outputStream.flush();
        outputStream.close();
        out.close();
        bos.close();

        String svgURI = svgFile.toURI().toString();
        TranscoderInput svgInputFile = new TranscoderInput(svgURI);

        OutputStream outstream = new FileOutputStream(exportFile);
        bos = new BufferedOutputStream(outstream);
        TranscoderOutput output = new TranscoderOutput(bos);

        Transcoder pdfTranscoder = new PDFTranscoder();
        pdfTranscoder.addTranscodingHint(PDFTranscoder.KEY_DEVICE_RESOLUTION, (float) Toolkit.getDefaultToolkit().getScreenResolution());
        pdfTranscoder.transcode(svgInputFile, output);

        outstream.flush();
        outstream.close();
        bos.close();

        if (svgFile.exists()) {
            svgFile.delete();
        }

    }

    /**
     * Export component to png format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     */
    private static void exportPNG(Component component, Rectangle bounds, File exportFile) throws IOException {

        BufferedImage bufferedImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);

        Graphics g = bufferedImage.getGraphics();

        component.paint(g);

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("png"); iw.hasNext();){

            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

            if (metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()){
                continue;
            }
            ImageOutputStream stream = null;
            try {

                setDPI(metadata);

                stream = ImageIO.createImageOutputStream(exportFile);
                writer.setOutput(stream);
                writer.write(metadata, new IIOImage(bufferedImage, null, metadata), writeParam);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
            break;
        }
    }

    /**
     * Set DPI for png format pic
     * @param metadata IIOMetadata
     * @throws IIOInvalidTreeException
     */
    private static void setDPI(IIOMetadata metadata) throws IIOInvalidTreeException {

        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

        double dotsPerMilli = 1.0 * dpi / 10 / 2.54;

        IIOMetadataNode horizontalPixelSize = new IIOMetadataNode("HorizontalPixelSize");
        horizontalPixelSize.setAttribute("value", String.valueOf(dotsPerMilli));

        IIOMetadataNode verticalPixelSize = new IIOMetadataNode("VerticalPixelSize");
        verticalPixelSize.setAttribute("value", String.valueOf(dotsPerMilli));

        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        dimension.appendChild(horizontalPixelSize);
        dimension.appendChild(verticalPixelSize);

        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        root.appendChild(dimension);

        metadata.mergeTree("javax_imageio_1.0", root);

    }

    /**
     * Export component to tiff format
     * @param component Component
     * @param bounds Rectangle
     * @param exportFile Export file
     * @throws IOException
     */
    private static void exportTIFF(Component component, Rectangle bounds, File exportFile) throws IOException {

        int dpi = Toolkit.getDefaultToolkit().getScreenResolution();

        BufferedImage bufferedImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);

        Graphics g = bufferedImage.getGraphics();

        component.paint(g);

        for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName("tiff"); iw.hasNext();) {

            ImageWriter writer = iw.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            //Picture compression
            //writeParam.setCompressionMode(ImageWriteParam.MODE_DEFAULT);
            //writeParam.setCompressionType("CCITT T.6");
            //writeParam.setCompressionQuality(0.2f);

            ArrayList<Entry> entries = new ArrayList<>();

            entries.add(new TIFFEntry(TIFF.TAG_X_RESOLUTION, new Rational(dpi)));
            entries.add(new TIFFEntry(TIFF.TAG_Y_RESOLUTION, new Rational(dpi)));

            TIFFImageMetadata tiffMetadata = new TIFFImageMetadata(entries);

            ImageOutputStream stream = null;

            try {
                stream = ImageIO.createImageOutputStream(exportFile);
                writer.setOutput(stream);

                writer.write(null, new IIOImage(bufferedImage, null, tiffMetadata), writeParam);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stream.close();
            }
            break;

        }

    }

}
